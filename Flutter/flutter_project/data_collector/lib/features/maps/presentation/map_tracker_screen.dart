import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import '../../../data/providers.dart';
import '../domain/location_model.dart';

class MapTrackerScreen extends ConsumerStatefulWidget {
  const MapTrackerScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<MapTrackerScreen> createState() => _MapTrackerScreenState();
}

class _MapTrackerScreenState extends ConsumerState<MapTrackerScreen> {
  GoogleMapController? _mapController;
  final Set<Marker> _markers = {};
  final Set<Polyline> _polylines = {};

  Position? _currentPosition;
  bool _isAutoTracking = false;
  StreamSubscription<Position>? _positionStreamSubscription;

  // 서울 시청 기본 좌표
  static const CameraPosition _initialPosition = CameraPosition(
    target: LatLng(37.5665, 126.9780),
    zoom: 14.0,
  );

  @override
  void initState() {
    super.initState();
    _getCurrentLocation();
  }

  @override
  void dispose() {
    _stopAutoTracking();
    _mapController?.dispose();
    super.dispose();
  }

  // --- 지도 로직 ---

  Future<void> _getCurrentLocation() async {
    try {
      final repo = ref.read(locationRepositoryProvider);
      final position = await repo.getCurrentPosition();

      if (!mounted) return;
      setState(() => _currentPosition = position);

      _updateCamera(position);
      _updateMyMarker(position);
    } catch (e) {
      if (mounted)
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('$e')));
    }
  }

  void _updateCamera(Position position) {
    _mapController?.animateCamera(
      CameraUpdate.newCameraPosition(
        CameraPosition(
            target: LatLng(position.latitude, position.longitude), zoom: 17.0),
      ),
    );
  }

  void _updateMyMarker(Position position) {
    final marker = Marker(
      markerId: const MarkerId('current'),
      position: LatLng(position.latitude, position.longitude),
      icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueBlue),
      infoWindow: const InfoWindow(title: '나의 위치'),
      zIndex: 10,
    );
    setState(() {
      _markers.removeWhere((m) => m.markerId.value == 'current');
      _markers.add(marker);
    });
  }

  // --- 데이터 렌더링 (Polyline) ---

  void _renderMapData(List<LocationModel> locations) {
    if (locations.isEmpty) return;

    // 시간순 정렬 (오래된 것 -> 최신)
    final sorted = List<LocationModel>.from(locations)
      ..sort((a, b) => a.timestamp.compareTo(b.timestamp));

    // 1. 경로 그리기 (Polyline)
    final points = sorted.map((l) => LatLng(l.latitude, l.longitude)).toList();
    final polyline = Polyline(
      polylineId: const PolylineId('history'),
      color: Colors.blueAccent,
      width: 5,
      points: points,
    );

    // 2. 기록된 위치 마커 (Start/End or All)
    final historyMarkers = sorted
        .map((l) => Marker(
              markerId: MarkerId(l.id),
              position: LatLng(l.latitude, l.longitude),
              icon: BitmapDescriptor.defaultMarkerWithHue(
                  BitmapDescriptor.hueRed),
              alpha: 0.6,
              infoWindow:
                  InfoWindow(title: '기록됨', snippet: l.timestamp.toString()),
            ))
        .toSet();

    if (mounted) {
      setState(() {
        _polylines.clear();
        _polylines.add(polyline);

        // 기존 '나의 위치' 마커 유지하면서 히스토리 마커 추가
        final myMarker =
            _markers.where((m) => m.markerId.value == 'current').toSet();
        _markers.clear();
        _markers.addAll(myMarker);
        _markers.addAll(historyMarkers);
      });
    }
  }

  // --- 추적 로직 ---

  void _toggleTracking() {
    if (_isAutoTracking) {
      _stopAutoTracking();
    } else {
      _startAutoTracking();
    }
  }

  void _startAutoTracking() {
    setState(() => _isAutoTracking = true);
    final repo = ref.read(locationRepositoryProvider);

    // 10미터 이동 시마다 스트림 수신
    _positionStreamSubscription =
        repo.streamPosition(distanceFilter: 10).listen((position) {
      _updateCamera(position);
      _updateMyMarker(position);
      _saveLocation(position, auto: true);
    });

    ScaffoldMessenger.of(context)
        .showSnackBar(const SnackBar(content: Text('추적 시작 (REC)')));
  }

  void _stopAutoTracking() {
    _positionStreamSubscription?.cancel();
    setState(() => _isAutoTracking = false);
    ScaffoldMessenger.of(context)
        .showSnackBar(const SnackBar(content: Text('추적 종료')));
  }

  Future<void> _saveLocation(Position pos, {bool auto = false}) async {
    final model = LocationModel(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      latitude: pos.latitude,
      longitude: pos.longitude,
      accuracy: pos.accuracy,
      altitude: pos.altitude,
      heading: pos.heading,
      speed: pos.speed,
      timestamp: DateTime.now(),
      notes: auto ? 'Auto Track' : 'Manual Save',
    );
    await ref.read(locationRepositoryProvider).addLocation(model);
  }

  @override
  Widget build(BuildContext context) {
    final locationsAsync = ref.watch(locationsStreamProvider);

    // 데이터 변경 시 지도 업데이트
    ref.listen(locationsStreamProvider, (prev, next) {
      next.whenData((data) => _renderMapData(data));
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Map Tracker'),
        actions: [
          IconButton(
            icon: const Icon(Icons.list),
            onPressed: () => context.push('/maps/list'),
          ),
        ],
      ),
      body: Stack(
        children: [
          GoogleMap(
            initialCameraPosition: _initialPosition,
            onMapCreated: (c) {
              _mapController = c;
              if (locationsAsync.hasValue)
                _renderMapData(locationsAsync.value!);
            },
            markers: _markers,
            polylines: _polylines,
            myLocationEnabled: true,
            myLocationButtonEnabled: false,
            zoomControlsEnabled: false,
          ),

          // REC 표시
          if (_isAutoTracking)
            Positioned(
              top: 20,
              left: 20,
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                    color: Colors.red, borderRadius: BorderRadius.circular(20)),
                child: const Row(
                  children: [
                    Icon(Icons.fiber_manual_record,
                        color: Colors.white, size: 16),
                    SizedBox(width: 4),
                    Text('REC',
                        style: TextStyle(
                            color: Colors.white, fontWeight: FontWeight.bold)),
                  ],
                ),
              ),
            ),
        ],
      ),
      floatingActionButton: Column(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          FloatingActionButton(
            heroTag: 'gps',
            onPressed: _getCurrentLocation,
            child: const Icon(Icons.my_location),
          ),
          const SizedBox(height: 16),
          FloatingActionButton.extended(
            heroTag: 'track',
            onPressed: _toggleTracking,
            backgroundColor: _isAutoTracking ? Colors.red : Colors.blue,
            icon: Icon(_isAutoTracking ? Icons.stop : Icons.play_arrow),
            label: Text(_isAutoTracking ? 'Stop' : 'Start'),
          ),
        ],
      ),
    );
  }
}
