import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:drift/drift.dart'; // Drift의 확장 함수(.insert) 등을 위해 필요
import 'package:uuid/uuid.dart';
import '../../../data/local/app_database.dart'; // DB 클래스 import
import '../../../data/providers.dart'; // databaseProvider import

// 1. Repository Provider 정의
final personRepositoryProvider = Provider<PersonRepository>((ref) {
  final db = ref.watch(databaseProvider);
  return PersonRepository(db);
});

// 2. Repository 클래스
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
