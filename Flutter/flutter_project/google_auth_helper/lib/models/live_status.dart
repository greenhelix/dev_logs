enum RunStage { idle, queued, starting, sharding, running, finished, error }

class LiveStatus {
  const LiveStatus({
    required this.stage,
    required this.commandLine,
    required this.xtsRootDir,
    required this.suiteName,
    required this.suiteVersion,
    required this.shardCount,
    required this.devices,
    required this.stateSummary,
    this.totalExecutionSeconds,
  });

  final RunStage stage;
  final String commandLine;
  final String xtsRootDir;
  final String suiteName;
  final String suiteVersion;
  final int shardCount;
  final List<String> devices;
  final String stateSummary;
  final int? totalExecutionSeconds;

  static const empty = LiveStatus(
    stage: RunStage.idle,
    commandLine: '',
    xtsRootDir: '',
    suiteName: '',
    suiteVersion: '',
    shardCount: 0,
    devices: <String>[],
    stateSummary: '',
  );
}
