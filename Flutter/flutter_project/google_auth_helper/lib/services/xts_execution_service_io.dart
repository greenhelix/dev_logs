import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as path;

import '../models/live_status.dart';
import '../models/run_request.dart';
import '../models/tool_config.dart';
import 'xts_execution_service.dart';

class IoXtsExecutionService implements XtsExecutionService {
  final _controller = StreamController<String>.broadcast();
  Process? _process;

  @override
  bool get isSupported => Platform.isLinux;

  @override
  Stream<String> get logLines => _controller.stream;

  @override
  RunStage deriveStage(String fullLogText) {
    final lower = fullLogText.toLowerCase();
    if (lower.contains('error') || lower.contains('exception')) {
      return RunStage.error;
    }
    if (lower.contains('invocation finished') ||
        lower.contains('result directory') ||
        lower.contains('end of results')) {
      return RunStage.finished;
    }
    if (lower.contains('running') || lower.contains('starting invocation')) {
      return RunStage.running;
    }
    if (lower.contains('shard')) {
      return RunStage.sharding;
    }
    return RunStage.starting;
  }

  @override
  Future<void> startRun({
    required ToolConfig config,
    required RunRequest request,
  }) async {
    if (!isSupported) {
      throw UnsupportedError('테스트 실행은 Ubuntu/Linux에서만 지원합니다.');
    }
    if (_process != null) {
      throw StateError('이미 실행 중인 테스트가 있습니다.');
    }

    final executable = path.join(
      config.toolRoot,
      'tools',
      request.toolType.tradefedExecutable,
    );
    final args = _tokenize(request.command);
    _process = await Process.start(
      executable,
      args,
      workingDirectory: config.toolRoot,
      runInShell: false,
    );

    _controller.add('명령 실행: $executable ${request.command}');
    unawaited(_pipe(_process!.stdout));
    unawaited(_pipe(_process!.stderr));
    unawaited(
      _process!.exitCode.then((code) {
        _controller.add('프로세스 종료 코드: $code');
        _process = null;
      }),
    );
  }

  @override
  Future<int?> stopRun() async {
    final process = _process;
    if (process == null) {
      return null;
    }
    process.kill(ProcessSignal.sigterm);
    final exitCode = await process.exitCode;
    _process = null;
    return exitCode;
  }

  @override
  Future<int?> waitForExit() async {
    final process = _process;
    if (process == null) {
      return null;
    }
    return process.exitCode;
  }

  Future<void> _pipe(Stream<List<int>> stream) async {
    await for (final chunk in stream.transform(utf8.decoder)) {
      final lines = chunk.replaceAll('\r', '').split('\n');
      for (final line in lines) {
        final trimmed = line.trimRight();
        if (trimmed.isEmpty) {
          continue;
        }
        _controller.add(trimmed);
      }
    }
  }

  List<String> _tokenize(String command) {
    return command
        .split(RegExp(r'\s+'))
        .map((item) => item.trim())
        .where((item) => item.isNotEmpty)
        .toList(growable: false);
  }
}

XtsExecutionService createExecutionService() => IoXtsExecutionService();
