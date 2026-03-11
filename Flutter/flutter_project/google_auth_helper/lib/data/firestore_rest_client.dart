import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../models/app_settings.dart';
import '../services/auth_header_provider.dart';
import 'firestore_field_codec.dart';

class FirestoreRestClient {
  FirestoreRestClient({
    required AppSettings settings,
    required AuthHeaderProvider authHeaderProvider,
    http.Client? httpClient,
  })  : _settings = settings,
        _authHeaderProvider = authHeaderProvider,
        _httpClient = httpClient ?? http.Client();

  final AppSettings _settings;
  final AuthHeaderProvider _authHeaderProvider;
  final http.Client _httpClient;

  Future<List<Map<String, dynamic>>> listDocuments(
    String collectionId, {
    int limit = 50,
  }) async {
    if (kIsWeb) {
      final response = await _httpClient.get(
        Uri.parse(
          '${_normalizedProxyBaseUrl()}api/${_routeSegment(collectionId)}?limit=$limit',
        ),
      );
      _ensureSuccess(response);
      final payload = jsonDecode(response.body) as Map<String, dynamic>;
      final items = payload['documents'] as List<dynamic>? ?? const [];
      return items.cast<Map<String, dynamic>>();
    }

    final response = await _httpClient.get(
      Uri.parse(
        'https://firestore.googleapis.com/v1/projects/${_settings.firebaseProjectId}/databases/${_settings.firestoreDatabaseId}/documents/$collectionId?pageSize=$limit',
      ),
      headers: await _authorizedHeaders(),
    );
    _ensureSuccess(response);
    final payload = jsonDecode(response.body) as Map<String, dynamic>;
    final documents = payload['documents'] as List<dynamic>? ?? const [];
    return documents
        .map((item) {
          final document = item as Map<String, dynamic>;
          final decoded = FirestoreFieldCodec.decodeDocument(document);
          final name = document['name'] as String? ?? '';
          decoded['id'] = name.split('/').last;
          return decoded;
        })
        .cast<Map<String, dynamic>>()
        .toList(growable: false);
  }

  Future<void> upsertDocument(
    String collectionId,
    String documentId,
    Map<String, dynamic> data,
  ) async {
    if (kIsWeb) {
      throw UnsupportedError('웹에서는 Firebase Hosting 프록시 업로드만 지원합니다.');
    }

    final response = await _httpClient.patch(
      Uri.parse(
        'https://firestore.googleapis.com/v1/projects/${_settings.firebaseProjectId}/databases/${_settings.firestoreDatabaseId}/documents/$collectionId/$documentId',
      ),
      headers: {
        ...await _authorizedHeaders(),
        'Content-Type': 'application/json',
      },
      body: jsonEncode(FirestoreFieldCodec.encodeDocument(data)),
    );
    _ensureSuccess(response);
  }

  Future<void> syncImportBundle(Map<String, dynamic> payload) async {
    if (!kIsWeb) {
      throw UnsupportedError('웹 프록시 업로드는 웹 환경에서만 사용할 수 있습니다.');
    }
    final response = await _httpClient.post(
      Uri.parse('${_normalizedProxyBaseUrl()}api/sync-import'),
      headers: const {'Content-Type': 'application/json'},
      body: jsonEncode(payload),
    );
    _ensureSuccess(response);
  }

  Future<Map<String, String>> _authorizedHeaders() async {
    return {
      'Accept': 'application/json',
      ...await _authHeaderProvider.buildHeaders(_settings),
    };
  }

  String _normalizedProxyBaseUrl() {
    final base = _settings.webProxyBaseUrl.trim();
    if (base.isEmpty || base == '/') {
      return '/';
    }
    return base.endsWith('/') ? base : '$base/';
  }

  String _routeSegment(String collectionId) {
    switch (collectionId) {
      case 'TestCases':
        return 'test-cases';
      case 'FailedTests':
        return 'failed-tests';
      case 'TestMetrics':
        return 'test-metrics';
      default:
        return collectionId.toLowerCase();
    }
  }

  void _ensureSuccess(http.Response response) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw StateError(
        'Firestore 요청 실패: ${response.statusCode} ${response.body}',
      );
    }
  }
}
