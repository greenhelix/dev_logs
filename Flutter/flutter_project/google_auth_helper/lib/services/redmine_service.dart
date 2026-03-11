import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../models/app_settings.dart';
import '../models/failed_test_record.dart';
import '../models/import_bundle.dart';
import '../models/redmine_current_user.dart';
import '../models/redmine_project_summary.dart';

class RedmineService {
  RedmineService({http.Client? httpClient})
      : _httpClient = httpClient ?? http.Client();

  final http.Client _httpClient;

  String buildMarkdown(ImportBundle bundle) {
    final start = bundle.metric.timestamp;
    final end = start.add(Duration(seconds: bundle.metric.durationSeconds));
    final failuresByModule = <String, List<dynamic>>{};
    for (final item in bundle.activeFailedTests) {
      failuresByModule.putIfAbsent(item.moduleName, () => []).add(item);
    }

    final title =
        '${bundle.metric.suiteName.toLowerCase()} ${bundle.metric.suiteVersion} | ${bundle.metric.compactBuildLabel} | ${_formatDateTime(start)} | 모듈 ${bundle.metric.moduleCount} | 성공 ${bundle.metric.passCount} | 실패 ${bundle.activeFailedTests.length}';
    final lines = <String>[
      '{{collapse($title)',
      '**Suite Plan:** ${bundle.metric.suiteName.toLowerCase()} ',
      '**Suite Version:** ${bundle.metric.suiteVersion}',
      '**Fingerprint:** ${bundle.metric.buildFingerprint.isEmpty ? "-" : bundle.metric.buildFingerprint}',
      '**Version:** ${bundle.metric.fwVersion.isEmpty ? "-" : bundle.metric.fwVersion}',
      '**Security Patch:** -',
      '|START|END|RUN TIME|PASS|FAIL|Module|',
      '|-|-|-|-|-|-|',
      '|${_formatDateTime(start)}|${_formatDateTime(end)}|${_formatDuration(bundle.metric.durationSeconds)}|${bundle.metric.passCount}|${bundle.activeFailedTests.length}|${bundle.metric.moduleCount}|',
      '### 실행 정보',
      '- 장치: ${bundle.metric.devices.isEmpty ? "-" : bundle.metric.devices.join(", ")}',
      '- 안드로이드 버전: ${bundle.metric.androidVersion.isEmpty ? "-" : bundle.metric.androidVersion}',
      '- 빌드 타입: ${bundle.metric.buildType.isEmpty ? "-" : bundle.metric.buildType}',
      '- 집계 기준: ${bundle.metric.countSource}',
      '- 상태 요약: ${bundle.liveStatus.stateSummary.isEmpty ? "미리보기 준비 완료" : bundle.liveStatus.stateSummary}',
      '### 실패 모듈',
    ];

    if (failuresByModule.isEmpty) {
      lines.add('- 실패 항목이 없습니다.');
    } else {
      for (final entry in failuresByModule.entries) {
        lines.add('');
        lines.add('- [[${entry.key.isEmpty ? "미확인 모듈" : entry.key}]]');
        lines.add('  |Test|Message|');
        lines.add('  |-|-|');
        for (final raw in entry.value) {
          final item = raw as FailedTestRecord;
          final memo = item.manualMemo.isEmpty ? '' : ' / 메모: ${item.manualMemo}';
          lines.add(
            '  |${item.testName}|${_escapeTable(item.failureMessage.isEmpty ? "메시지 없음" : item.failureMessage)}$memo|',
          );
        }
      }
    }

    if (bundle.previewWarnings.isNotEmpty) {
      lines.add('');
      lines.add('### 경고');
      for (final warning in bundle.previewWarnings) {
        lines.add('- $warning');
      }
    }

    lines.add('}}');
    return lines.join('\n');
  }

  Future<RedmineCurrentUser> fetchCurrentUser({
    required AppSettings settings,
  }) async {
    if (kIsWeb) {
      throw UnsupportedError('레드마인 사용자 조회는 데스크톱에서만 지원합니다.');
    }
    final response = await _httpClient.get(
      Uri.parse('${_normalizedBaseUrl(settings)}/users/current.json'),
      headers: _desktopHeaders(settings),
    );
    _ensureSuccess(response, 'Redmine current user lookup');
    final payload = jsonDecode(response.body) as Map<String, dynamic>;
    final user = payload['user'] as Map<String, dynamic>? ?? const {};
    return RedmineCurrentUser.fromMap(user);
  }

  Future<List<RedmineProjectSummary>> fetchProjects({
    required AppSettings settings,
    int limit = 100,
    int offset = 0,
  }) async {
    if (kIsWeb) {
      throw UnsupportedError(
        '레드마인 프로젝트 목록 조회는 데스크톱에서만 지원합니다.',
      );
    }
    final response = await _httpClient.get(
      Uri.parse(
        '${_normalizedBaseUrl(settings)}/projects.json?limit=$limit&offset=$offset',
      ),
      headers: _desktopHeaders(settings),
    );
    _ensureSuccess(response, 'Redmine project list lookup');
    final payload = jsonDecode(response.body) as Map<String, dynamic>;
    final projects = payload['projects'] as List<dynamic>? ?? const [];
    return projects
        .map((item) => RedmineProjectSummary.fromMap(item as Map<String, dynamic>))
        .toList(growable: false);
  }

  Future<RedmineProjectSummary> fetchProject({
    required AppSettings settings,
    required String projectId,
  }) async {
    if (kIsWeb) {
      throw UnsupportedError('레드마인 프로젝트 조회는 데스크톱에서만 지원합니다.');
    }
    final normalizedProjectId = projectId.trim();
    if (normalizedProjectId.isEmpty) {
      throw StateError('레드마인 프로젝트 ID가 필요합니다.');
    }
    final response = await _httpClient.get(
      Uri.parse('${_normalizedBaseUrl(settings)}/projects/$normalizedProjectId.json'),
      headers: _desktopHeaders(settings),
    );
    _ensureSuccess(response, 'Redmine project lookup');
    final payload = jsonDecode(response.body) as Map<String, dynamic>;
    final project = payload['project'] as Map<String, dynamic>? ?? const {};
    return RedmineProjectSummary.fromMap(project);
  }

  Future<void> createIssue({
    required AppSettings settings,
    required ImportBundle bundle,
  }) async {
    _ensureConfigured(settings);

    final subject =
        '[${bundle.metric.suiteName}] ${bundle.metric.suiteVersion} ${bundle.metric.compactBuildLabel} failures ${bundle.activeFailedTests.length}';
    final description = buildMarkdown(bundle);

    if (kIsWeb) {
      final response = await _httpClient.post(
        Uri.parse(
            '${_normalizeProxyBaseUrl(settings.webProxyBaseUrl)}api/redmine-issues'),
        headers: const {'Content-Type': 'application/json'},
        body: jsonEncode({
          'baseUrl': settings.redmineBaseUrl.trim(),
          'apiKey': settings.redmineApiKey.trim(),
          'projectId': settings.redmineProjectId.trim(),
          'issue': {
            'subject': subject,
            'description': description,
          },
        }),
      );
      _ensureSuccess(response, 'Redmine upload');
      return;
    }

    final response = await _httpClient.post(
      Uri.parse('${_normalizedBaseUrl(settings)}/issues.json'),
      headers: _desktopHeaders(settings, contentType: 'application/json'),
      body: jsonEncode({
          'issue': {
            'project_id': settings.redmineProjectId.trim(),
            'subject': subject,
            'description': description,
        },
      }),
    );
    _ensureSuccess(response, 'Redmine upload');
  }

  Map<String, String> _desktopHeaders(
    AppSettings settings, {
    String? contentType,
  }) {
    _ensureConfigured(settings);
    return {
      if (contentType != null) 'Content-Type': contentType,
      'X-Redmine-API-Key': settings.redmineApiKey.trim(),
    };
  }

  String _normalizedBaseUrl(AppSettings settings) {
    return settings.redmineBaseUrl.trim().replaceAll(RegExp(r'/$'), '');
  }

  void _ensureConfigured(AppSettings settings) {
    final baseUrl = settings.redmineBaseUrl.trim();
    final apiKey = settings.redmineApiKey.trim();
    final projectId = settings.redmineProjectId.trim();
    if (baseUrl.isEmpty || apiKey.isEmpty || projectId.isEmpty) {
      throw StateError(
        '레드마인 주소, API 키, 프로젝트 ID를 모두 입력해야 합니다.',
      );
    }
  }

  String _normalizeProxyBaseUrl(String raw) {
    final trimmed = raw.trim();
    if (trimmed.isEmpty || trimmed == '/') {
      return '/';
    }
    return trimmed.endsWith('/') ? trimmed : '$trimmed/';
  }

  void _ensureSuccess(http.Response response, String label) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw StateError(
        '$label failed: ${response.statusCode} ${response.body}',
      );
    }
  }

  String _formatDateTime(DateTime value) {
    final local = value.toLocal();
    final year = local.year.toString().padLeft(4, '0');
    final month = local.month.toString().padLeft(2, '0');
    final day = local.day.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    final second = local.second.toString().padLeft(2, '0');
    return '$year-$month-$day-$hour:$minute:$second';
  }

  String _formatDuration(int seconds) {
    final duration = Duration(seconds: seconds);
    final hours = duration.inHours.toString().padLeft(2, '0');
    final minutes = (duration.inMinutes % 60).toString().padLeft(2, '0');
    final secs = (duration.inSeconds % 60).toString().padLeft(2, '0');
    return '$hours:$minutes:$secs';
  }

  String _escapeTable(String value) {
    return value.replaceAll('\n', ' ').replaceAll('|', '\\|');
  }
}
