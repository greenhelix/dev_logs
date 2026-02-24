class CertTestModel {
  final String id; // Document ID (e.g., GTS_Android14_ModuleA_TestCaseB)
  final String suite; // e.g., 'CTS', 'GTS', 'VTS', 'STS'
  final String androidVersion; // e.g., '14', '15'
  final String module; // e.g., 'GtsMediaTestCases'
  final String testCase; // e.g., 'com.google.android.media.gts.MediaTest#testVideo'
  final List<String> tags; // Keywords for searching
  final List<String> failLogs; // Accumulated fail log snippets for this test
  final List<String> resolutionIds; // Linked resolution IDs

  CertTestModel({
    required this.id,
    required this.suite,
    required this.androidVersion,
    required this.module,
    required this.testCase,
    this.tags = const [],
    this.failLogs = const [],
    this.resolutionIds = const [],
  });

  // Convert from Map (Firestore)
  factory CertTestModel.fromMap(Map<String, dynamic> map, String docId) {
    return CertTestModel(
      id: docId,
      suite: map['suite'] ?? '',
      androidVersion: map['androidVersion'] ?? '',
      module: map['module'] ?? '',
      testCase: map['testCase'] ?? '',
      tags: List<String>.from(map['tags'] ?? []),
      failLogs: List<String>.from(map['failLogs'] ?? []),
      resolutionIds: List<String>.from(map['resolutionIds'] ?? []),
    );
  }

  // Convert to Map (Firestore)
  Map<String, dynamic> toMap() {
    return {
      'suite': suite,
      'androidVersion': androidVersion,
      'module': module,
      'testCase': testCase,
      'tags': tags,
      'failLogs': failLogs,
      'resolutionIds': resolutionIds,
    };
  }
}