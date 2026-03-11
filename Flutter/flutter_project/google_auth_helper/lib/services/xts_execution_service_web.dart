import '../models/console_health.dart';
import '../models/live_status.dart';
import '../models/run_request.dart';
import '../models/tool_config.dart';
import 'xts_execution_service.dart';

class UnsupportedXtsExecutionService implements XtsExecutionService {
  @override
  bool get isSupported => false;

  @override
  bool get isConsoleRunning => false;

  @override
  Stream<String> get logLines => const Stream.empty();

  @override
  Stream<ConsoleHealth> get consoleHealthUpdates =>
      Stream.value(ConsoleHealth.idle);

  @override
  RunStage deriveStage(String fullLogText) => RunStage.idle;

  @override
  Future<void> startConsole({
    required ToolConfig config,
    required RunRequest request,
  }) async {
    throw UnsupportedError('이 플랫폼에서는 자동 테스트를 실행할 수 없습니다.');
  }

  @override
  Future<void> sendRunCommand(RunRequest request) async {
    throw UnsupportedError('이 플랫폼에서는 자동 테스트를 실행할 수 없습니다.');
  }

  @override
  Future<void> startRun({
    required ToolConfig config,
    required RunRequest request,
  }) async {
    throw UnsupportedError('이 플랫폼에서는 자동 테스트를 실행할 수 없습니다.');
  }

  @override
  Future<int?> stopRun() async => null;

  @override
  Future<int?> waitForExit() async => null;
}

XtsExecutionService createExecutionService() =>
    UnsupportedXtsExecutionService();
