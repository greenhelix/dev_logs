import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import '../../../data/providers.dart';
import '../domain/track_record_model.dart';

class TrackingDetailScreen extends ConsumerStatefulWidget {
  final TrackRecordModel record;

  const TrackingDetailScreen({Key? key, required this.record}) : super(key: key);

  @override
  ConsumerState<TrackingDetailScreen> createState() => _TrackingDetailScreenState();
}

class _TrackingDetailScreenState extends ConsumerState<TrackingDetailScreen> {
  final _memoCtrl = TextEditingController();
  GoogleMapController? _mapController;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _memoCtrl.text = widget.record.memo ?? '';
  }

  @override
  void dispose() {
    _memoCtrl.dispose();
    _mapController?.dispose();
    super.dispose();
  }

  String _formatDateTime(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} '
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  String _formatDuration(DateTime start, DateTime end) {
    final diff = end.difference(start);
    final h = diff.inHours;
    final m = diff.inMinutes % 60;
    final s = diff.inSeconds % 60;
    if (h > 0) return '${h}시간 ${m}분';
    if (m > 0) return '${m}분 ${s}초';
    return '${s}초';
  }

  // Calculate the center of all path points for initial camera position
  LatLng _getCenter(List<LatLng> points) {
    double lat = points.map((p) => p.latitude).reduce((a, b) => a + b) / points.length;
    double lng = points.map((p) => p.longitude).reduce((a, b) => a + b) / points.length;
    return LatLng(lat, lng);
  }

  // Zoom to fit all path points within the map bounds
  void _fitBounds(List<LatLng> points) {
    if (_mapController == null || points.isEmpty) return;

    double minLat = points.map((p) => p.latitude).reduce((a, b) => a < b ? a : b);
    double maxLat = points.map((p) => p.latitude).reduce((a, b) => a > b ? a : b);
    double minLng = points.map((p) => p.longitude).reduce((a, b) => a < b ? a : b);
    double maxLng = points.map((p) => p.longitude).reduce((a, b) => a > b ? a : b);

    _mapController!.animateCamera(
      CameraUpdate.newLatLngBounds(
        LatLngBounds(
          southwest: LatLng(minLat, minLng),
          northeast: LatLng(maxLat, maxLng),
        ),
        60, // padding in pixels
      ),
    );
  }

  Future<void> _saveMemo() async {
    setState(() => _isSaving = true);
    try {
      await ref.read(trackRecordRepositoryProvider).updateTrackRecordMemo(
        widget.record.id,
        _memoCtrl.text.trim(),
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('메모가 저장되었습니다.'), backgroundColor: Colors.green),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('저장 실패: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final record = widget.record;
    final center = record.pathPoints.isNotEmpty
        ? _getCenter(record.pathPoints)
        : const LatLng(37.5665, 126.9780);

    return Scaffold(
      appBar: AppBar(
        title: Text(record.title, overflow: TextOverflow.ellipsis),
        actions: [
          if (_isSaving)
            const Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator(strokeWidth: 2))
          else
            IconButton(
              icon: const Icon(Icons.save_outlined),
              onPressed: _saveMemo,
              tooltip: '메모 저장',
            ),
        ],
      ),
      body: Column(
        children: [
          // Info card (summary)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            color: Colors.blue[50],
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _InfoChip(label: '거리', value: '${record.totalDistance.toStringAsFixed(2)}km', icon: Icons.route),
                _InfoChip(
                  label: '소요 시간',
                  value: _formatDuration(record.startTime, record.endTime),
                  icon: Icons.timer_outlined,
                ),
                _InfoChip(label: '좌표 수', value: '${record.pathPoints.length}pt', icon: Icons.location_on),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
            color: Colors.blue[50],
            child: Row(
              children: [
                const Icon(Icons.play_arrow, size: 16, color: Colors.green),
                const SizedBox(width: 4),
                Text(_formatDateTime(record.startTime), style: const TextStyle(fontSize: 12)),
                const SizedBox(width: 12),
                const Icon(Icons.stop, size: 16, color: Colors.red),
                const SizedBox(width: 4),
                Text(_formatDateTime(record.endTime), style: const TextStyle(fontSize: 12)),
              ],
            ),
          ),

          // Google Map with route polyline
          Expanded(
            child: GoogleMap(
              initialCameraPosition: CameraPosition(target: center, zoom: 14),
              onMapCreated: (controller) {
                _mapController = controller;
                // Auto-fit bounds after map is ready
                Future.delayed(const Duration(milliseconds: 300), () {
                  _fitBounds(record.pathPoints);
                });
              },
              polylines: record.pathPoints.length > 1
                  ? {
                      Polyline(
                        polylineId: const PolylineId('route'),
                        color: Colors.blue,
                        width: 6,
                        points: record.pathPoints,
                      ),
                    }
                  : {},
              markers: {
                if (record.pathPoints.isNotEmpty)
                  Marker(
                    markerId: const MarkerId('start'),
                    position: record.startLocation,
                    icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueGreen),
                    infoWindow: InfoWindow(
                      title: '출발',
                      snippet: _formatDateTime(record.startTime),
                    ),
                  ),
                if (record.pathPoints.isNotEmpty)
                  Marker(
                    markerId: const MarkerId('end'),
                    position: record.endLocation,
                    icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRed),
                    infoWindow: InfoWindow(
                      title: '도착',
                      snippet: _formatDateTime(record.endTime),
                    ),
                  ),
              },
              zoomControlsEnabled: true,
              myLocationButtonEnabled: false,
            ),
          ),

          // Memo input area
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
            child: TextField(
              controller: _memoCtrl,
              maxLines: 3,
              decoration: InputDecoration(
                labelText: '메모',
                hintText: '이 트래킹에 대한 메모를 남겨보세요...',
                border: const OutlineInputBorder(),
                suffixIcon: IconButton(
                  icon: const Icon(Icons.save),
                  onPressed: _saveMemo,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// Info chip widget for summary display
class _InfoChip extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;

  const _InfoChip({required this.label, required this.value, required this.icon});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Icon(icon, color: Colors.blue[700], size: 20),
        const SizedBox(height: 4),
        Text(value, style: TextStyle(fontWeight: FontWeight.bold, color: Colors.blue[900])),
        Text(label, style: const TextStyle(fontSize: 11, color: Colors.blueGrey)),
      ],
    );
  }
}
