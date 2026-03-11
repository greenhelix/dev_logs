import 'package:flutter/foundation.dart';

import '../core/constants/firestore_collections.dart';
import '../models/failed_test_record.dart';
import '../models/import_bundle.dart';
import '../models/test_case_record.dart';
import '../models/test_metric_record.dart';
import 'demo_seed_data.dart';
import 'firestore_rest_client.dart';

class FirestoreRepository {
  FirestoreRepository({required FirestoreRestClient client}) : _client = client;

  final FirestoreRestClient _client;

  Future<List<TestCaseRecord>> fetchTestCases({int limit = 80}) async {
    try {
      final documents = await _client.listDocuments(
        FirestoreCollections.testCases,
        limit: limit,
      );
      return documents.map(TestCaseRecord.fromMap).toList(growable: false);
    } catch (_) {
      if (kIsWeb) {
        return DemoSeedData.testCases().take(limit).toList(growable: false);
      }
      rethrow;
    }
  }

  Future<List<FailedTestRecord>> fetchFailedTests({int limit = 80}) async {
    try {
      final documents = await _client.listDocuments(
        FirestoreCollections.failedTests,
        limit: limit,
      );
      return documents.map(FailedTestRecord.fromMap).toList(growable: false);
    } catch (_) {
      if (kIsWeb) {
        return DemoSeedData.failedTests().take(limit).toList(growable: false);
      }
      rethrow;
    }
  }

  Future<List<TestMetricRecord>> fetchMetrics({int limit = 40}) async {
    try {
      final documents = await _client.listDocuments(
        FirestoreCollections.testMetrics,
        limit: limit,
      );
      return documents.map(TestMetricRecord.fromMap).toList(growable: false);
    } catch (_) {
      if (kIsWeb) {
        return DemoSeedData.metrics().take(limit).toList(growable: false);
      }
      rethrow;
    }
  }

  Future<void> upsertTestCases(Iterable<TestCaseRecord> records) async {
    for (final record in records) {
      await _client.upsertDocument(
        FirestoreCollections.testCases,
        record.id,
        record.toMap(),
      );
    }
  }

  Future<void> upsertFailedTests(Iterable<FailedTestRecord> records) async {
    for (final record in records) {
      await _client.upsertDocument(
        FirestoreCollections.failedTests,
        record.id,
        record.toMap(),
      );
    }
  }

  Future<void> upsertMetrics(Iterable<TestMetricRecord> records) async {
    for (final record in records) {
      await _client.upsertDocument(
        FirestoreCollections.testMetrics,
        record.id,
        record.toMap(),
      );
    }
  }

  Future<void> syncImportBundle(ImportBundle bundle) async {
    final effectiveBundle = bundle.copyWith(
      metric: bundle.metric.copyWith(
        excludedFailureCount: bundle.excludedFailedTests.length,
      ),
    );
    if (kIsWeb) {
      await _client.syncImportBundle({
        'metric': effectiveBundle.metric.toMap(),
        'testCases': effectiveBundle.testCases
            .map((item) => item.toMap())
            .toList(growable: false),
        'failedTests': effectiveBundle.activeFailedTests
            .map((item) => item.toMap())
            .toList(growable: false),
      });
      return;
    }
    await upsertMetrics([effectiveBundle.metric]);
    await upsertTestCases(effectiveBundle.testCases);
    await upsertFailedTests(effectiveBundle.activeFailedTests);
  }
}
