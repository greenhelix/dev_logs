import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:geolocator/geolocator.dart';
import '../domain/location_model.dart';

class LocationFirestoreRepository {
  final FirebaseFirestore _firestore;

  LocationFirestoreRepository({required FirebaseFirestore firestore})
      : _firestore = firestore;

  // 컬렉션: locations
  CollectionReference get _collection => _firestore.collection('locations');

  // 1. 위치 저장 (Firestore)
  Future<void> addLocation(LocationModel location) async {
    await _collection.doc(location.id).set(location.toMap());
  }

  // 2. 위치 목록 스트림 (Firestore -> Map Polyline용)
  Stream<List<LocationModel>> streamLocations() {
    return _collection
        .orderBy('timestamp', descending: true) // 최신순 정렬
        .snapshots()
        .map((snapshot) {
      return snapshot.docs
          .map((doc) =>
              LocationModel.fromMap(doc.data() as Map<String, dynamic>))
          .toList();
    });
  }

  // 3. 위치 삭제
  Future<void> deleteLocation(String id) async {
    await _collection.doc(id).delete();
  }

  // 4. 전체 삭제 (초기화용)
  Future<void> deleteAllLocations() async {
    final snapshot = await _collection.get();
    final batch = _firestore.batch();
    for (var doc in snapshot.docs) {
      batch.delete(doc.reference);
    }
    await batch.commit();
  }

  // --- GPS 기능 (Geolocator) ---

  // 현재 위치 1회 조회
  Future<Position> getCurrentPosition() async {
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) throw Exception('Location services are disabled.');

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied)
        throw Exception('Location permissions are denied');
    }

    return await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high);
  }

  // 위치 실시간 스트림 (자동 추적용)
  Stream<Position> streamPosition({int distanceFilter = 10}) {
    return Geolocator.getPositionStream(
      locationSettings: LocationSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: distanceFilter, // 10미터 이동 시마다 업데이트
      ),
    );
  }
}
