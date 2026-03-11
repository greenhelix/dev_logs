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
    return importSplitZipBytes(
      resultFileName: fileName,
      resultBytes: bytes,
      logFileName: fileName,
      logBytes: bytes,
    );
  }

  Future<ImportBundle> importSplitZipBytes({
    required String resultFileName,
    required Uint8List resultBytes,
    required String logFileName,
    required Uint8List logBytes,
  }) async {
    final resultArchive = ZipDecoder().decodeBytes(resultBytes);
    final logArchive = ZipDecoder().decodeBytes(logBytes);
    final resultFile = _findLatestFile(resultArchive, 'test_result.xml');
    if (resultFile == null) {
      throw StateError(
        'Result zip does not contain test_result.xml.',
      );
    }

    final tfOutputLog = _findLatestFile(logArchive, 'xts_tf_output.log');
    final liveStatusLog =
        _findLatestFile(logArchive, 'olc_server_session_log.txt') ??
            tfOutputLog ??
            _findLatestFile(logArchive, 'command_history.txt');
    if (tfOutputLog == null && liveStatusLog == null) {
      throw StateError(
        'Log zip does not contain xts_tf_output.log or supported fallback logs.',
      );
    }

    final liveStatus = liveStatusLog == null
        ? LiveStatus.empty
        : _liveLogParser.parseText(_readText(liveStatusLog.content));
    final tfOutput = tfOutputLog == null
        ? null
        : _tfOutputParser.parseText(_readText(tfOutputLog.content));
    final parsed = _resultParser.parseText(
      _readText(resultFile.content),
      liveStatus: liveStatus,
      tfOutput: tfOutput,
    );

    return ImportBundle(
      metric: parsed.metric,
      testCases: parsed.testCases,
      failedTests: parsed.failedTests,
      liveStatus: liveStatus,
      resultPath: '$resultFileName::${resultFile.name}',
      logPath: tfOutputLog == null ? '$logFileName::${liveStatusLog!.name}' : '$logFileName::${tfOutputLog.name}',
      previewWarnings: parsed.warnings,
    );
  }

  ArchiveFile? _findLatestFile(Archive archive, String fileName) {
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
