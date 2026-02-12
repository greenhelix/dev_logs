import 'package:cloud_firestore/cloud_firestore.dart';
import '../domain/person_model.dart';

class PersonFirestoreRepository {
  final FirebaseFirestore _firestore;

  PersonFirestoreRepository({required FirebaseFirestore firestore})
      : _firestore = firestore;

  CollectionReference get _collection => _firestore.collection('people');

  Future<void> addPerson(PersonModel person) async {
    // ID가 이미 있다면 지정해서 생성, 없으면 자동 생성 후 업데이트는 UI 로직에 따라 결정
    if (person.id.isNotEmpty) {
      await _collection.doc(person.id).set(person.toMap());
    } else {
      await _collection.add(person.toMap());
    }
  }

  Stream<List<PersonModel>> streamPeople() {
    return _collection.snapshots().map((snapshot) {
      return snapshot.docs.map((doc) {
        // ★ doc.id를 두 번째 인자로 전달
        return PersonModel.fromMap(doc.data() as Map<String, dynamic>, doc.id);
      }).toList();
    });
  }

  Future<void> updatePerson(PersonModel person) async {
    await _collection.doc(person.id).update(person.toMap());
  }

  Future<void> deletePerson(String id) async {
    await _collection.doc(id).delete();
  }
}
