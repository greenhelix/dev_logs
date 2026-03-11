class RedmineProjectSummary {
  const RedmineProjectSummary({
    required this.id,
    required this.identifier,
    required this.name,
    this.description = '',
    this.status = 0,
  });

  final int id;
  final String identifier;
  final String name;
  final String description;
  final int status;

  String get displayLabel {
    final suffix = identifier.isEmpty ? '' : ' ($identifier)';
    return '$name #$id$suffix';
  }

  factory RedmineProjectSummary.fromMap(Map<String, dynamic> map) {
    return RedmineProjectSummary(
      id: (map['id'] as num?)?.toInt() ?? 0,
      identifier: map['identifier'] as String? ?? '',
      name: map['name'] as String? ?? '',
      description: map['description'] as String? ?? '',
      status: (map['status'] as num?)?.toInt() ?? 0,
    );
  }
}
