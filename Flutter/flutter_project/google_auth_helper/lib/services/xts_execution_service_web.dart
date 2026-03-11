import '../models/console_health.dart';
import '../models/live_status.dart';
import '../models/run_request.dart';
import '../models/tool_config.dart';
import 'xts_execution_service.dart';

class UnsupportedXtsExecutionService implements XtsExecutionService {
  @override
  bool get isSupported => false;

  @override
  Stream<String> get logLines => const Stream.empty();

  @override
  Stream<ConsoleHealth> get consoleHealthUpdates =>
      Stream.value(ConsoleHealth.idle);

  @override
  RunStage deriveStage(String fullLogText) => RunStage.idle;

  @override
  Future<void> startRun({
    required ToolConfig config,
    required RunRequest request,
  }) async {
    throw UnsupportedError('Test execution is not available on this platform.');
  }

  @override
  Future<int?> stopRun() async => null;

  @override
  Future<int?> waitForExit() async => null;
}

XtsExecutionService createExecutionService() =>
    UnsupportedXtsExecutionService();
