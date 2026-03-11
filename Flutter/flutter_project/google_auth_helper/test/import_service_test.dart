import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/services/import_service.dart';
import 'package:google_auth_helper/services/local_file_gateway_io.dart';
import 'package:google_auth_helper/services/xts_live_log_parser.dart';
import 'package:google_auth_helper/services/xts_result_parser.dart';
import 'package:google_auth_helper/services/xts_tf_output_parser.dart';

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
      resultsDir: 'test_sample/results/2026.01.05_16.03.34.081_7223',
      logsDir: 'test_sample/logs/2026.01.05_16.03.34.081_7223',
    );

    expect(bundle.metric.countSource, 'xts_tf_output.log');
    expect(bundle.metric.totalTests, 40708);
    expect(bundle.metric.failCount, 122);
    expect(bundle.metric.fwVersion, '403_1');
    expect(bundle.metric.buildDevice, 'IMTM8300_HU');
    expect(bundle.failedTests, isNotEmpty);
  });
}
