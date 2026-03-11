import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/services/archive_import_service.dart';
import 'package:google_auth_helper/services/xts_live_log_parser.dart';
import 'package:google_auth_helper/services/xts_result_parser.dart';
import 'package:google_auth_helper/services/xts_tf_output_parser.dart';

void main() {
  test('ArchiveImportService parses sample upload zip with tf output summary',
      () async {
    final bytes =
        await File('test_sample/sample_upload_bundle.zip').readAsBytes();
    final service = ArchiveImportService(
      resultParser: XtsResultParser(),
      liveLogParser: XtsLiveLogParser(),
      tfOutputParser: XtsTfOutputParser(),
    );

    final bundle = await service.importZipBytes(
      fileName: 'sample_upload_bundle.zip',
      bytes: bytes,
    );

    expect(bundle.metric.totalTests, 117150);
    expect(bundle.metric.failCount, 33);
    expect(bundle.metric.buildDevice, 'IMTM8300_HU');
    expect(bundle.metric.androidVersion, '14');
    expect(bundle.metric.buildType, 'user');
    expect(bundle.metric.fwVersion, '403_1');
    expect(bundle.metric.countSource, 'xts_tf_output.log');
    expect(bundle.failedTests, isNotEmpty);
    expect(bundle.resultPath, contains('test_result.xml'));
    expect(bundle.logPath, contains('xts_tf_output.log'));
  });
}
