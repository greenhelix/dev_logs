import '../models/import_bundle.dart';
import '../models/live_status.dart';
import 'local_file_gateway.dart';
import 'xts_live_log_parser.dart';
import 'xts_result_parser.dart';
import 'xts_tf_output_parser.dart';

class ImportService {
  ImportService({
    required LocalFileGateway localFileGateway,
    required XtsResultParser resultParser,
    required XtsLiveLogParser liveLogParser,
    required XtsTfOutputParser tfOutputParser,
  })  : _localFileGateway = localFileGateway,
        _resultParser = resultParser,
        _liveLogParser = liveLogParser,
        _tfOutputParser = tfOutputParser;

  final LocalFileGateway _localFileGateway;
  final XtsResultParser _resultParser;
  final XtsLiveLogParser _liveLogParser;
  final XtsTfOutputParser _tfOutputParser;

  bool get supportsLocalImport => _localFileGateway.supportsLocalFiles;

  Future<ImportBundle> importFromPaths({
    required String resultsDir,
    required String logsDir,
    bool preferLatest = true,
  }) async {
    if (!_localFileGateway.supportsLocalFiles) {
      throw UnsupportedError('웹에서는 로컬 경로를 직접 읽을 수 없습니다.');
    }

    final resultPath = preferLatest
        ? await _localFileGateway.findLatestFile(resultsDir, 'test_result.xml')
        : await _localFileGateway.findFirstFile(resultsDir, 'test_result.xml');
    if (resultPath == null) {
      throw StateError('$resultsDir 에서 test_result.xml을 찾을 수 없습니다.');
    }

    final tfOutputPath = await _localFileGateway.findLatestFile(
      logsDir,
      'xts_tf_output.log',
    );
    final liveStatusPath = await _localFileGateway.findLatestFile(
            logsDir, 'olc_server_session_log.txt') ??
        tfOutputPath ??
        await _localFileGateway.findLatestFile(logsDir, 'command_history.txt');
    final liveStatus = liveStatusPath == null
        ? LiveStatus.empty
        : _liveLogParser
            .parseText(await _localFileGateway.readAsString(liveStatusPath));
    final tfOutput = tfOutputPath == null
        ? null
        : _tfOutputParser.parseText(
            await _localFileGateway.readAsString(tfOutputPath),
          );
    final parsed = _resultParser.parseText(
      await _localFileGateway.readAsString(resultPath),
      liveStatus: liveStatus,
      tfOutput: tfOutput,
    );

    return ImportBundle(
      metric: parsed.metric,
      testCases: parsed.testCases,
      failedTests: parsed.failedTests,
      liveStatus: liveStatus,
      resultPath: resultPath,
      logPath: tfOutputPath,
      previewWarnings: parsed.warnings,
    );
  }
}
