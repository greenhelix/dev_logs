import 'package:drift/drift.dart';
//import 'package:drift/web.dart';

// QueryExecutor openConnection() {
//   // Drift Web 지원 기능을 사용 (IndexedDB)
//   return WebDatabase('data_collector_db');
// }

QueryExecutor openConnection() {
  return LazyDatabase(() async {
    throw UnsupportedError('웹 환경에서는 Drift(Local DB) 대신 Firestore를 사용합니다.');
  });
}
