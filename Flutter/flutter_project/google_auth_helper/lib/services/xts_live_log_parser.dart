import '../models/live_status.dart';

class XtsLiveLogParser {
  LiveStatus parseText(String source) {
    final normalized = source.replaceAll('\r', '');
    final commandLine = _extractFirst(
      normalized,
      RegExp(r'command_line_args:\s*"([^"]+)"'),
    );
    final xtsRootDir = _extractFirst(
      normalized,
      RegExp(r'xts_root_dir:\s*"([^"]+)"'),
    );
    final suiteName = _extractFirst(
      normalized,
      RegExp(r'xts_suite_info \{ key: "suite_name" value: "([^"]+)" \}'),
    );
    final suiteVersion = _extractFirst(
      normalized,
      RegExp(r'xts_suite_info \{ key: "suite_version" value: "([^"]+)" \}'),
    );
    final shardCount = int.tryParse(
          _extractFirst(normalized, RegExp(r'shard_count:\s*(\d+)')),
        ) ??
        int.tryParse(
          _extractFirst(normalized, RegExp(r'--shard-count\s+(\d+)')),
        ) ??
        0;
    final devices = RegExp(r'device_serial:\s*"([^"]+)"')
        .allMatches(normalized)
        .map((match) => match.group(1)!)
        .toSet()
        .toList(growable: false);
    final totalExecutionSeconds = int.tryParse(
      _extractFirst(
        normalized,
        RegExp(r'total_execution_time \{ seconds: (\d+)'),
      ),
    );
    final stateSummary = _extractLast(
      normalized,
      RegExp(r'state_summary:\s*"([^"]+)"'),
    );

    return LiveStatus(
      stage: _detectStage(normalized, stateSummary),
      commandLine: commandLine,
      xtsRootDir: xtsRootDir,
      suiteName: suiteName,
      suiteVersion: suiteVersion,
      shardCount: shardCount,
      devices: devices,
      stateSummary: stateSummary,
      totalExecutionSeconds: totalExecutionSeconds,
    );
  }

  RunStage _detectStage(String source, String stateSummary) {
    final lower = '${source.toLowerCase()}\n${stateSummary.toLowerCase()}';
    if (lower.contains('error') || lower.contains('exception')) {
      return RunStage.error;
    }
    if (lower.contains('result directory') ||
        lower.contains('total_execution_time') ||
        lower.contains('end of results')) {
      return RunStage.finished;
    }
    if (lower.contains('running cts on build') ||
        lower.contains('running module') ||
        lower.contains('starting invocation')) {
      return RunStage.running;
    }
    if (lower.contains('sharding')) {
      return RunStage.sharding;
    }
    if (lower.contains('waiting for devices') ||
        lower.contains('sessionstartingevent') ||
        lower.contains('command [1] started')) {
      return RunStage.starting;
    }
    if (lower.contains('scheduled')) {
      return RunStage.queued;
    }
    return RunStage.idle;
  }

  String _extractFirst(String source, RegExp pattern) {
    final match = pattern.firstMatch(source);
    return match?.group(1) ?? '';
  }

  String _extractLast(String source, RegExp pattern) {
    final matches = pattern.allMatches(source).toList(growable: false);
    if (matches.isEmpty) {
      return '';
    }
    return matches.last.group(1) ?? '';
  }
}
