import '../models/live_status.dart';
import '../models/run_request.dart';
import '../models/tool_config.dart';
import 'xts_execution_service_web.dart'
    if (dart.library.io) 'xts_execution_service_io.dart';

abstract class XtsExecutionService {
  bool get isSupported;

  Stream<String> get logLines;

  Future<void> startRun({
    required ToolConfig config,
    required RunRequest request,
  });

  Future<int?> stopRun();

  Future<int?> waitForExit();

  RunStage deriveStage(String fullLogText);
}

XtsExecutionService createXtsExecutionService() => createExecutionService();
