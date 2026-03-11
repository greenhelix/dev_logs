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
    required this.redmineConnection,
    required this.redmineCurrentUser,
    required this.redmineProjectAccess,
  });

  final EnvironmentProbeResult hosting;
  final EnvironmentProbeResult firestoreDownload;
  final EnvironmentProbeResult firestoreUpload;
  final EnvironmentProbeResult redmineConnection;
  final EnvironmentProbeResult redmineCurrentUser;
  final EnvironmentProbeResult redmineProjectAccess;

  List<EnvironmentProbeResult> get allResults {
    return [
      hosting,
      firestoreDownload,
      firestoreUpload,
      redmineConnection,
      redmineCurrentUser,
      redmineProjectAccess,
    ];
  }

  List<EnvironmentProbeResult> get redmineResults {
    return [
      redmineConnection,
      redmineCurrentUser,
      redmineProjectAccess,
    ];
  }
}
