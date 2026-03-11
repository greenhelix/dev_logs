import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/services/xts_live_log_parser.dart';
import 'package:google_auth_helper/services/xts_result_parser.dart';
import 'package:google_auth_helper/services/xts_tf_output_parser.dart';

void main() {
  group('XtsLiveLogParser', () {
    test('extracts command, shard count, devices, and finished state', () {
      const source = '''
command_line_args: "cts-validation --shard-count 2 -s serial1 -s serial2"
xts_root_dir: "/home/innopia/xts/cts/android-cts-14_r10-linux_x86-arm"
device_serial: "serial1"
device_serial: "serial2"
xts_suite_info { key: "suite_name" value: "CTS" }
xts_suite_info { key: "suite_version" value: "14_r10" }
total_execution_time { seconds: 49808 nanos: 123 }
state_summary: "running cts on build(s)"
''';

      final result = XtsLiveLogParser().parseText(source);

      expect(result.commandLine, contains('cts-validation'));
      expect(result.shardCount, 2);
      expect(result.devices, ['serial1', 'serial2']);
      expect(result.suiteVersion, '14_r10');
      expect(result.totalExecutionSeconds, 49808);
      expect(result.stage.name, 'finished');
    });
  });

  group('XtsTfOutputParser', () {
    test('extracts summary and failure snippets', () {
      const log = '''
01-06 07:49:13 I/ModuleListener: [40/137] serial1 android.net.wifi.cts.WifiLocationInfoBackgroundTest#testTransportInfoRetrievalAllowedWithBackgroundLocationPermission FAILURE: expected to be true
\tat android.net.wifi.cts.WaitForResultActivity.waitForServiceResult(WaitForResultActivity.java:126)
\tat android.net.wifi.cts.WifiLocationInfoBackgroundTest.startBgServiceAndAssertStatusIs(WifiLocationInfoBackgroundTest.java:192)
Total Tests       : 10
PASSED            : 7
FAILED            : 1
IGNORED           : 1
ASSUMPTION_FAILURE: 1
''';

      final result = XtsTfOutputParser().parseText(log);

      expect(result.totalTests, 10);
      expect(result.passCount, 7);
      expect(result.failCount, 1);
      expect(result.ignoredCount, 1);
      expect(result.assumptionFailureCount, 1);
      expect(result.failures.single.classSimpleName,
          'WifiLocationInfoBackgroundTest');
      expect(
        result.failures.single.logSnippet,
        contains('FAILURE: expected to be true'),
      );
    });
  });

  group('XtsResultParser', () {
    test('prefers tf output summary and enriches build metadata', () {
      const xml = '''<?xml version='1.0' encoding='UTF-8' standalone='no' ?>
<Result start="1767596648026" end="1767655280678" suite_name="CTS" suite_version="14_r10" devices="serial1,serial2">
  <Build build_version_incremental="403_1" build_device="IMTM8300_HU" build_fingerprint="TelekomTV/IMTM8300_HU/IMTM8300_HU:14/UTT2.250604.001/403_1:user/release-keys" build_version_release="14" build_type="user" />
  <Summary pass="2" failed="4" warning="0" />
  <Module name="CtsNetTestCases" abi="arm64-v8a">
    <TestCase name="android.net.wifi.cts.WifiLocationInfoBackgroundTest">
      <Test result="pass" name="testOne" />
      <Test result="fail" name="testTransportInfoRetrievalAllowedWithBackgroundLocationPermission">
        <Failure message="xml failure">
          <StackTrace>java.lang.AssertionError: boom</StackTrace>
        </Failure>
      </Test>
      <Test result="IGNORED" name="testThree" skipped="true" />
    </TestCase>
  </Module>
</Result>
''';
      const log = '''
01-06 07:49:13 I/ModuleListener: [40/137] serial1 android.net.wifi.cts.WifiLocationInfoBackgroundTest#testTransportInfoRetrievalAllowedWithBackgroundLocationPermission FAILURE: expected to be true
\tat android.net.wifi.cts.WaitForResultActivity.waitForServiceResult(WaitForResultActivity.java:126)
\tat android.net.wifi.cts.WifiLocationInfoBackgroundTest.startBgServiceAndAssertStatusIs(WifiLocationInfoBackgroundTest.java:192)
Total Tests       : 10
PASSED            : 7
FAILED            : 1
IGNORED           : 1
ASSUMPTION_FAILURE: 1
''';

      final result = XtsResultParser().parseText(
        xml,
        tfOutput: XtsTfOutputParser().parseText(log),
      );

      expect(result.metric.suiteVersion, '14_r10');
      expect(result.metric.fwVersion, '403_1');
      expect(result.metric.buildDevice, 'IMTM8300_HU');
      expect(result.metric.androidVersion, '14');
      expect(result.metric.buildType, 'user');
      expect(
        result.metric.primaryBuildLabel,
        'TelekomTV/IMTM8300_HU/IMTM8300_HU:14/UTT2.250604.001/403_1:user/release-keys',
      );
      expect(result.metric.compactBuildLabel, contains('IMTM8300_HU'));
      expect(result.metric.countSource, 'xts_tf_output.log');
      expect(result.metric.failCount, 1);
      expect(result.metric.passCount, 7);
      expect(result.metric.ignoredCount, 1);
      expect(result.metric.assumptionFailureCount, 1);
      expect(result.testCases.single.testMethodCount, 3);
      expect(result.failedTests.single.testName,
          'testTransportInfoRetrievalAllowedWithBackgroundLocationPermission');
      expect(result.failedTests.single.moduleName,
          'WifiLocationInfoBackgroundTest');
      expect(result.failedTests.single.suiteModuleName, 'CtsNetTestCases');
      expect(result.failedTests.single.deviceSerial, 'serial1');
      expect(result.failedTests.single.errorLogSnippet,
          contains('FAILURE: expected to be true'));
      expect(result.warnings, isNotEmpty);
    });
  });
}
