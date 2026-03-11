import 'connected_adb_device.dart';
import 'console_health.dart';
import 'live_status.dart';
import 'tool_config.dart';

class RunSessionState {
  const RunSessionState({
    required this.selectedTool,
    required this.stage,
    required this.command,
    required this.selectedDeviceSerials,
    required this.availableDevices,
    required this.consoleHealth,
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
    this.isRefreshingDevices = false,
  });

  final ToolType selectedTool;
  final RunStage stage;
  final String command;
  final List<String> selectedDeviceSerials;
  final List<ConnectedAdbDevice> availableDevices;
  final ConsoleHealth consoleHealth;
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
  final bool isRefreshingDevices;

  factory RunSessionState.initial(ToolConfig config) {
    return RunSessionState(
      selectedTool: config.toolType,
      stage: RunStage.idle,
      command: config.defaultCommand,
      selectedDeviceSerials: const [],
      availableDevices: const [],
      consoleHealth: ConsoleHealth.idle,
      autoUploadAfterRun: config.autoUploadAfterRun,
      latestLogs: const [],
    );
  }

  int get shardCount => selectedDeviceSerials.length;

  String get generatedCommand {
    final base = command.trim();
    if (base.isEmpty || selectedDeviceSerials.isEmpty) {
      return base;
    }

    final serialArgs =
        selectedDeviceSerials.map((serial) => '-s $serial').join(' ');
    return '$base --shard-count ${selectedDeviceSerials.length} $serialArgs';
  }

  RunSessionState copyWith({
    ToolType? selectedTool,
    RunStage? stage,
    String? command,
    List<String>? selectedDeviceSerials,
    List<ConnectedAdbDevice>? availableDevices,
    ConsoleHealth? consoleHealth,
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
    bool? isRefreshingDevices,
  }) {
    return RunSessionState(
      selectedTool: selectedTool ?? this.selectedTool,
      stage: stage ?? this.stage,
      command: command ?? this.command,
      selectedDeviceSerials: selectedDeviceSerials ?? this.selectedDeviceSerials,
      availableDevices: availableDevices ?? this.availableDevices,
      consoleHealth: consoleHealth ?? this.consoleHealth,
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
      isRefreshingDevices: isRefreshingDevices ?? this.isRefreshingDevices,
    );
  }
}
