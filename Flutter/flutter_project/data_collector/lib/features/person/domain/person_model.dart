class PersonModel {
  final String id;
  final String name;
  final int? age;
  final String? photoUrl;
  final Map<String, dynamic> attributes;

  PersonModel({
    required this.id,
    required this.name,
    this.age,
    this.photoUrl,
    this.attributes = const {},
  });

  // Firestore 데이터 -> 객체 변환
  factory PersonModel.fromMap(Map<String, dynamic> map, String docId) {
    return PersonModel(
      id: docId,
      name: map['name'] ?? '',
      age: map['age'] as int?,
      photoUrl: map['photoUrl'] as String?,
      // Firestore의 Map을 Dart Map으로 안전하게 변환
      attributes: Map<String, dynamic>.from(map['attributes'] ?? {}),
    );
  }

  // 객체 -> Firestore 저장용 Map 변환
  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'age': age,
      'photoUrl': photoUrl,
      'attributes': attributes,
    };
  }
}
