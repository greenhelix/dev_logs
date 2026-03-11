import '../models/console_health.dart';
import '../models/live_status.dart';
import '../models/run_request.dart';
import '../models/tool_config.dart';
import 'xts_execution_service_web.dart'
    if (dart.library.io) 'xts_execution_service_io.dart';

abstract class XtsExecutionService {
  bool get isSupported;
  bool get isConsoleRunning;

  Stream<String> get logLines;

  Stream<ConsoleHealth> get consoleHealthUpdates;

  Future<void> startConsole({
    required ToolConfig config,
    required RunRequest request,
  });

  Future<void> sendRunCommand(RunRequest request);

  Future<void> startRun({
    required ToolConfig config,
    required RunRequest request,
  });

  Future<int?> stopRun();

  Future<int?> waitForExit();

  RunStage deriveStage(String fullLogText);
}

XtsExecutionService createXtsExecutionService() => createExecutionService();
