import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../models/app_settings.dart';
import '../models/import_bundle.dart';
import '../models/redmine_current_user.dart';
import '../models/redmine_project_summary.dart';

class RedmineService {
  RedmineService({http.Client? httpClient})
      : _httpClient = httpClient ?? http.Client();

  final http.Client _httpClient;

  String buildMarkdown(ImportBundle bundle) {
    final lines = <String>[
      '# ${bundle.metric.suiteName} ${bundle.metric.suiteVersion}',
      '',
      '- Build: ${bundle.metric.compactBuildLabel}',
      '- Fingerprint: ${bundle.metric.buildFingerprint.isEmpty ? '-' : bundle.metric.buildFingerprint}',
      '- FW: ${bundle.metric.fwVersion}',
      '- Device: ${bundle.metric.buildDevice.isEmpty ? '-' : bundle.metric.buildDevice}',
      '- Android: ${bundle.metric.androidVersion.isEmpty ? '-' : bundle.metric.androidVersion}',
      '- Build Type: ${bundle.metric.buildType.isEmpty ? '-' : bundle.metric.buildType}',
      '- Total Tests: ${bundle.metric.totalTests}',
      '- Passed: ${bundle.metric.passCount}',
      '- Fail (log summary): ${bundle.metric.failCount}',
      '- Excluded by user: ${bundle.excludedFailedTests.length}',
      '- Fail to upload: ${bundle.activeFailedTests.length}',
      '- Ignored: ${bundle.metric.ignoredCount}',
      '- Assumption Failure: ${bundle.metric.assumptionFailureCount}',
      '- Devices: ${bundle.metric.devices.join(', ')}',
      '- Count Source: ${bundle.metric.countSource}',
      '- Status: ${bundle.liveStatus.stateSummary.isEmpty ? 'Preview ready' : bundle.liveStatus.stateSummary}',
      '',
      '## Failure List',
    ];

    if (bundle.activeFailedTests.isEmpty) {
      lines.add('- No active failures.');
    } else {
      for (final item in bundle.activeFailedTests.take(20)) {
        final memoSuffix =
            item.manualMemo.isEmpty ? '' : ' (memo: ${item.manualMemo})';
        lines.add(
          '- ${item.displayModuleName} / ${item.testName}: '
          '${item.failureMessage.isEmpty ? 'No message' : item.failureMessage}$memoSuffix',
        );
      }
    }

    if (bundle.previewWarnings.isNotEmpty) {
      lines.add('');
      lines.add('## Preview Warnings');
      for (final warning in bundle.previewWarnings) {
        lines.add('- $warning');
      }
    }

    return lines.join('\n');
  }

  Future<RedmineCurrentUser> fetchCurrentUser({
    required AppSettings settings,
  }) async {
    if (kIsWeb) {
      throw UnsupportedError('Redmine user lookup is only available on desktop.');
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
        'Redmine project listing is only available on desktop.',
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
      throw UnsupportedError('Redmine project lookup is only available on desktop.');
    }
    final normalizedProjectId = projectId.trim();
    if (normalizedProjectId.isEmpty) {
      throw StateError('Redmine project ID is required.');
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
        'Redmine Base URL, API Key, and Project ID are required.',
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
}
