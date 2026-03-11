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
  Future<void> startRun({
    required ToolConfig config,
    required RunRequest request,
  }) async {
    if (!isSupported) {
      throw UnsupportedError('Test execution is only supported on Linux.');
    }
    if (_process != null) {
      throw StateError('Another test process is already running.');
    }

    final executable = path.join(
      config.toolRoot,
      'tools',
      request.toolType.tradefedExecutable,
    );

    _consoleHealthController.add(
      const ConsoleHealth(
        status: ConsoleHealthStatus.checking,
        message: 'Waiting for tradefed console prompt.',
      ),
    );

    final process = await Process.start(
      executable,
      const [],
      workingDirectory: config.toolRoot,
      runInShell: false,
    );
    _process = process;
    _logController.add('Launching: $executable');

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
            message: 'Console prompt detected.',
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
            message: 'Console startup failed before prompt was detected.',
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
              message: 'Process exited before console prompt appeared.',
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
          message: 'Console prompt was not detected within 20 seconds.',
        ),
      );
    });

    try {
      await promptCompleter.future;
      process.stdin.writeln(request.command);
      _logController.add('Command sent: ${request.command}');
    } catch (_) {
      process.kill(ProcessSignal.sigterm);
      rethrow;
    } finally {
      timeoutTimer.cancel();
    }
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
