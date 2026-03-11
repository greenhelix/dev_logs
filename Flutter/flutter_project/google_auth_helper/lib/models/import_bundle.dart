import 'failed_test_record.dart';
import 'live_status.dart';
import 'test_case_record.dart';
import 'test_metric_record.dart';

class ImportBundle {
  const ImportBundle({
    required this.metric,
    required this.testCases,
    required this.failedTests,
    required this.liveStatus,
    required this.resultPath,
    required this.logPath,
    this.previewWarnings = const [],
  });

  final TestMetricRecord metric;
  final List<TestCaseRecord> testCases;
  final List<FailedTestRecord> failedTests;
  final LiveStatus liveStatus;
  final String resultPath;
  final String? logPath;
  final List<String> previewWarnings;

  List<FailedTestRecord> get activeFailedTests {
    return failedTests.where((item) => !item.excluded).toList(growable: false);
  }

  List<FailedTestRecord> get excludedFailedTests {
    return failedTests.where((item) => item.excluded).toList(growable: false);
  }

  ImportBundle copyWith({
    TestMetricRecord? metric,
    List<TestCaseRecord>? testCases,
    List<FailedTestRecord>? failedTests,
    LiveStatus? liveStatus,
    String? resultPath,
    String? logPath,
    List<String>? previewWarnings,
  }) {
    return ImportBundle(
      metric: metric ?? this.metric,
      testCases: testCases ?? this.testCases,
      failedTests: failedTests ?? this.failedTests,
      liveStatus: liveStatus ?? this.liveStatus,
      resultPath: resultPath ?? this.resultPath,
      logPath: logPath ?? this.logPath,
      previewWarnings: previewWarnings ?? this.previewWarnings,
    );
  }
}
