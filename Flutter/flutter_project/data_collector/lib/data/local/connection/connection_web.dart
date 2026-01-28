import 'package:drift/drift.dart';
import 'package:drift/web.dart';

QueryExecutor openConnection() {
  // Drift Web 지원 기능을 사용 (IndexedDB)
  return WebDatabase('data_collector_db');
}
