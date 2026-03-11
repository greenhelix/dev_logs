import 'dart:convert';
import 'dart:io';

import 'package:dart_jsonwebtoken/dart_jsonwebtoken.dart';
import 'package:http/http.dart' as http;
import 'package:path/path.dart' as path;

import '../models/app_settings.dart';
import 'auth_header_provider.dart';

class IoAuthHeaderProvider implements AuthHeaderProvider {
  @override
  Future<Map<String, String>> buildHeaders(AppSettings settings) async {
    switch (settings.credentialMode) {
      case CredentialMode.localToken:
        final token = await _readFirebaseCliAccessToken();
        return {'Authorization': 'Bearer $token'};
      case CredentialMode.serviceAccountFile:
        final token =
            await _exchangeServiceAccount(settings.serviceAccountPath);
        return {'Authorization': 'Bearer $token'};
    }
  }

  Future<String> _readFirebaseCliAccessToken() async {
    final configPath = _defaultFirebaseToolsPath();
    final file = File(configPath);
    if (!await file.exists()) {
      throw StateError(
        'firebase-tools.json was not found. Run firebase login first.',
      );
    }

    final json = jsonDecode(await file.readAsString()) as Map<String, dynamic>;
    final tokens = json['tokens'] as Map<String, dynamic>? ?? const {};
    final accessToken =
        tokens['access_token'] as String? ?? json['access_token'] as String?;
    if (accessToken == null || accessToken.isEmpty) {
      throw StateError('firebase-tools.json does not contain access_token.');
    }
    return accessToken;
  }

  Future<String> _exchangeServiceAccount(String serviceAccountPath) async {
    final normalizedPath = path.normalize(serviceAccountPath.trim());
    if (normalizedPath.isEmpty) {
      throw StateError('Service account path is empty.');
    }

    final json = jsonDecode(
      await File(normalizedPath).readAsString(),
    ) as Map<String, dynamic>;

    final privateKey = json['private_key'] as String? ?? '';
    final clientEmail = json['client_email'] as String? ?? '';
    final privateKeyId = json['private_key_id'] as String? ?? '';
    final tokenUri =
        json['token_uri'] as String? ?? 'https://oauth2.googleapis.com/token';
    final now = DateTime.now().toUtc();

    final jwt = JWT(
      {
        'iss': clientEmail,
        'sub': clientEmail,
        'aud': tokenUri,
        'scope': 'https://www.googleapis.com/auth/datastore',
        'iat': now.millisecondsSinceEpoch ~/ 1000,
        'exp': now.add(const Duration(hours: 1)).millisecondsSinceEpoch ~/ 1000,
      },
      header: {
        'alg': 'RS256',
        'typ': 'JWT',
        'kid': privateKeyId,
      },
    );

    final assertion = jwt.sign(
      RSAPrivateKey(privateKey),
      algorithm: JWTAlgorithm.RS256,
      noIssueAt: true,
    );

    final response = await http.post(
      Uri.parse(tokenUri),
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      body: {
        'grant_type': 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion': assertion,
      },
    );

    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw StateError(
        'Service account exchange failed: ${response.statusCode} ${response.body}',
      );
    }

    final payload = jsonDecode(response.body) as Map<String, dynamic>;
    final accessToken = payload['access_token'] as String? ?? '';
    if (accessToken.isEmpty) {
      throw StateError('OAuth response did not include access_token.');
    }
    return accessToken;
  }

  String _defaultFirebaseToolsPath() {
    if (Platform.isWindows) {
      final appData = Platform.environment['APPDATA'] ?? '';
      return path.join(appData, 'configstore', 'firebase-tools.json');
    }
    final home = Platform.environment['HOME'] ?? '';
    return path.join(home, '.config', 'configstore', 'firebase-tools.json');
  }
}

AuthHeaderProvider createProvider() => IoAuthHeaderProvider();
