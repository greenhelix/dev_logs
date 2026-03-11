import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as path;

import '../models/console_health.dart';
import '../models/live_status.dart';
import '../models/run_request.dart';
import '../models/tool_config.dart';
import 'xts_execution_service.dart';

class IoXtsExecutionService implements XtsExecutionService {
  final _logController = StreamController<String>.broadcast();
  final _consoleHealthController = StreamController<ConsoleHealth>.broadcast();
  Process? _process;

  @override
  bool get isSupported => Platform.isLinux;

  @override
  bool get isConsoleRunning => _process != null;

  @override
  Stream<String> get logLines => _logController.stream;

  @override
  Stream<ConsoleHealth> get consoleHealthUpdates =>
      _consoleHealthController.stream;

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
  Future<void> startConsole({
    required ToolConfig config,
    required RunRequest request,
  }) async {
    if (!isSupported) {
      throw UnsupportedError('자동 테스트 실행은 우분투에서만 지원합니다.');
    }
    if (_process != null) {
      throw StateError('이미 실행 중인 콘솔이 있습니다.');
    }

    final executable = path.join(
      config.toolRoot,
      'tools',
      request.toolType.tradefedExecutable,
    );

    _consoleHealthController.add(
      const ConsoleHealth(
        status: ConsoleHealthStatus.checking,
        message: 'tradefed 콘솔 프롬프트를 기다리는 중입니다.',
      ),
    );

    final process = await Process.start(
      executable,
      const [],
      workingDirectory: config.toolRoot,
      runInShell: false,
    );
    _process = process;
    _logController.add('콘솔 시작: $executable');

    final expectedPrompt = request.toolType.consolePrompt.toLowerCase();
    final promptCompleter = Completer<void>();
    Timer? timeoutTimer;
    var promptDetected = false;

    void failConsole(ConsoleHealth health) {
      if (!promptCompleter.isCompleted) {
        promptCompleter.completeError(StateError(health.message));
      }
      _consoleHealthController.add(health);
    }

    void onLine(String line) {
      _logController.add(line);
      final lower = line.toLowerCase();
      if (!promptDetected && lower.contains(expectedPrompt)) {
        promptDetected = true;
        timeoutTimer?.cancel();
        _consoleHealthController.add(
          ConsoleHealth(
            status: ConsoleHealthStatus.ok,
            message: '콘솔 프롬프트를 확인했습니다.',
            matchedPrompt: request.toolType.consolePrompt,
          ),
        );
        if (!promptCompleter.isCompleted) {
          promptCompleter.complete();
        }
        return;
      }

      if (!promptDetected &&
          (lower.contains('exception') ||
              lower.contains('fatal') ||
              lower.contains('no such file') ||
              lower.contains('permission denied'))) {
        failConsole(
          ConsoleHealth(
            status: ConsoleHealthStatus.failed,
            message: '프롬프트 확인 전에 콘솔 시작이 실패했습니다.',
          ),
        );
      }
    }

    _streamLines(process.stdout).listen(onLine);
    _streamLines(process.stderr).listen(onLine);

    unawaited(
      process.exitCode.then((code) {
        _logController.add('Process exit code: $code');
        if (!promptDetected) {
          failConsole(
            ConsoleHealth(
              status: ConsoleHealthStatus.failed,
              message: '콘솔 프롬프트가 나타나기 전에 프로세스가 종료되었습니다.',
            ),
          );
        }
        _process = null;
      }),
    );

    timeoutTimer = Timer(const Duration(seconds: 20), () {
      if (promptDetected) {
        return;
      }
      failConsole(
        const ConsoleHealth(
          status: ConsoleHealthStatus.needsAttention,
          message: '20초 안에 콘솔 프롬프트를 찾지 못했습니다.',
        ),
      );
    });

    try {
      await promptCompleter.future;
    } catch (_) {
      process.kill(ProcessSignal.sigterm);
      rethrow;
    } finally {
      timeoutTimer.cancel();
    }
  }

  @override
  Future<void> sendRunCommand(RunRequest request) async {
    final process = _process;
    if (process == null) {
      throw StateError('먼저 콘솔을 시작해야 합니다.');
    }
    process.stdin.writeln(request.command);
    _logController.add('실행 명령 전송: ${request.command}');
  }

  @override
  Future<void> startRun({
    required ToolConfig config,
    required RunRequest request,
  }) async {
    await startConsole(config: config, request: request);
    await sendRunCommand(request);
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

  Stream<String> _streamLines(Stream<List<int>> stream) {
    return stream
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .map((line) => line.trimRight())
        .where((line) => line.isNotEmpty);
  }
}

XtsExecutionService createExecutionService() => IoXtsExecutionService();
