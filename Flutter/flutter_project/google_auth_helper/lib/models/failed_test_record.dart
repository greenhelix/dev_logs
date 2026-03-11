class FailedTestRecord {
  const FailedTestRecord({
    required this.id,
    required this.moduleName,
    required this.testCaseName,
    required this.testName,
    required this.failureMessage,
    required this.errorLogSnippet,
    required this.timestamp,
    required this.suiteVersion,
    this.solution = '',
    this.suiteModuleName = '',
    this.className = '',
    this.classSimpleName = '',
    this.deviceSerial = '',
    this.status = 'FAILURE',
    this.manualMemo = '',
    this.excluded = false,
  });

  final String id;
  final String moduleName;
  final String testCaseName;
  final String testName;
  final String failureMessage;
  final String errorLogSnippet;
  final DateTime timestamp;
  final String suiteVersion;
  final String solution;
  final String suiteModuleName;
  final String className;
  final String classSimpleName;
  final String deviceSerial;
  final String status;
  final String manualMemo;
  final bool excluded;

  String get displayModuleName {
    if (classSimpleName.isNotEmpty) {
      return classSimpleName;
    }
    return moduleName;
  }

  FailedTestRecord copyWith({
    String? id,
    String? moduleName,
    String? testCaseName,
    String? testName,
    String? failureMessage,
    String? errorLogSnippet,
    DateTime? timestamp,
    String? suiteVersion,
    String? solution,
    String? suiteModuleName,
    String? className,
    String? classSimpleName,
    String? deviceSerial,
    String? status,
    String? manualMemo,
    bool? excluded,
  }) {
    return FailedTestRecord(
      id: id ?? this.id,
      moduleName: moduleName ?? this.moduleName,
      testCaseName: testCaseName ?? this.testCaseName,
      testName: testName ?? this.testName,
      failureMessage: failureMessage ?? this.failureMessage,
      errorLogSnippet: errorLogSnippet ?? this.errorLogSnippet,
      timestamp: timestamp ?? this.timestamp,
      suiteVersion: suiteVersion ?? this.suiteVersion,
      solution: solution ?? this.solution,
      suiteModuleName: suiteModuleName ?? this.suiteModuleName,
      className: className ?? this.className,
      classSimpleName: classSimpleName ?? this.classSimpleName,
      deviceSerial: deviceSerial ?? this.deviceSerial,
      status: status ?? this.status,
      manualMemo: manualMemo ?? this.manualMemo,
      excluded: excluded ?? this.excluded,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'moduleName': moduleName,
      'testCaseName': testCaseName,
      'testName': testName,
      'failureMessage': failureMessage,
      'errorLogSnippet': errorLogSnippet,
      'timestamp': timestamp,
      'suiteVersion': suiteVersion,
      'solution': solution,
      'suiteModuleName': suiteModuleName,
      'className': className,
      'classSimpleName': classSimpleName,
      'deviceSerial': deviceSerial,
      'status': status,
      'manualMemo': manualMemo,
      'excluded': excluded,
    };
  }

  factory FailedTestRecord.fromMap(Map<String, dynamic> map) {
    return FailedTestRecord(
      id: map['id'] as String? ?? '',
      moduleName: map['moduleName'] as String? ?? '',
      testCaseName: map['testCaseName'] as String? ?? '',
      testName: map['testName'] as String? ?? '',
      failureMessage: map['failureMessage'] as String? ?? '',
      errorLogSnippet: map['errorLogSnippet'] as String? ?? '',
      timestamp: DateTime.tryParse(map['timestamp'] as String? ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
      suiteVersion: map['suiteVersion'] as String? ?? '',
      solution: map['solution'] as String? ?? '',
      suiteModuleName: map['suiteModuleName'] as String? ?? '',
      className: map['className'] as String? ?? '',
      classSimpleName: map['classSimpleName'] as String? ?? '',
      deviceSerial: map['deviceSerial'] as String? ?? '',
      status: map['status'] as String? ?? 'FAILURE',
      manualMemo: map['manualMemo'] as String? ?? '',
      excluded: map['excluded'] as bool? ?? false,
    );
  }
}
