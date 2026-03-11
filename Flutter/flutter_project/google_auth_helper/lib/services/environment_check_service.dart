import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../models/app_settings.dart';
import '../models/environment_check_status.dart';
import 'adb_service.dart';
import 'auth_header_provider.dart';

enum EnvironmentCheckProgressPhase { started, finished }

class EnvironmentCheckProgress {
  const EnvironmentCheckProgress({
    required this.phase,
    required this.label,
    required this.message,
    this.result,
  });

  final EnvironmentCheckProgressPhase phase;
  final String label;
  final String message;
  final EnvironmentProbeResult? result;
}

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

  Future<EnvironmentCheckStatus> check(
    AppSettings settings, {
    void Function(EnvironmentCheckProgress progress)? onProgress,
  }) async {
    final baseUrl = _normalizeBaseUrl(settings.webProxyBaseUrl);
    final hosting = await _runProbe(
      label: 'Firebase Hosting',
      message: '호스팅 상태를 확인합니다.',
      onProgress: onProgress,
      action: () => _probeHosting(baseUrl),
    );
    final download = await _runProbe(
      label: 'Firestore Download',
      message: 'Firestore 조회 경로를 확인합니다.',
      onProgress: onProgress,
      action: () => _probeFirestoreDownload(baseUrl, settings),
    );
    final upload = await _runProbe(
      label: 'Firestore Upload',
      message: 'Firestore 업로드 경로를 확인합니다.',
      onProgress: onProgress,
      action: () => _probeFirestoreUpload(baseUrl, settings),
    );
    final adb = await _runProbe(
      label: 'ADB',
      message: 'ADB 실행 파일과 연결 장치를 확인합니다.',
      onProgress: onProgress,
      action: () => _probeAdb(settings),
    );
    final redmine = await _probeRedmine(
      baseUrl,
      settings,
      onProgress: onProgress,
    );

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

  Future<EnvironmentProbeResult> _runProbe({
    required String label,
    required String message,
    required Future<EnvironmentProbeResult> Function() action,
    void Function(EnvironmentCheckProgress progress)? onProgress,
  }) async {
    onProgress?.call(
      EnvironmentCheckProgress(
        phase: EnvironmentCheckProgressPhase.started,
        label: label,
        message: message,
      ),
    );
    final result = await action();
    onProgress?.call(
      EnvironmentCheckProgress(
        phase: EnvironmentCheckProgressPhase.finished,
        label: label,
        message: result.message,
        result: result,
      ),
    );
    return result;
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

  Future<EnvironmentProbeResult> _probeAdb(AppSettings settings) async {
    if (!_adbService.isSupported) {
      return const EnvironmentProbeResult(
        label: 'ADB',
        isOk: true,
        message: '이 플랫폼에서는 ADB 점검을 표시하지 않습니다.',
      );
    }

    final snapshot = await _adbService.inspect(
      configuredPath: settings.adbExecutablePath,
    );
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
          'ADB 사용 가능, 장치 ${snapshot.devices.length}개 중 실행 가능 $readyCount개를 찾았습니다.',
    );
  }

  Future<_RedmineProbeSet> _probeRedmine(
    String baseUrl,
    AppSettings settings, {
    void Function(EnvironmentCheckProgress progress)? onProgress,
  }) async {
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
      return _probeRedmineFromWeb(
        baseUrl,
        settings,
        onProgress: onProgress,
      );
    }
    return _probeRedmineDirect(
      settings,
      onProgress: onProgress,
    );
  }

  Future<_RedmineProbeSet> _probeRedmineFromWeb(
    String baseUrl,
    AppSettings settings, {
    void Function(EnvironmentCheckProgress progress)? onProgress,
  }) async {
    onProgress?.call(
      const EnvironmentCheckProgress(
        phase: EnvironmentCheckProgressPhase.started,
        label: 'Redmine Proxy',
        message: '웹 프록시를 통해 Redmine 연결 상태를 확인합니다.',
      ),
    );
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
        final failed = _failedRedmineSet('HTTP ${response.statusCode}');
        _notifyRedmineProgress(failed, onProgress);
        return failed;
      }
      final payload = jsonDecode(response.body) as Map<String, dynamic>;
      final results = payload['results'] as Map<String, dynamic>? ?? const {};
      final set = _RedmineProbeSet(
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
      _notifyRedmineProgress(set, onProgress);
      return set;
    } catch (error) {
      final failed = _failedRedmineSet('$error');
      _notifyRedmineProgress(failed, onProgress);
      return failed;
    }
  }

  Future<_RedmineProbeSet> _probeRedmineDirect(
    AppSettings settings, {
    void Function(EnvironmentCheckProgress progress)? onProgress,
  }) async {
    final normalizedBase =
        settings.redmineBaseUrl.replaceAll(RegExp(r'/$'), '');
    final headers = {'X-Redmine-API-Key': settings.redmineApiKey};
    final projectPath = settings.redmineProjectId.trim().isEmpty
        ? '$normalizedBase/projects.json?limit=1'
        : '$normalizedBase/projects/${settings.redmineProjectId.trim()}.json';

    final connection = await _runProbe(
      label: 'Redmine Connection',
      message: 'Redmine 이슈 조회 연결을 확인합니다.',
      onProgress: onProgress,
      action: () => _requestProbe(
        label: 'Redmine Connection',
        uri: Uri.parse('$normalizedBase/issues.json?limit=1'),
        headers: headers,
      ),
    );
    final currentUser = await _runProbe(
      label: 'Redmine Current User',
      message: 'Redmine 현재 사용자 조회를 확인합니다.',
      onProgress: onProgress,
      action: () => _requestProbe(
        label: 'Redmine Current User',
        uri: Uri.parse('$normalizedBase/users/current.json'),
        headers: headers,
      ),
    );
    final projectAccess = await _runProbe(
      label: 'Redmine Project Access',
      message: 'Redmine 프로젝트 접근 권한을 확인합니다.',
      onProgress: onProgress,
      action: () => _requestProbe(
        label: 'Redmine Project Access',
        uri: Uri.parse(projectPath),
        headers: headers,
      ),
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

  void _notifyRedmineProgress(
    _RedmineProbeSet set,
    void Function(EnvironmentCheckProgress progress)? onProgress,
  ) {
    for (final result in [
      set.connection,
      set.currentUser,
      set.projectAccess,
    ]) {
      onProgress?.call(
        EnvironmentCheckProgress(
          phase: EnvironmentCheckProgressPhase.finished,
          label: result.label,
          message: result.message,
          result: result,
        ),
      );
    }
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
