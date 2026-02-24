import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

class TrackRecordModel {
  final String id;
  final String title; // "2026-02-20 서울 산책" 등
  final DateTime startTime;
  final DateTime endTime;
  final LatLng startLocation;
  final LatLng endLocation;
  final List<LatLng> pathPoints; // 전체 경로 좌표 배열
  final String? memo; // 사용자 메모
  final double totalDistance; // 총 거리 (km)

  TrackRecordModel({
    required this.id,
    required this.title,
    required this.startTime,
    required this.endTime,
    required this.startLocation,
    required this.endLocation,
    required this.pathPoints,
    this.memo,
    required this.totalDistance,
  });

  Map<String, dynamic> toMap() {
    return {
      'title': title,
      'startTime': startTime,
      'endTime': endTime,
      'startLocation': {'lat': startLocation.latitude, 'lng': startLocation.longitude},
      'endLocation': {'lat': endLocation.latitude, 'lng': endLocation.longitude},
      'pathPoints': pathPoints.map((p) => {'lat': p.latitude, 'lng': p.longitude}).toList(),
      'memo': memo,
      'totalDistance': totalDistance,
    };
  }

  factory TrackRecordModel.fromMap(Map<String, dynamic> map, String id) {
    final startLoc = map['startLocation'] as Map<String, dynamic>;
    final endLoc = map['endLocation'] as Map<String, dynamic>;
    final pathPoints = (map['pathPoints'] as List<dynamic>)
        .map((p) => LatLng(p['lat'], p['lng']))
        .toList();

    return TrackRecordModel(
      id: id,
      title: map['title'] ?? '',
      startTime: (map['startTime'] as Timestamp).toDate(),
      endTime: (map['endTime'] as Timestamp).toDate(),
      startLocation: LatLng(startLoc['lat'], startLoc['lng']),
      endLocation: LatLng(endLoc['lat'], endLoc['lng']),
      pathPoints: pathPoints,
      memo: map['memo'],
      totalDistance: map['totalDistance']?.toDouble() ?? 0.0,
    );
  }
}
