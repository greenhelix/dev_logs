import 'tool_config.dart';

class RunRequest {
  const RunRequest({
    required this.toolType,
    required this.command,
    required this.deviceSerials,
    required this.shardCount,
  });

  final ToolType toolType;
  final String command;
  final List<String> deviceSerials;
  final int shardCount;
}
