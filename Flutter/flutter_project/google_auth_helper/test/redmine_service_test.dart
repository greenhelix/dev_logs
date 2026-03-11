import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/models/app_settings.dart';
import 'package:google_auth_helper/models/import_bundle.dart';
import 'package:google_auth_helper/models/live_status.dart';
import 'package:google_auth_helper/models/test_metric_record.dart';
import 'package:google_auth_helper/models/tool_config.dart';
import 'package:google_auth_helper/services/redmine_service.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

AppSettings _settings() {
  return AppSettings(
    firebaseProjectId: 'kani-projects',
    firestoreDatabaseId: 'google-auth',
    credentialMode: CredentialMode.serviceAccountFile,
    serviceAccountPath: '',
    adbExecutablePath: '',
    webProxyBaseUrl: '/',
    redmineBaseUrl: 'https://redmine.example.com',
    redmineApiKey: 'secret',
    redmineProjectId: '69',
    toolConfigs: const [
      ToolConfig(
        toolType: ToolType.cts,
        toolRoot: '',
        resultsDir: '',
        logsDir: '',
        defaultCommand: 'run cts',
        autoUploadAfterRun: true,
      ),
    ],
  );
}

ImportBundle _bundle() {
  return ImportBundle(
    metric: TestMetricRecord(
      id: 'metric-1',
      toolType: 'CTS',
      suiteName: 'CTS',
      suiteVersion: '14_r10',
      fwVersion: '403_1',
      totalTests: 10,
      passCount: 7,
      failCount: 1,
      ignoredCount: 1,
      assumptionFailureCount: 1,
      warningCount: 0,
      moduleCount: 1,
      durationSeconds: 600,
      devices: const ['serial1'],
      timestamp: DateTime.parse('2026-01-05T16:04:08Z'),
      buildDevice: 'IMTM8300_HU',
      androidVersion: '14',
      buildType: 'user',
      buildFingerprint:
          'TelekomTV/IMTM8300_HU/IMTM8300_HU:14/UTT2.250604.001/403_1:user/release-keys',
      countSource: 'xts_tf_output.log',
    ),
    testCases: const [],
    failedTests: const [],
    liveStatus: LiveStatus.empty,
    resultPath: 'test_result.xml',
    logPath: 'xts_tf_output.log',
  );
}

void main() {
  test('RedmineService fetches current user, project list, and project detail',
      () async {
    final service = RedmineService(
      httpClient: MockClient((request) async {
        if (request.url.path.endsWith('/users/current.json')) {
          return http.Response(
            '{"user":{"id":12,"login":"kim","firstname":"Kim","lastname":"Coder"}}',
            200,
          );
        }
        if (request.url.path.endsWith('/projects.json')) {
          return http.Response(
            '{"projects":[{"id":69,"identifier":"gahi","name":"GAH Integration"}]}',
            200,
          );
        }
        if (request.url.path.endsWith('/projects/69.json')) {
          return http.Response(
            '{"project":{"id":69,"identifier":"gahi","name":"GAH Integration"}}',
            200,
          );
        }
        return http.Response('not found', 404);
      }),
    );

    final settings = _settings();
    final user = await service.fetchCurrentUser(settings: settings);
    final projects = await service.fetchProjects(settings: settings);
    final project = await service.fetchProject(
      settings: settings,
      projectId: '69',
    );

    expect(user.displayName, 'Kim Coder');
    expect(projects.single.id, 69);
    expect(project.displayLabel, contains('#69'));
  });

  test('RedmineService creates issue with build identity in payload', () async {
    late Map<String, dynamic> payload;
    final service = RedmineService(
      httpClient: MockClient((request) async {
        payload = jsonDecode(request.body) as Map<String, dynamic>;
        return http.Response('{"issue":{"id":1001}}', 201);
      }),
    );

    await service.createIssue(
      settings: _settings(),
      bundle: _bundle(),
    );

    final issue = payload['issue'] as Map<String, dynamic>;
    expect(issue['project_id'], '69');
    expect(issue['subject'], contains('IMTM8300_HU'));
    expect(issue['description'], contains('Fingerprint'));
    expect(issue['description'], contains('403_1'));
  });
}
