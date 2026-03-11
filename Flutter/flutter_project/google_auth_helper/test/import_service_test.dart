import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/services/import_service.dart';
import 'package:google_auth_helper/services/local_file_gateway_io.dart';
import 'package:google_auth_helper/services/xts_live_log_parser.dart';
import 'package:google_auth_helper/services/xts_result_parser.dart';
import 'package:google_auth_helper/services/xts_tf_output_parser.dart';

import 'test_sample_locator.dart';

void main() {
  test('ImportService prefers xts_tf_output.log from local path imports',
      () async {
    final service = ImportService(
      localFileGateway: IoLocalFileGateway(),
      resultParser: XtsResultParser(),
      liveLogParser: XtsLiveLogParser(),
      tfOutputParser: XtsTfOutputParser(),
    );

    final bundle = await service.importFromPaths(
      resultsDir: 'test_sample/results',
      logsDir: 'test_sample/logs',
    );

    expect(bundle.metric.countSource, 'xts_tf_output.log');
    expect(bundle.metric.totalTests, greaterThan(0));
    expect(bundle.metric.failCount, greaterThanOrEqualTo(0));
    expect(bundle.metric.fwVersion.trim(), isNotEmpty);
    expect(bundle.metric.buildDevice.trim(), isNotEmpty);
    expect(bundle.resultPath, contains('test_result.xml'));
    expect(bundle.logPath, contains('xts_tf_output.log'));
  }, skip: hasSampleImportDirectories() ? false : 'test_sample import directories are missing.');
}
