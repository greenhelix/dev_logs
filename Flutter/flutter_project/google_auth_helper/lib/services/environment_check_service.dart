import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../models/app_settings.dart';
import '../models/environment_check_status.dart';
import 'adb_service.dart';
import 'auth_header_provider.dart';

class EnvironmentCheckService {
  EnvironmentCheckService({
    required AuthHeaderProvider authHeaderProvider,
    required AdbService adbService,
    http.Client? httpClient,
  })  : _authHeaderProvider = authHeaderProvider,
        _adbService = adbService,
        _httpClient = httpClient ?? http.Client();

  final AuthHeaderProvider _authHeaderProvider;
  final AdbService _adbService;
  final http.Client _httpClient;

  Future<EnvironmentCheckStatus> check(AppSettings settings) async {
    final baseUrl = _normalizeBaseUrl(settings.webProxyBaseUrl);
    final hosting = await _probeHosting(baseUrl);
    final download = await _probeFirestoreDownload(baseUrl, settings);
    final upload = await _probeFirestoreUpload(baseUrl, settings);
    final adb = await _probeAdb();
    final redmine = await _probeRedmine(baseUrl, settings);

    return EnvironmentCheckStatus(
      hosting: hosting,
      firestoreDownload: download,
      firestoreUpload: upload,
      adb: adb,
      redmineConnection: redmine.connection,
      redmineCurrentUser: redmine.currentUser,
      redmineProjectAccess: redmine.projectAccess,
    );
  }

  Future<EnvironmentProbeResult> _probeHosting(String baseUrl) async {
    try {
      final response = await _httpClient.get(Uri.parse('${baseUrl}api/health'));
      if (_isSuccess(response.statusCode)) {
        return const EnvironmentProbeResult(
          label: 'Firebase Hosting',
          isOk: true,
          message: 'Hosting health endpoint is reachable.',
        );
      }
      return EnvironmentProbeResult(
        label: 'Firebase Hosting',
        isOk: false,
        message: 'HTTP ${response.statusCode}',
      );
    } catch (error) {
      return EnvironmentProbeResult(
        label: 'Firebase Hosting',
        isOk: false,
        message: '$error',
      );
    }
  }

  Future<EnvironmentProbeResult> _probeFirestoreDownload(
    String baseUrl,
    AppSettings settings,
  ) async {
    try {
      final response = await _httpClient
          .get(Uri.parse('${baseUrl}api/test-metrics?limit=1'));
      if (_isSuccess(response.statusCode)) {
        return const EnvironmentProbeResult(
          label: 'Firestore Download',
          isOk: true,
          message: 'Download path is reachable.',
        );
      }
      return EnvironmentProbeResult(
        label: 'Firestore Download',
        isOk: false,
        message: 'HTTP ${response.statusCode}',
      );
    } catch (_) {
      try {
        await _authHeaderProvider.buildHeaders(settings);
        return const EnvironmentProbeResult(
          label: 'Firestore Download',
          isOk: true,
          message: 'Credentials are available.',
        );
      } catch (error) {
        return EnvironmentProbeResult(
          label: 'Firestore Download',
          isOk: false,
          message: '$error',
        );
      }
    }
  }

  Future<EnvironmentProbeResult> _probeFirestoreUpload(
    String baseUrl,
    AppSettings settings,
  ) async {
    try {
      final response = await _httpClient.post(
        Uri.parse('${baseUrl}api/upload-health'),
        headers: const {'Content-Type': 'application/json'},
        body: jsonEncode({'probe': true}),
      );
      if (_isSuccess(response.statusCode)) {
        return const EnvironmentProbeResult(
          label: 'Firestore Upload',
          isOk: true,
          message: 'Upload path is reachable.',
        );
      }
      return EnvironmentProbeResult(
        label: 'Firestore Upload',
        isOk: false,
        message: 'HTTP ${response.statusCode}',
      );
    } catch (_) {
      try {
        await _authHeaderProvider.buildHeaders(settings);
        return const EnvironmentProbeResult(
          label: 'Firestore Upload',
          isOk: true,
          message: 'Credentials are available.',
        );
      } catch (error) {
        return EnvironmentProbeResult(
          label: 'Firestore Upload',
          isOk: false,
          message: '$error',
        );
      }
    }
  }

  Future<EnvironmentProbeResult> _probeAdb() async {
    if (!_adbService.isSupported) {
      return const EnvironmentProbeResult(
        label: 'ADB',
        isOk: false,
        message: 'ADB checks are not supported on this platform.',
      );
    }

    final snapshot = await _adbService.inspect();
    if (!snapshot.available) {
      return EnvironmentProbeResult(
        label: 'ADB',
        isOk: false,
        message: snapshot.message,
      );
    }

    final readyCount =
        snapshot.devices.where((device) => device.isReady).length;
    return EnvironmentProbeResult(
      label: 'ADB',
      isOk: true,
      message:
          'ADB ready. ${snapshot.devices.length} device(s) detected, $readyCount ready for run.',
    );
  }

  Future<_RedmineProbeSet> _probeRedmine(
    String baseUrl,
    AppSettings settings,
  ) async {
    final redmineBaseUrl = settings.redmineBaseUrl.trim();
    final apiKey = settings.redmineApiKey.trim();
    if (redmineBaseUrl.isEmpty || apiKey.isEmpty) {
      return const _RedmineProbeSet(
        connection: EnvironmentProbeResult(
          label: 'Redmine Connection',
          isOk: false,
          message: 'Redmine settings are required.',
        ),
        currentUser: EnvironmentProbeResult(
          label: 'Redmine Current User',
          isOk: false,
          message: 'Redmine settings are required.',
        ),
        projectAccess: EnvironmentProbeResult(
          label: 'Redmine Project Access',
          isOk: false,
          message: 'Redmine settings are required.',
        ),
      );
    }

    if (kIsWeb) {
      return _probeRedmineFromWeb(baseUrl, settings);
    }
    return _probeRedmineDirect(settings);
  }

  Future<_RedmineProbeSet> _probeRedmineFromWeb(
    String baseUrl,
    AppSettings settings,
  ) async {
    try {
      final response = await _httpClient.post(
        Uri.parse('${baseUrl}api/redmine-health'),
        headers: const {'Content-Type': 'application/json'},
        body: jsonEncode({
          'baseUrl': settings.redmineBaseUrl,
          'apiKey': settings.redmineApiKey,
          'projectId': settings.redmineProjectId,
        }),
      );
      if (!_isSuccess(response.statusCode)) {
        return _failedRedmineSet('HTTP ${response.statusCode}');
      }
      final payload = jsonDecode(response.body) as Map<String, dynamic>;
      final results = payload['results'] as Map<String, dynamic>? ?? const {};
      return _RedmineProbeSet(
        connection: _probeFromPayload(
          'Redmine Connection',
          results['connection'] as Map<String, dynamic>?,
        ),
        currentUser: _probeFromPayload(
          'Redmine Current User',
          results['currentUser'] as Map<String, dynamic>?,
        ),
        projectAccess: _probeFromPayload(
          'Redmine Project Access',
          results['projectAccess'] as Map<String, dynamic>?,
        ),
      );
    } catch (error) {
      return _failedRedmineSet('$error');
    }
  }

  Future<_RedmineProbeSet> _probeRedmineDirect(AppSettings settings) async {
    final normalizedBase =
        settings.redmineBaseUrl.replaceAll(RegExp(r'/$'), '');
    final headers = {'X-Redmine-API-Key': settings.redmineApiKey};
    final projectPath = settings.redmineProjectId.trim().isEmpty
        ? '$normalizedBase/projects.json?limit=1'
        : '$normalizedBase/projects/${settings.redmineProjectId.trim()}.json';

    final connection = await _requestProbe(
      label: 'Redmine Connection',
      uri: Uri.parse('$normalizedBase/issues.json?limit=1'),
      headers: headers,
    );
    final currentUser = await _requestProbe(
      label: 'Redmine Current User',
      uri: Uri.parse('$normalizedBase/users/current.json'),
      headers: headers,
    );
    final projectAccess = await _requestProbe(
      label: 'Redmine Project Access',
      uri: Uri.parse(projectPath),
      headers: headers,
    );

    return _RedmineProbeSet(
      connection: connection,
      currentUser: currentUser,
      projectAccess: projectAccess,
    );
  }

  Future<EnvironmentProbeResult> _requestProbe({
    required String label,
    required Uri uri,
    required Map<String, String> headers,
  }) async {
    try {
      final response = await _httpClient.get(uri, headers: headers);
      if (_isSuccess(response.statusCode)) {
        return EnvironmentProbeResult(
          label: label,
          isOk: true,
          message: 'HTTP ${response.statusCode}',
        );
      }
      return EnvironmentProbeResult(
        label: label,
        isOk: false,
        message: 'HTTP ${response.statusCode}',
      );
    } catch (error) {
      return EnvironmentProbeResult(
        label: label,
        isOk: false,
        message: '$error',
      );
    }
  }

  EnvironmentProbeResult _probeFromPayload(
    String label,
    Map<String, dynamic>? map,
  ) {
    if (map == null) {
      return EnvironmentProbeResult(
        label: label,
        isOk: false,
        message: 'No response payload.',
      );
    }
    return EnvironmentProbeResult(
      label: label,
      isOk: map['ok'] as bool? ?? false,
      message: map['message'] as String? ?? 'No message.',
    );
  }

  _RedmineProbeSet _failedRedmineSet(String message) {
    return _RedmineProbeSet(
      connection: EnvironmentProbeResult(
        label: 'Redmine Connection',
        isOk: false,
        message: message,
      ),
      currentUser: EnvironmentProbeResult(
        label: 'Redmine Current User',
        isOk: false,
        message: message,
      ),
      projectAccess: EnvironmentProbeResult(
        label: 'Redmine Project Access',
        isOk: false,
        message: message,
      ),
    );
  }

  bool _isSuccess(int statusCode) => statusCode >= 200 && statusCode < 300;

  String _normalizeBaseUrl(String raw) {
    final trimmed = raw.trim();
    if (trimmed.isEmpty || trimmed == '/') {
      return '/';
    }
    return trimmed.endsWith('/') ? trimmed : '$trimmed/';
  }
}

class _RedmineProbeSet {
  const _RedmineProbeSet({
    required this.connection,
    required this.currentUser,
    required this.projectAccess,
  });

  final EnvironmentProbeResult connection;
  final EnvironmentProbeResult currentUser;
  final EnvironmentProbeResult projectAccess;
}
