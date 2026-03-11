class EnvironmentProbeResult {
  const EnvironmentProbeResult({
    required this.label,
    required this.isOk,
    required this.message,
  });

  final String label;
  final bool isOk;
  final String message;
}

class EnvironmentCheckStatus {
  const EnvironmentCheckStatus({
    required this.hosting,
    required this.firestoreDownload,
    required this.firestoreUpload,
    required this.adb,
    required this.redmineConnection,
    required this.redmineCurrentUser,
    required this.redmineProjectAccess,
  });

  final EnvironmentProbeResult hosting;
  final EnvironmentProbeResult firestoreDownload;
  final EnvironmentProbeResult firestoreUpload;
  final EnvironmentProbeResult adb;
  final EnvironmentProbeResult redmineConnection;
  final EnvironmentProbeResult redmineCurrentUser;
  final EnvironmentProbeResult redmineProjectAccess;

  List<EnvironmentProbeResult> get firebaseResults {
    return [
      hosting,
      firestoreDownload,
      firestoreUpload,
    ];
  }

  List<EnvironmentProbeResult> get localResults {
    return [adb];
  }

  List<EnvironmentProbeResult> get redmineResults {
    return [
      redmineConnection,
      redmineCurrentUser,
      redmineProjectAccess,
    ];
  }
}
