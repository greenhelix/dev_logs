import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/services/archive_import_service.dart';
import 'package:google_auth_helper/services/xts_live_log_parser.dart';
import 'package:google_auth_helper/services/xts_result_parser.dart';
import 'package:google_auth_helper/services/xts_tf_output_parser.dart';

import 'test_sample_locator.dart';

void main() {
  test('ArchiveImportService parses sample upload zip with tf output summary',
      () async {
    final sampleZipPath = findSampleUploadBundle()!;
    final bytes = await File(sampleZipPath).readAsBytes();
    final service = ArchiveImportService(
      resultParser: XtsResultParser(),
      liveLogParser: XtsLiveLogParser(),
      tfOutputParser: XtsTfOutputParser(),
    );

    final bundle = await service.importZipBytes(
      fileName: sampleZipPath.split(Platform.pathSeparator).last,
      bytes: bytes,
    );

    expect(bundle.metric.totalTests, greaterThan(0));
    expect(bundle.metric.failCount, greaterThanOrEqualTo(0));
    expect(bundle.metric.buildDevice.trim(), isNotEmpty);
    expect(bundle.metric.androidVersion.trim(), isNotEmpty);
    expect(bundle.metric.buildType.trim(), isNotEmpty);
    expect(bundle.metric.fwVersion.trim(), isNotEmpty);
    expect(bundle.metric.countSource, 'xts_tf_output.log');
    expect(bundle.resultPath, contains('test_result.xml'));
    expect(bundle.logPath, contains('xts_tf_output.log'));
  }, skip: findSampleUploadBundle() == null ? 'No sample upload bundle was found under test_sample.' : false);
}
