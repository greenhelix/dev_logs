// lib/features/maps/presentation/map_tracker_screen.dart

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import '../../../data/providers.dart';
import '../domain/track_record_model.dart';

class MapTrackerScreen extends ConsumerStatefulWidget {
  const MapTrackerScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<MapTrackerScreen> createState() => _MapTrackerScreenState();
}

class _MapTrackerScreenState extends ConsumerState<MapTrackerScreen> {
  GoogleMapController? _mapController;

  final Set<Marker> _markers = {};
  final Set<Polyline> _polylines = {};

  // Current position (nullable - loading until fetched)
  Position? _currentPosition;

  // Tracking state
  bool _isTracking = false;
  StreamSubscription<Position>? _positionStream;

  // Track data collected during a session
  final List<LatLng> _trackPoints = [];
  DateTime? _startTime;
  String _trackId = '';

  // Default fallback location (Seoul)
  static const LatLng _defaultLocation = LatLng(37.5665, 126.9780);

  @override
  void initState() {
    super.initState();
    _getCurrentLocation();
  }

  @override
  void dispose() {
    _positionStream?.cancel();
    _mapController?.dispose();
    super.dispose();
  }

  // Fetch initial position using locationRepositoryProvider
  Future<void> _getCurrentLocation() async {
    try {
      final repo = ref.read(locationRepositoryProvider);
      final position = await repo.getCurrentPosition();
      if (!mounted) return;
      setState(() {
        _currentPosition = position;
        _updateMyMarker(position);
      });
      _updateCamera(position);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('위치를 가져올 수 없습니다: $e')),
        );
      }
    }
  }

  // Start tracking: begin collecting position stream
  void _startTracking() {
    setState(() {
      _isTracking = true;
      _trackPoints.clear();
      _startTime = DateTime.now();
      _trackId = DateTime.now().millisecondsSinceEpoch.toString();
    });

    final repo = ref.read(locationRepositoryProvider);
    _positionStream = repo.streamPosition(distanceFilter: 10).listen((position) {
      if (!mounted || !_isTracking) return;
      _onNewPosition(position);
    });

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('트래킹 시작 (REC)'), backgroundColor: Colors.green),
    );
  }

  // Stop tracking: save entire route as one TrackRecord
  Future<void> _stopTracking() async {
    _positionStream?.cancel();

    if (_trackPoints.length < 2) {
      setState(() => _isTracking = false);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('기록된 경로가 너무 짧습니다.')),
      );
      return;
    }

    final endTime = DateTime.now();
    final totalDistance = _calculateDistanceKm(_trackPoints);

    // Format title: "2026-02-25 서울 트래킹" style
    final titleDate = _formatDateTime(_startTime!);
    final record = TrackRecordModel(
      id: _trackId,
      title: '$titleDate 트래킹',
      startTime: _startTime!,
      endTime: endTime,
      startLocation: _trackPoints.first,
      endLocation: _trackPoints.last,
      pathPoints: List.from(_trackPoints),
      totalDistance: totalDistance,
    );

    // Save to Firestore via trackRecordRepositoryProvider
    await ref.read(trackRecordRepositoryProvider).saveTrackRecord(record);

    if (!mounted) return;
    setState(() {
      _isTracking = false;
      _trackPoints.clear();
      _polylines.clear();
      _startTime = null;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('트래킹 완료! ${totalDistance.toStringAsFixed(2)}km 저장됨'),
        backgroundColor: Colors.blue,
      ),
    );
  }

  // Handle new position update during tracking
  void _onNewPosition(Position position) {
    final newLatLng = LatLng(position.latitude, position.longitude);

    setState(() {
      _currentPosition = position;
      _trackPoints.add(newLatLng);

      // Update current position marker
      _updateMyMarker(position);

      // Update live polyline
      _polylines.removeWhere((p) => p.polylineId.value == 'live_track');
      if (_trackPoints.length > 1) {
        _polylines.add(Polyline(
          polylineId: const PolylineId('live_track'),
          color: Colors.blue,
          width: 5,
          points: List.from(_trackPoints),
        ));
      }
    });

    _updateCamera(position);
  }

  void _updateMyMarker(Position position) {
    _markers.removeWhere((m) => m.markerId.value == 'current');
    _markers.add(Marker(
      markerId: const MarkerId('current'),
      position: LatLng(position.latitude, position.longitude),
      icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueBlue),
      infoWindow: const InfoWindow(title: '현재 위치'),
    ));
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

  // Calculate total distance from list of LatLng points (returns km)
  double _calculateDistanceKm(List<LatLng> points) {
    double total = 0.0;
    for (int i = 0; i < points.length - 1; i++) {
      total += Geolocator.distanceBetween(
        points[i].latitude,
        points[i].longitude,
        points[i + 1].latitude,
        points[i + 1].longitude,
      );
    }
    return total / 1000;
  }

  String _formatDateTime(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} '
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    // Show loading until first position is ready
    if (_currentPosition == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Map Tracker')),
        body: const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(),
              SizedBox(height: 16),
              Text('현재 위치를 확인 중입니다...'),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Map Tracker'),
        actions: [
          // Navigate to tracking history screen
          IconButton(
            icon: const Icon(Icons.history),
            onPressed: () => context.push('/maps/history'),
          ),
        ],
      ),
      body: Stack(
        children: [
          GoogleMap(
            initialCameraPosition: CameraPosition(
              target: LatLng(
                _currentPosition?.latitude ?? _defaultLocation.latitude,
                _currentPosition?.longitude ?? _defaultLocation.longitude,
              ),
              zoom: 17.0,
            ),
            onMapCreated: (controller) => _mapController = controller,
            markers: _markers,
            polylines: _polylines,
            myLocationEnabled: true,
            myLocationButtonEnabled: false,
            zoomControlsEnabled: false,
          ),

          // REC badge overlay
          if (_isTracking)
            Positioned(
              top: 20,
              left: 20,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.red,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.fiber_manual_record, color: Colors.white, size: 16),
                    SizedBox(width: 4),
                    Text('REC', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  ],
                ),
              ),
            ),

          // Points count badge during tracking
          if (_isTracking)
            Positioned(
              top: 20,
              right: 20,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: Colors.black54,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  '${_trackPoints.length}pt',
                  style: const TextStyle(color: Colors.white, fontSize: 12),
                ),
              ),
            ),
        ],
      ),
      floatingActionButton: Column(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          // GPS re-center button
          FloatingActionButton(
            heroTag: 'gps',
            onPressed: _getCurrentLocation,
            child: const Icon(Icons.my_location),
          ),
          const SizedBox(height: 12),
          // Start / Stop tracking button
          FloatingActionButton.extended(
            heroTag: 'track',
            onPressed: _isTracking ? _stopTracking : _startTracking,
            backgroundColor: _isTracking ? Colors.red : Colors.blue,
            icon: Icon(_isTracking ? Icons.stop : Icons.play_arrow),
            label: Text(_isTracking ? 'Stop' : 'Start'),
          ),
        ],
      ),
    );
  }
}
