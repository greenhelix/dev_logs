import 'package:drift/drift.dart';
import 'package:drift/web.dart';

QueryExecutor openConnecrtion() {
  return WebDatabase.withStorage(
      DriftWebStorage.indexedDb("data_collector_db"));
}
