import 'live_status.dart';
import 'tool_config.dart';

class RunSessionState {
  const RunSessionState({
    required this.selectedTool,
    required this.stage,
    required this.command,
    required this.deviceSerials,
    required this.shardCount,
    required this.autoUploadAfterRun,
    required this.latestLogs,
    this.startedAt,
    this.finishedAt,
    this.detectedResultsDir,
    this.detectedLogsDir,
    this.message,
    this.exitCode,
    this.isRunning = false,
    this.isUploading = false,
  });

  final ToolType selectedTool;
  final RunStage stage;
  final String command;
  final List<String> deviceSerials;
  final int shardCount;
  final bool autoUploadAfterRun;
  final List<String> latestLogs;
  final DateTime? startedAt;
  final DateTime? finishedAt;
  final String? detectedResultsDir;
  final String? detectedLogsDir;
  final String? message;
  final int? exitCode;
  final bool isRunning;
  final bool isUploading;

  factory RunSessionState.initial(ToolConfig config) {
    return RunSessionState(
      selectedTool: config.toolType,
      stage: RunStage.idle,
      command: config.defaultCommand,
      deviceSerials: config.deviceSerials,
      shardCount: config.shardCount,
      autoUploadAfterRun: config.autoUploadAfterRun,
      latestLogs: const [],
    );
  }

  RunSessionState copyWith({
    ToolType? selectedTool,
    RunStage? stage,
    String? command,
    List<String>? deviceSerials,
    int? shardCount,
    bool? autoUploadAfterRun,
    List<String>? latestLogs,
    DateTime? startedAt,
    DateTime? finishedAt,
    String? detectedResultsDir,
    String? detectedLogsDir,
    String? message,
    int? exitCode,
    bool? isRunning,
    bool? isUploading,
  }) {
    return RunSessionState(
      selectedTool: selectedTool ?? this.selectedTool,
      stage: stage ?? this.stage,
      command: command ?? this.command,
      deviceSerials: deviceSerials ?? this.deviceSerials,
      shardCount: shardCount ?? this.shardCount,
      autoUploadAfterRun: autoUploadAfterRun ?? this.autoUploadAfterRun,
      latestLogs: latestLogs ?? this.latestLogs,
      startedAt: startedAt ?? this.startedAt,
      finishedAt: finishedAt ?? this.finishedAt,
      detectedResultsDir: detectedResultsDir ?? this.detectedResultsDir,
      detectedLogsDir: detectedLogsDir ?? this.detectedLogsDir,
      message: message,
      exitCode: exitCode ?? this.exitCode,
      isRunning: isRunning ?? this.isRunning,
      isUploading: isUploading ?? this.isUploading,
    );
  }
}
