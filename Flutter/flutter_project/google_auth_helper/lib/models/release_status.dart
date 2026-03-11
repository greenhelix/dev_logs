import 'release_asset_info.dart';

class ReleaseStatus {
  const ReleaseStatus({
    required this.currentVersion,
    required this.latestVersion,
    required this.releaseUrl,
    this.installerAsset,
  });

  final String currentVersion;
  final String latestVersion;
  final String releaseUrl;
  final ReleaseAssetInfo? installerAsset;

  bool get hasUpdate =>
      latestVersion.isNotEmpty &&
      _normalizeVersion(latestVersion) != _normalizeVersion(currentVersion);

  static String _normalizeVersion(String raw) {
    final trimmed = raw.trim().toLowerCase();
    if (trimmed.isEmpty) {
      return '';
    }
    final withoutV = trimmed.startsWith('v') ? trimmed.substring(1) : trimmed;
    final plusIndex = withoutV.indexOf('+');
    return plusIndex >= 0 ? withoutV.substring(0, plusIndex) : withoutV;
  }
}
