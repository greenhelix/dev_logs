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
  RunStage deriveStage(String fullLogText) => RunStage.idle;

  @override
  Future<void> startRun({
    required ToolConfig config,
    required RunRequest request,
  }) async {
    throw UnsupportedError('현재 플랫폼에서는 테스트 실행을 지원하지 않습니다.');
  }

  @override
  Future<int?> stopRun() async => null;

  @override
  Future<int?> waitForExit() async => null;
}

XtsExecutionService createExecutionService() =>
    UnsupportedXtsExecutionService();
