import 'dart:convert';

import 'package:http/http.dart' as http;

import '../core/config/app_defaults.dart';
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
      );
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw StateError(
        'Failed to fetch release status: ${response.statusCode}',
      );
    }

    final payload = jsonDecode(response.body) as Map<String, dynamic>;
    return ReleaseStatus(
      currentVersion: AppDefaults.appVersion,
      latestVersion: payload['tag_name'] as String? ?? '',
      releaseUrl: payload['html_url'] as String? ??
          'https://github.com/greenhelix/GAH-Release-Repo/releases',
    );
  }
}
