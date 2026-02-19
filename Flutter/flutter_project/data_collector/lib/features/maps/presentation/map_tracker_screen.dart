import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import '../../../data/providers.dart';
import '../domain/location_model.dart';

class MapTrackerScreen extends ConsumerStatefulWidget {
  const MapTrackerScreen({super.key});

  @override
  ConsumerState<MapTrackerScreen> createState() => _MapTrackerScreenState();
}

class _MapTrackerScreenState extends ConsumerState<MapTrackerScreen> {
  // 지도 컨트롤러 (Completer 사용 권장, 여기서는 nullable로 단순화)
  GoogleMapController? _mapController;

  // 마커 및 경로 데이터
  final Set<Marker> _markers = {};
  final Set<Polyline> _polylines = {};

  // 현재 위치 (초기화 전까지 null)
  Position? _currentPosition;

  // 자동 추적 상태
  bool _isAutoTracking = false;
  StreamSubscription<Position>? _positionStreamSubscription;

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
      // 권한 확인 및 요청 (Geolocator 직접 사용 또는 리포지토리 위임)
      bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) throw '위치 서비스가 비활성화되어 있습니다.';

      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
        if (permission == LocationPermission.denied) throw '위치 권한이 거부되었습니다.';
      }
      if (permission == LocationPermission.deniedForever)
        throw '위치 권한이 영구적으로 거부되었습니다.';

      // 리포지토리를 통해 위치 가져오기
      final repo = ref.read(locationRepositoryProvider);
      final position = await repo.getCurrentPosition();

      if (!mounted) return;

      setState(() {
        _currentPosition = position;
      });

      // 초기 위치로 카메라 이동 및 마커 업데이트
      _updateCamera(position);
      _updateMyMarker(position);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Error: $e')));
      }
    }
  }

  void _updateCamera(Position position) {
    _mapController?.animateCamera(
      CameraUpdate.newCameraPosition(
        CameraPosition(
          target: LatLng(position.latitude, position.longitude),
          zoom: 17.0,
        ),
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

  // --- 데이터 렌더링 (Polyline & History Markers) ---

  void _renderMapData(List<LocationModel> locations) {
    if (locations.isEmpty) return;

    // 시간순 정렬 (오래된 것 -> 최신)
    final sorted = List.from(locations)
      ..sort((a, b) => a.timestamp.compareTo(b.timestamp));

    // 1. 경로 그리기 (Polyline)
    final points = sorted.map((l) => LatLng(l.latitude, l.longitude)).toList();
    final polyline = Polyline(
      polylineId: const PolylineId('history'),
      color: Colors.blueAccent,
      width: 5,
      points: points,
    );

    // 2. 기록된 위치 마커
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

    // 데이터 변경 시 지도 업데이트 (이전 기록 보기)
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
      // [FIX] _currentPosition이 null이면 로딩 표시 (Null Check Operator Crash 방지)
      body: _currentPosition == null
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text("현재 위치를 불러오는 중입니다..."),
                ],
              ),
            )
          : Stack(
              children: [
                GoogleMap(
                  // [FIX] 초기 위치를 현재 위치로 설정
                  initialCameraPosition: CameraPosition(
                    target: LatLng(
                      _currentPosition!.latitude,
                      _currentPosition!.longitude,
                    ),
                    zoom: 17.0,
                  ),
                  onMapCreated: (c) {
                    _mapController = c;
                    // 지도가 생성되면 기존 데이터가 있을 경우 렌더링
                    if (locationsAsync.hasValue) {
                      _renderMapData(locationsAsync.value!);
                    }
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
                      padding: const EdgeInsets.symmetric(
                          horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                          color: Colors.red,
                          borderRadius: BorderRadius.circular(20)),
                      child: const Row(
                        children: [
                          Icon(Icons.fiber_manual_record,
                              color: Colors.white, size: 16),
                          SizedBox(width: 4),
                          Text('REC',
                              style: TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
      floatingActionButton: _currentPosition == null
          ? null // 로딩 중에는 버튼 숨김
          : Column(
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
