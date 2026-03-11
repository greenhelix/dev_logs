class ReleaseStatus {
  const ReleaseStatus({
    required this.currentVersion,
    required this.latestVersion,
    required this.releaseUrl,
  });

  final String currentVersion;
  final String latestVersion;
  final String releaseUrl;

  bool get hasUpdate =>
      latestVersion.isNotEmpty && latestVersion != currentVersion;
}
