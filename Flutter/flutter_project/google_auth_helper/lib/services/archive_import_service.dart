import 'dart:typed_data';

import 'package:archive/archive.dart';

import '../models/import_bundle.dart';
import '../models/import_source.dart';
import '../models/live_status.dart';
import 'local_file_gateway.dart';
import 'xts_live_log_parser.dart';
import 'xts_result_parser.dart';
import 'xts_tf_output_parser.dart';

class ArchiveImportService {
  ArchiveImportService({
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

  Future<ImportBundle> importZipBytes({
    required String fileName,
    required Uint8List bytes,
  }) async {
    return importFromSources(
      resultSource: ArchiveImportSource(fileName: fileName, bytes: bytes),
      logSource: ArchiveImportSource(fileName: fileName, bytes: bytes),
    );
  }

  Future<ImportBundle> importSplitZipBytes({
    required String resultFileName,
    required Uint8List resultBytes,
    required String logFileName,
    required Uint8List logBytes,
  }) async {
    return importFromSources(
      resultSource:
          ArchiveImportSource(fileName: resultFileName, bytes: resultBytes),
      logSource: ArchiveImportSource(fileName: logFileName, bytes: logBytes),
    );
  }

  Future<ImportBundle> importFromSources({
    required ImportSource resultSource,
    required ImportSource logSource,
  }) async {
    final resultReader = _createReader(resultSource);
    final logReader = _createReader(logSource);

    final resultFile = await resultReader.findLatest('test_result.xml');
    if (resultFile == null) {
      throw StateError('결과 원본에서 test_result.xml 파일을 찾지 못했습니다.');
    }

    final tfOutputLog = await logReader.findLatest('xts_tf_output.log');
    final liveStatusLog =
        await logReader.findLatest('olc_server_session_log.txt') ??
            tfOutputLog ??
            await logReader.findLatest('command_history.txt');
    if (tfOutputLog == null && liveStatusLog == null) {
      throw StateError(
        '로그 원본에서 xts_tf_output.log 또는 지원 로그 파일을 찾지 못했습니다.',
      );
    }

    final liveStatus = liveStatusLog == null
        ? LiveStatus.empty
        : _liveLogParser.parseText(await liveStatusLog.readAsString());
    final tfOutput = tfOutputLog == null
        ? null
        : _tfOutputParser.parseText(await tfOutputLog.readAsString());
    final parsed = _resultParser.parseText(
      await resultFile.readAsString(),
      liveStatus: liveStatus,
      tfOutput: tfOutput,
    );

    return ImportBundle(
      metric: parsed.metric,
      testCases: parsed.testCases,
      failedTests: parsed.failedTests,
      liveStatus: liveStatus,
      resultPath: resultFile.displayPath,
      logPath: tfOutputLog?.displayPath ?? liveStatusLog?.displayPath,
      previewWarnings: parsed.warnings,
    );
  }

  _ImportSourceReader _createReader(ImportSource source) {
    switch (source.kind) {
      case ImportSourceKind.archive:
        return _ArchiveSourceReader(
          source as ArchiveImportSource,
        );
      case ImportSourceKind.directory:
        return _DirectorySourceReader(
          source as DirectoryImportSource,
          _localFileGateway,
        );
    }
  }
}

abstract class _ImportSourceReader {
  Future<_ImportedTextFile?> findLatest(String fileName);
}

class _ArchiveSourceReader implements _ImportSourceReader {
  _ArchiveSourceReader(this._source)
      : _archive = ZipDecoder().decodeBytes(_source.bytes);

  final ArchiveImportSource _source;
  final Archive _archive;

  @override
  Future<_ImportedTextFile?> findLatest(String fileName) async {
    final matches = _archive.files.where((file) {
      return file.isFile && file.name.endsWith(fileName);
    }).toList(growable: false);
    if (matches.isEmpty) {
      return null;
    }
    final selected = matches.last;
    return _ImportedTextFile(
      displayPath: '${_source.fileName}::${selected.name}',
      readAsString: () async => String.fromCharCodes(selected.content as List<int>),
    );
  }
}

class _DirectorySourceReader implements _ImportSourceReader {
  const _DirectorySourceReader(this._source, this._gateway);

  final DirectoryImportSource _source;
  final LocalFileGateway _gateway;

  @override
  Future<_ImportedTextFile?> findLatest(String fileName) async {
    final foundPath =
        await _gateway.findLatestFile(_source.directoryPath, fileName);
    if (foundPath == null) {
      return null;
    }
    return _ImportedTextFile(
      displayPath: foundPath,
      readAsString: () => _gateway.readAsString(foundPath),
    );
  }
}

class _ImportedTextFile {
  const _ImportedTextFile({
    required this.displayPath,
    required Future<String> Function() readAsString,
  }) : _readAsString = readAsString;

  final String displayPath;
  final Future<String> Function() _readAsString;

  Future<String> readAsString() => _readAsString();
}
