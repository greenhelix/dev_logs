import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:data_accumulator_app/features/person/data/person_firestore_repository.dart';
import 'package:data_accumulator_app/features/person/domain/person_model.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:drift/drift.dart'; // Drift의 확장 함수(.insert) 등을 위해 필요
import 'package:uuid/uuid.dart';
import '../../../data/local/app_database.dart'; // DB 클래스 import
// import '../../../data/providers.dart'; // databaseProvider import

// 2026.02.08.일요일
// 이건 테스트용 repository로 사용했던 코드이다. 로컬에서 firebase연동 없이 사용할때 사용한다.
// 이제는 firebase와 연동을 해야하니 백업을 해두고 새로운 파일로 옮긴다.

// 1. Repository Provider 정의

// 로컬데이터를 사용하는 경우 이부분을 활성화하고, firestore것은 비활성화 해야한다.
// personRepositoryProvider가 겹침 명칭 달라도 되긴하는데 오히려 복잡해서 그냥 주석함.
// final personRepositoryProvider = Provider<PersonRepository>((ref) {
//   final db = ref.watch(databaseProvider);
//   return PersonRepository(db);
// });

// 2. Repository 클래스
class PersonFirestoreRepository {
  final FirebaseFirestore _firestore;
  PersonFirestoreRepository(this._firestore);

  // Firestore 에 컬렉션 지정
  CollectionReference<Map<String, dynamic>> get _peopleRef =>
      _firestore.collection('people');

  // 저장(create)
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
      'createdAt': FieldValue.serverTimestamp(),
    });
  }

  // 단발성 조회(Future) -필요한 경우 사용
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
} // PersonFirestoreRepository

class PersonRepository {
  final AppDatabase _db;

  PersonRepository(this._db);

  // 인물 저장 (Create)
  Future<void> addPerson({
    required String name,
    int? age,
    String? photoUrl,
    Map<String, dynamic> attributes = const {},
  }) async {
    await _db.into(_db.people).insert(
          PeopleCompanion.insert(
            id: const Uuid().v4(), // 고유 ID 자동 생성
            name: name,
            age: Value(age), // Nullable 필드는 Value()로 감쌈
            photoUrl: Value(photoUrl),
            attributes: attributes,
          ),
        );
  }

  // 인물 목록 조회 (Read)
  Future<List<Person>> getAllPeople() async {
    return await _db.select(_db.people).get();
  }

  // 인물 검색 (Search)
  Future<List<Person>> searchPeople(String query) async {
    return await (_db.select(_db.people)
          ..where((tbl) => tbl.name.contains(query)))
        .get();
  }

  // 인물 삭제 (Delete)
  Future<void> deletePerson(String id) async {
    await (_db.delete(_db.people)..where((tbl) => tbl.id.equals(id))).go();
  }
}
