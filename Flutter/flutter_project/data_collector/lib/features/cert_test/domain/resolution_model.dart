class ResolutionModel {
  final String id; // Document ID
  final String title; // Short summary of the fix
  final String description; // Detailed steps to resolve
  final List<String> appliedTestIds; // List of CertTestModel IDs this applies to
  final DateTime createdAt;
  final DateTime updatedAt;

  ResolutionModel({
    required this.id,
    required this.title,
    required this.description,
    this.appliedTestIds = const [],
    required this.createdAt,
    required this.updatedAt,
  });

  // Convert from Map (Firestore)
  factory ResolutionModel.fromMap(Map<String, dynamic> map, String docId) {
    return ResolutionModel(
      id: docId,
      title: map['title'] ?? '',
      description: map['description'] ?? '',
      appliedTestIds: List<String>.from(map['appliedTestIds'] ?? []),
      createdAt: map['createdAt'] != null 
          ? DateTime.fromMillisecondsSinceEpoch(map['createdAt']) 
          : DateTime.now(),
      updatedAt: map['updatedAt'] != null 
          ? DateTime.fromMillisecondsSinceEpoch(map['updatedAt']) 
          : DateTime.now(),
    );
  }

  // Convert to Map (Firestore)
  Map<String, dynamic> toMap() {
    return {
      'title': title,
      'description': description,
      'appliedTestIds': appliedTestIds,
      'createdAt': createdAt.millisecondsSinceEpoch,
      'updatedAt': updatedAt.millisecondsSinceEpoch,
    };
  }
}