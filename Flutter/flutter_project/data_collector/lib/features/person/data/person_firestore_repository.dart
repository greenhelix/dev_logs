import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../domain/person_model.dart';

// ★ Provider 교체 포인트 ★
// 나중에 로컬로 돌아가고 싶으면 이 Provider의 반환값만 Drift Repository로 바꾸면 됨 (인터페이스 통일 시)
final personRepositoryProvider = Provider<PersonFirestoreRepository>((ref) {
  final firestore = FirebaseFirestore.instanceFor(
    app: Firebase.app(),
    databaseId: 'db-data-collector', // default안쓰는 경우 따로 지정해야한다.
  );
  return PersonFirestoreRepository(firestore);
  // return PersonFirestoreRepository(FirebaseFirestore.instance); //default 인 경우
});

class PersonFirestoreRepository {
  final FirebaseFirestore _firestore;

  PersonFirestoreRepository(this._firestore);

  CollectionReference<Map<String, dynamic>> get _peopleRef =>
      _firestore.collection('people');

  // 저장 (Create)
  Future<void> addPerson({
    required String name,
    int? age,
    String? photoUrl,
    Map<String, dynamic> attributes = const {},
  }) async {
    await _peopleRef.add({
      'name': name,
      'age': age,
      'photoUrl': photoUrl,
      'attributes': attributes,
      'createdAt': FieldValue.serverTimestamp(), // 정렬용 타임스탬프
    });
  }

  // 목록 조회 (Read - Realtime Stream)
  // Firestore의 장점인 '실시간 업데이트'를 살리기 위해 Stream으로 반환합니다!
  Stream<List<PersonModel>> getPeopleStream() {
    return _peopleRef
        .orderBy('createdAt', descending: true)
        .snapshots()
        .map((snapshot) {
      return snapshot.docs.map((doc) {
        return PersonModel.fromMap(doc.data(), doc.id);
      }).toList();
    });
  }

  // 단발성 조회 (Future) - 필요할 경우 사용
  Future<List<PersonModel>> getAllPeople() async {
    final snapshot =
        await _peopleRef.orderBy('createdAt', descending: true).get();
    return snapshot.docs
        .map((doc) => PersonModel.fromMap(doc.data(), doc.id))
        .toList();
  }

  // 삭제
  Future<void> deletePerson(String id) async {
    await _peopleRef.doc(id).delete();
  }
}
