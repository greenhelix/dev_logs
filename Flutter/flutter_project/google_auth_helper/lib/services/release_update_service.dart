import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../core/config/app_defaults.dart';
import '../models/release_asset_info.dart';
import '../models/release_status.dart';

class ReleaseUpdateService {
  ReleaseUpdateService({http.Client? httpClient})
      : _httpClient = httpClient ?? http.Client();

  final http.Client _httpClient;

  Future<ReleaseStatus> fetchLatestRelease() async {
    final response = await _httpClient.get(
      Uri.parse(
        'https://api.github.com/repos/${AppDefaults.releaseRepo}/releases/latest',
      ),
      headers: const {'Accept': 'application/vnd.github+json'},
    );

    if (response.statusCode == 404) {
      return const ReleaseStatus(
        currentVersion: AppDefaults.appVersion,
        latestVersion: '',
        releaseUrl: 'https://github.com/greenhelix/GAH-Release-Repo/releases',
        installerAsset: null,
      );
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw StateError(
        'Failed to fetch release status: ${response.statusCode}',
      );
    }

    final payload = jsonDecode(response.body) as Map<String, dynamic>;
    final asset = _selectInstallerAsset(payload['assets'] as List<dynamic>?);
    return ReleaseStatus(
      currentVersion: AppDefaults.appVersion,
      latestVersion: payload['tag_name'] as String? ?? '',
      releaseUrl: payload['html_url'] as String? ??
          'https://github.com/greenhelix/GAH-Release-Repo/releases',
      installerAsset: asset,
    );
  }

  ReleaseAssetInfo? _selectInstallerAsset(List<dynamic>? assets) {
    if (assets == null || kIsWeb) {
      return null;
    }

    String expectedSuffix;
    switch (defaultTargetPlatform) {
      case TargetPlatform.windows:
        expectedSuffix = '.exe';
        break;
      case TargetPlatform.linux:
        expectedSuffix = '.deb';
        break;
      default:
        return null;
    }

    for (final raw in assets) {
      final asset = raw as Map<String, dynamic>;
      final name = asset['name'] as String? ?? '';
      if (!name.toLowerCase().endsWith(expectedSuffix)) {
        continue;
      }
      final downloadUrl = asset['browser_download_url'] as String? ?? '';
      if (downloadUrl.isEmpty) {
        continue;
      }
      return ReleaseAssetInfo(
        name: name,
        downloadUrl: downloadUrl,
      );
    }
    return null;
  }
}
