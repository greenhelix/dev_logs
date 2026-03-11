class TestCaseRecord {
  const TestCaseRecord({
    required this.id,
    required this.moduleName,
    required this.testCaseName,
    required this.testMethodCount,
    required this.suiteName,
    required this.suiteVersion,
    this.description = '',
  });

  final String id;
  final String moduleName;
  final String testCaseName;
  final int testMethodCount;
  final String suiteName;
  final String suiteVersion;
  final String description;

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'moduleName': moduleName,
      'testCaseName': testCaseName,
      'testMethodCount': testMethodCount,
      'suiteName': suiteName,
      'suiteVersion': suiteVersion,
      'description': description,
    };
  }

  factory TestCaseRecord.fromMap(Map<String, dynamic> map) {
    return TestCaseRecord(
      id: map['id'] as String? ?? '',
      moduleName: map['moduleName'] as String? ?? '',
      testCaseName: map['testCaseName'] as String? ?? '',
      testMethodCount: (map['testMethodCount'] as num?)?.toInt() ?? 0,
      suiteName: map['suiteName'] as String? ?? '',
      suiteVersion: map['suiteVersion'] as String? ?? '',
      description: map['description'] as String? ?? '',
    );
  }
}
