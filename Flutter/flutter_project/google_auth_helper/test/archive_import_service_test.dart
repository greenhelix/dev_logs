import 'dart:convert';
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/services/archive_import_service.dart';
import 'package:google_auth_helper/services/xts_live_log_parser.dart';
import 'package:google_auth_helper/services/xts_result_parser.dart';
import 'package:google_auth_helper/services/xts_tf_output_parser.dart';

void main() {
  test('ArchiveImportService parses split result and log zip uploads',
      () async {
    final service = ArchiveImportService(
      resultParser: XtsResultParser(),
      liveLogParser: XtsLiveLogParser(),
      tfOutputParser: XtsTfOutputParser(),
    );

    final resultZip = _encodeZip({
      'results/test_result.xml': _sampleResultXml,
    });
    final logZip = _encodeZip({
      'logs/xts_tf_output.log': _sampleTfOutputLog,
      'logs/olc_server_session_log.txt': _sampleLiveLog,
    });

    final bundle = await service.importSplitZipBytes(
      resultFileName: '2026.03.09_19.21.45.591_2953.zip',
      resultBytes: resultZip,
      logFileName: '2026.03.09_19.21.45.591_2953.zip',
      logBytes: logZip,
    );

    expect(bundle.metric.totalTests, 10);
    expect(bundle.metric.failCount, 1);
    expect(bundle.metric.fwVersion, '403_1');
    expect(bundle.metric.buildDevice, 'IMTM8300_HU');
    expect(bundle.metric.countSource, 'xts_tf_output.log');
    expect(bundle.resultPath, contains('test_result.xml'));
    expect(bundle.logPath, contains('xts_tf_output.log'));
  });
}

Uint8List _encodeZip(Map<String, String> files) {
  final archive = Archive();
  for (final entry in files.entries) {
    final bytes = utf8.encode(entry.value);
    archive.addFile(ArchiveFile(entry.key, bytes.length, bytes));
  }
  return Uint8List.fromList(ZipEncoder().encode(archive));
}

const _sampleResultXml = '''<?xml version='1.0' encoding='UTF-8' standalone='no' ?>
<Result start="1767596648026" end="1767655280678" suite_name="GTS" suite_version="13.1_r2" devices="serial1,serial2">
  <Build build_version_incremental="403_1" build_device="IMTM8300_HU" build_fingerprint="TelekomTV/IMTM8300_HU/IMTM8300_HU:14/UTT2.250604.001/403_1:user/release-keys" build_version_release="14" build_type="user" />
  <Summary pass="2" failed="4" warning="0" />
  <Module name="GtsNetTestCases" abi="arm64-v8a">
    <TestCase name="android.net.wifi.cts.WifiLocationInfoBackgroundTest">
      <Test result="pass" name="testOne" />
      <Test result="fail" name="testTransportInfoRetrievalAllowedWithBackgroundLocationPermission">
        <Failure message="xml failure">
          <StackTrace>java.lang.AssertionError: boom</StackTrace>
        </Failure>
      </Test>
    </TestCase>
  </Module>
</Result>
''';

const _sampleTfOutputLog = '''
01-06 07:49:13 I/ModuleListener: [40/137] serial1 android.net.wifi.cts.WifiLocationInfoBackgroundTest#testTransportInfoRetrievalAllowedWithBackgroundLocationPermission FAILURE: expected to be true
\tat android.net.wifi.cts.WaitForResultActivity.waitForServiceResult(WaitForResultActivity.java:126)
\tat android.net.wifi.cts.WifiLocationInfoBackgroundTest.startBgServiceAndAssertStatusIs(WifiLocationInfoBackgroundTest.java:192)
Total Tests       : 10
PASSED            : 7
FAILED            : 1
IGNORED           : 1
ASSUMPTION_FAILURE: 1
''';

const _sampleLiveLog = '''
command_line_args: "run gts --shard-count 2 -s serial1 -s serial2"
xts_root_dir: "/home/innopia/xts/gts/android-gts-13.1-R2"
device_serial: "serial1"
device_serial: "serial2"
xts_suite_info { key: "suite_name" value: "GTS" }
xts_suite_info { key: "suite_version" value: "13.1_r2" }
total_execution_time { seconds: 49808 nanos: 123 }
state_summary: "running gts on build(s)"
''';
