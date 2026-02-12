import '../domain/person_model.dart';

abstract class PersonRepository {
  Future<void> addPerson(PersonModel person);
  Future<List<PersonModel>> getPeople();
  Stream<List<PersonModel>> streamPeople();
  // ▼ 추가된 기능
  Future<void> updatePerson(PersonModel person);
  Future<void> deletePerson(String id);
}
