enum AppLogArea {
  common,
  dashboard,
  results,
  updates,
  environment,
  run,
  settings,
}

enum AppLogLevel { info, warning, error }

class AppLogEntry {
  const AppLogEntry({
    required this.timestamp,
    required this.area,
    required this.level,
    required this.message,
    this.detail,
  });

  final DateTime timestamp;
  final AppLogArea area;
  final AppLogLevel level;
  final String message;
  final String? detail;
}
