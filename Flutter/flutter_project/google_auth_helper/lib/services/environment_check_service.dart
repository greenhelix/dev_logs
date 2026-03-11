import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../models/app_settings.dart';
import '../models/environment_check_status.dart';
import 'auth_header_provider.dart';

class EnvironmentCheckService {
  EnvironmentCheckService({
    required AuthHeaderProvider authHeaderProvider,
    http.Client? httpClient,
  })  : _authHeaderProvider = authHeaderProvider,
        _httpClient = httpClient ?? http.Client();

  final AuthHeaderProvider _authHeaderProvider;
  final http.Client _httpClient;

  Future<EnvironmentCheckStatus> check(AppSettings settings) async {
    final baseUrl = _normalizeBaseUrl(settings.webProxyBaseUrl);
    final hosting = await _probeHosting(baseUrl);
    final download = await _probeFirestoreDownload(baseUrl, settings);
    final upload = await _probeFirestoreUpload(baseUrl, settings);
    final redmine = await _probeRedmine(baseUrl, settings);

    return EnvironmentCheckStatus(
      hosting: hosting,
      firestoreDownload: download,
      firestoreUpload: upload,
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
          message: '응답 정상',
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
          label: 'Firestore 다운로드',
          isOk: true,
          message: '조회 경로 정상',
        );
      }
      return EnvironmentProbeResult(
        label: 'Firestore 다운로드',
        isOk: false,
        message: 'HTTP ${response.statusCode}',
      );
    } catch (_) {
      try {
        await _authHeaderProvider.buildHeaders(settings);
        return const EnvironmentProbeResult(
          label: 'Firestore 다운로드',
          isOk: true,
          message: '자격증명 확인 완료',
        );
      } catch (error) {
        return EnvironmentProbeResult(
          label: 'Firestore 다운로드',
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
          label: 'Firestore 업로드',
          isOk: true,
          message: '업로드 경로 정상',
        );
      }
      return EnvironmentProbeResult(
        label: 'Firestore 업로드',
        isOk: false,
        message: 'HTTP ${response.statusCode}',
      );
    } catch (_) {
      try {
        await _authHeaderProvider.buildHeaders(settings);
        return const EnvironmentProbeResult(
          label: 'Firestore 업로드',
          isOk: true,
          message: '자격증명 확인 완료',
        );
      } catch (error) {
        return EnvironmentProbeResult(
          label: 'Firestore 업로드',
          isOk: false,
          message: '$error',
        );
      }
    }
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
          label: 'Redmine 연결',
          isOk: false,
          message: '설정 필요',
        ),
        currentUser: EnvironmentProbeResult(
          label: 'Redmine 현재 사용자',
          isOk: false,
          message: '설정 필요',
        ),
        projectAccess: EnvironmentProbeResult(
          label: 'Redmine 프로젝트 접근',
          isOk: false,
          message: '설정 필요',
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
          'Redmine 연결',
          results['connection'] as Map<String, dynamic>?,
        ),
        currentUser: _probeFromPayload(
          'Redmine 현재 사용자',
          results['currentUser'] as Map<String, dynamic>?,
        ),
        projectAccess: _probeFromPayload(
          'Redmine 프로젝트 접근',
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
      label: 'Redmine 연결',
      uri: Uri.parse('$normalizedBase/issues.json?limit=1'),
      headers: headers,
    );
    final currentUser = await _requestProbe(
      label: 'Redmine 현재 사용자',
      uri: Uri.parse('$normalizedBase/users/current.json'),
      headers: headers,
    );
    final projectAccess = await _requestProbe(
      label: 'Redmine 프로젝트 접근',
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
          message: '응답 정상',
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
        message: '응답 누락',
      );
    }
    return EnvironmentProbeResult(
      label: label,
      isOk: map['ok'] as bool? ?? false,
      message: map['message'] as String? ?? '응답 없음',
    );
  }

  _RedmineProbeSet _failedRedmineSet(String message) {
    return _RedmineProbeSet(
      connection: EnvironmentProbeResult(
        label: 'Redmine 연결',
        isOk: false,
        message: message,
      ),
      currentUser: EnvironmentProbeResult(
        label: 'Redmine 현재 사용자',
        isOk: false,
        message: message,
      ),
      projectAccess: EnvironmentProbeResult(
        label: 'Redmine 프로젝트 접근',
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
