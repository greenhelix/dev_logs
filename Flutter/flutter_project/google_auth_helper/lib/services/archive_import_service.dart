import 'dart:typed_data';

import 'package:archive/archive.dart';

import '../models/import_bundle.dart';
import '../models/live_status.dart';
import 'xts_live_log_parser.dart';
import 'xts_result_parser.dart';
import 'xts_tf_output_parser.dart';

class ArchiveImportService {
  ArchiveImportService({
    required XtsResultParser resultParser,
    required XtsLiveLogParser liveLogParser,
    required XtsTfOutputParser tfOutputParser,
  })  : _resultParser = resultParser,
        _liveLogParser = liveLogParser,
        _tfOutputParser = tfOutputParser;

  final XtsResultParser _resultParser;
  final XtsLiveLogParser _liveLogParser;
  final XtsTfOutputParser _tfOutputParser;

  Future<ImportBundle> importZipBytes({
    required String fileName,
    required Uint8List bytes,
  }) async {
    final archive = ZipDecoder().decodeBytes(bytes);
    final resultFiles = archive.files.where((file) {
      return file.isFile && file.name.endsWith('test_result.xml');
    }).toList(growable: false);
    if (resultFiles.isEmpty) {
      throw StateError('zip 안에서 test_result.xml을 찾을 수 없습니다.');
    }

    final tfOutputLog = _findLatestLog(archive, 'xts_tf_output.log');
    final liveStatusLog =
        _findLatestLog(archive, 'olc_server_session_log.txt') ??
            tfOutputLog ??
            _findLatestLog(archive, 'command_history.txt');
    final resultFile = resultFiles.last;
    final resultText = _readText(resultFile.content);
    final liveStatus = liveStatusLog == null
        ? LiveStatus.empty
        : _liveLogParser.parseText(_readText(liveStatusLog.content));
    final tfOutput = tfOutputLog == null
        ? null
        : _tfOutputParser.parseText(_readText(tfOutputLog.content));
    final parsed = _resultParser.parseText(
      resultText,
      liveStatus: liveStatus,
      tfOutput: tfOutput,
    );

    return ImportBundle(
      metric: parsed.metric,
      testCases: parsed.testCases,
      failedTests: parsed.failedTests,
      liveStatus: liveStatus,
      resultPath: '$fileName::${resultFile.name}',
      logPath: tfOutputLog == null ? null : '$fileName::${tfOutputLog.name}',
      previewWarnings: parsed.warnings,
    );
  }

  ArchiveFile? _findLatestLog(Archive archive, String fileName) {
    final matches = archive.files.where((file) {
      return file.isFile && file.name.endsWith(fileName);
    }).toList(growable: false);
    if (matches.isEmpty) {
      return null;
    }
    return matches.last;
  }

  String _readText(List<int> content) {
    return String.fromCharCodes(content);
  }
}
