import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/models/app_settings.dart';
import 'package:google_auth_helper/models/adb_snapshot.dart';
import 'package:google_auth_helper/models/tool_config.dart';
import 'package:google_auth_helper/services/auth_header_provider.dart';
import 'package:google_auth_helper/services/adb_service.dart';
import 'package:google_auth_helper/services/environment_check_service.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

class _FakeAuthHeaderProvider implements AuthHeaderProvider {
  @override
  Future<Map<String, String>> buildHeaders(AppSettings settings) async {
    return const {'Authorization': 'Bearer fake'};
  }
}

class _FakeAdbService implements AdbService {
  @override
  bool get isSupported => true;

  @override
  Future<AdbSnapshot> inspect({String? configuredPath}) async {
    return const AdbSnapshot(
      available: true,
      version: 'Android Debug Bridge version 1.0.41',
      devices: [],
      message: 'ADB is ready.',
    );
  }
}

void main() {
  test('EnvironmentCheckService splits Redmine probes on desktop flow',
      () async {
    final client = MockClient((request) async {
      final url = request.url.toString();
      if (url.endsWith('/api/health')) {
        return http.Response('{}', 200);
      }
      if (url.endsWith('/api/test-metrics?limit=1')) {
        return http.Response('{}', 200);
      }
      if (url.endsWith('/api/upload-health')) {
        return http.Response('{}', 200);
      }
      if (url.endsWith('/issues.json?limit=1')) {
        return http.Response(jsonEncode({'issues': []}), 200);
      }
      if (url.endsWith('/users/current.json')) {
        return http.Response(
            jsonEncode({
              'user': {'id': 1}
            }),
            200);
      }
      if (url.endsWith('/projects/demo.json')) {
        return http.Response(
            jsonEncode({
              'project': {'id': 1}
            }),
            200);
      }
      return http.Response('not found', 404);
    });

    final service = EnvironmentCheckService(
      authHeaderProvider: _FakeAuthHeaderProvider(),
      adbService: _FakeAdbService(),
      httpClient: client,
    );
    final progressLabels = <String>[];

    final status = await service.check(
      AppSettings(
        firebaseProjectId: 'kani-projects',
        firestoreDatabaseId: 'google-auth',
        credentialMode: CredentialMode.serviceAccountFile,
        serviceAccountPath: '',
        adbExecutablePath: '',
        webProxyBaseUrl: '/',
        redmineBaseUrl: 'https://redmine.example.com',
        redmineApiKey: 'secret',
        redmineProjectId: 'demo',
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
      ),
      onProgress: (progress) {
        progressLabels.add('${progress.phase.name}:${progress.label}');
      },
    );

    expect(status.hosting.isOk, isTrue);
    expect(status.firestoreDownload.isOk, isTrue);
    expect(status.firestoreUpload.isOk, isTrue);
    expect(status.adb.isOk, isTrue);
    expect(status.redmineConnection.isOk, isTrue);
    expect(status.redmineCurrentUser.isOk, isTrue);
    expect(status.redmineProjectAccess.isOk, isTrue);
    expect(
      progressLabels,
      containsAllInOrder([
        'started:Firebase Hosting',
        'finished:Firebase Hosting',
        'started:Firestore Download',
        'finished:Firestore Download',
        'started:Firestore Upload',
        'finished:Firestore Upload',
        'started:ADB',
        'finished:ADB',
        'started:Redmine Connection',
        'finished:Redmine Connection',
        'started:Redmine Current User',
        'finished:Redmine Current User',
        'started:Redmine Project Access',
        'finished:Redmine Project Access',
      ]),
    );
  });
}
