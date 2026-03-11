class TestMetricRecord {
  const TestMetricRecord({
    required this.id,
    required this.toolType,
    required this.suiteName,
    required this.suiteVersion,
    required this.fwVersion,
    required this.totalTests,
    required this.passCount,
    required this.failCount,
    required this.ignoredCount,
    required this.assumptionFailureCount,
    required this.warningCount,
    required this.moduleCount,
    required this.durationSeconds,
    required this.devices,
    required this.timestamp,
    this.buildDevice = '',
    this.androidVersion = '',
    this.buildType = '',
    this.buildFingerprint = '',
    this.countSource = 'xml',
    this.excludedFailureCount = 0,
  });

  final String id;
  final String toolType;
  final String suiteName;
  final String suiteVersion;
  final String fwVersion;
  final int totalTests;
  final int passCount;
  final int failCount;
  final int ignoredCount;
  final int assumptionFailureCount;
  final int warningCount;
  final int moduleCount;
  final int durationSeconds;
  final List<String> devices;
  final DateTime timestamp;
  final String buildDevice;
  final String androidVersion;
  final String buildType;
  final String buildFingerprint;
  final String countSource;
  final int excludedFailureCount;

  String get primaryBuildLabel {
    if (buildFingerprint.isNotEmpty) {
      return buildFingerprint;
    }
    if (fwVersion.isNotEmpty) {
      return fwVersion;
    }
    if (buildDevice.isNotEmpty) {
      return buildDevice;
    }
    return '-';
  }

  String get compactBuildLabel {
    final segments = <String>[
      if (buildDevice.isNotEmpty) buildDevice,
      if (fwVersion.isNotEmpty) fwVersion,
      if (androidVersion.isNotEmpty) 'Android $androidVersion',
    ];
    return segments.isEmpty ? primaryBuildLabel : segments.join(' / ');
  }

  int get reportedFailCount {
    final next = failCount - excludedFailureCount;
    return next < 0 ? 0 : next;
  }

  TestMetricRecord copyWith({
    String? id,
    String? toolType,
    String? suiteName,
    String? suiteVersion,
    String? fwVersion,
    int? totalTests,
    int? passCount,
    int? failCount,
    int? ignoredCount,
    int? assumptionFailureCount,
    int? warningCount,
    int? moduleCount,
    int? durationSeconds,
    List<String>? devices,
    DateTime? timestamp,
    String? buildDevice,
    String? androidVersion,
    String? buildType,
    String? buildFingerprint,
    String? countSource,
    int? excludedFailureCount,
  }) {
    return TestMetricRecord(
      id: id ?? this.id,
      toolType: toolType ?? this.toolType,
      suiteName: suiteName ?? this.suiteName,
      suiteVersion: suiteVersion ?? this.suiteVersion,
      fwVersion: fwVersion ?? this.fwVersion,
      totalTests: totalTests ?? this.totalTests,
      passCount: passCount ?? this.passCount,
      failCount: failCount ?? this.failCount,
      ignoredCount: ignoredCount ?? this.ignoredCount,
      assumptionFailureCount:
          assumptionFailureCount ?? this.assumptionFailureCount,
      warningCount: warningCount ?? this.warningCount,
      moduleCount: moduleCount ?? this.moduleCount,
      durationSeconds: durationSeconds ?? this.durationSeconds,
      devices: devices ?? this.devices,
      timestamp: timestamp ?? this.timestamp,
      buildDevice: buildDevice ?? this.buildDevice,
      androidVersion: androidVersion ?? this.androidVersion,
      buildType: buildType ?? this.buildType,
      buildFingerprint: buildFingerprint ?? this.buildFingerprint,
      countSource: countSource ?? this.countSource,
      excludedFailureCount: excludedFailureCount ?? this.excludedFailureCount,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'toolType': toolType,
      'suiteName': suiteName,
      'suiteVersion': suiteVersion,
      'fwVersion': fwVersion,
      'totalTests': totalTests,
      'passCount': passCount,
      'failCount': failCount,
      'ignoredCount': ignoredCount,
      'assumptionFailureCount': assumptionFailureCount,
      'warningCount': warningCount,
      'moduleCount': moduleCount,
      'durationSeconds': durationSeconds,
      'devices': devices,
      'timestamp': timestamp,
      'buildDevice': buildDevice,
      'androidVersion': androidVersion,
      'buildType': buildType,
      'buildFingerprint': buildFingerprint,
      'countSource': countSource,
      'excludedFailureCount': excludedFailureCount,
    };
  }

  factory TestMetricRecord.fromMap(Map<String, dynamic> map) {
    final suiteName = map['suiteName'] as String? ?? '';
    return TestMetricRecord(
      id: map['id'] as String? ?? '',
      toolType: map['toolType'] as String? ?? _deriveToolType(suiteName),
      suiteName: suiteName,
      suiteVersion: map['suiteVersion'] as String? ?? '',
      fwVersion: map['fwVersion'] as String? ?? '',
      totalTests: (map['totalTests'] as num?)?.toInt() ?? 0,
      passCount: (map['passCount'] as num?)?.toInt() ?? 0,
      failCount: (map['failCount'] as num?)?.toInt() ?? 0,
      ignoredCount: (map['ignoredCount'] as num?)?.toInt() ?? 0,
      assumptionFailureCount:
          (map['assumptionFailureCount'] as num?)?.toInt() ?? 0,
      warningCount: (map['warningCount'] as num?)?.toInt() ?? 0,
      moduleCount: (map['moduleCount'] as num?)?.toInt() ?? 0,
      durationSeconds: (map['durationSeconds'] as num?)?.toInt() ?? 0,
      devices: (map['devices'] as List<dynamic>? ?? const [])
          .map((item) => item.toString())
          .toList(),
      timestamp: DateTime.tryParse(map['timestamp'] as String? ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
      buildDevice: map['buildDevice'] as String? ?? '',
      androidVersion: map['androidVersion'] as String? ?? '',
      buildType: map['buildType'] as String? ?? '',
      buildFingerprint: map['buildFingerprint'] as String? ?? '',
      countSource: map['countSource'] as String? ?? 'xml',
      excludedFailureCount: (map['excludedFailureCount'] as num?)?.toInt() ?? 0,
    );
  }

  static String _deriveToolType(String suiteName) {
    final upper = suiteName.toUpperCase();
    if (upper.contains('TVTS')) {
      return 'TVTS';
    }
    if (upper.contains('GTS')) {
      return 'GTS';
    }
    if (upper.contains('VTS')) {
      return 'VTS';
    }
    if (upper.contains('STS')) {
      return 'STS';
    }
    if (upper.contains('CTS')) {
      return 'CTS';
    }
    return 'UNKNOWN';
  }
}
