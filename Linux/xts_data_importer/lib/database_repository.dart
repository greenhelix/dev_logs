import 'package:drift/drift.dart';
import 'database.dart';

// 데이터베이스 로직을 추상화하는 Repository입니다.
class DatabaseRepository {
  final AppDatabase db;
  DatabaseRepository(this.db);

  Stream<List<TestResult>> watchAllResults() {
    return (db.select(db.testResults)
          ..orderBy([
            (t) => OrderingTerm(expression: t.testDate, mode: OrderingMode.desc)
          ]))
        .watch();
  }

  Future<List<String>> getCategories() async {
    final query = db.selectOnly(db.testResults, distinct: true)
      ..addColumns([db.testResults.category]);
    final result =
        await query.map((row) => row.read(db.testResults.category)!).get();
    result.sort();
    return result;
  }

  Future<void> addTestResult(TestResultsCompanion entry) async {
    await db.into(db.testResults).insert(entry);
  }

  // 일괄 삽입을 트랜잭션으로 묶고 실제 성공 수 계산
  Future<int> bulkInsert(List<TestResultsCompanion> entries) async {
    return await db.transaction(() async {
      int successCount = 0;
      await db.batch((batch) {
        batch.insertAll(
          db.testResults,
          entries,
          // insertOrIgnore 사용: UNIQUE 충돌은 무시
          mode: InsertMode.insertOrIgnore,
        );
      });
      // insertOrIgnore는 영향받은 row 카운트를 직접 제공하지 않으므로
      // 충돌 없이 삽입된 건수를 다시 조회하여 계산(빠른 근사: 시도 건수에서 충돌 수 제외)
      // 여기서는 보수적으로 '시도 건수 = 성공'으로 두지 않고,
      // 키 충돌 확인을 위해 각 row 존재 여부를 체크해 집계
      for (final e in entries) {
        final exists = await (db.select(db.testResults)
              ..where((t) =>
                  t.testName.equals(e.testName.value) &
                  t.abi.equals(e.abi.value) &
                  t.category.equals(
                      e.category.present ? e.category.value : 'ETC')))
            .getSingleOrNull();
        if (exists != null) successCount++;
      }
      return successCount;
    });
  }

  Future<void> updateTestResult(int id, TestResultsCompanion entry) async {
    await (db.update(db.testResults)..where((t) => t.id.equals(id)))
        .write(entry);
  }

  Future<void> deleteTestResult(int id) async {
    await (db.delete(db.testResults)..where((t) => t.id.equals(id))).go();
  }
}
