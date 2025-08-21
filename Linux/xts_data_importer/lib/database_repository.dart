// lib/database_repository.dart
import 'package:drift/drift.dart';
import 'database.dart';

class DatabaseRepository {
  final AppDatabase db;
  DatabaseRepository(this.db);

  Stream<List<TestResult>> watchAllResults() {
    print('[Repository] Watching all results...');
    return (db.select(db.testResults)
          ..orderBy([
            (t) => OrderingTerm(expression: t.testDate, mode: OrderingMode.desc)
          ]))
        .watch();
  }

  Future<List<String>> getCategories() async {
    print('[Repository] Fetching distinct categories...');
    final query = db.selectOnly(db.testResults, distinct: true)
      ..addColumns([db.testResults.category]);
    final result = await query.map((row) => row.read(db.testResults.category)!).get();
    result.sort();
    print('[Repository] Found ${result.length} categories.');
    return result;
  }

  Future<void> addTestResult(TestResultsCompanion entry) async {
    print('[Repository] Adding single test result: ${entry.testName.value}');
    await db.into(db.testResults).insert(entry);
    print('[Repository] Single entry added successfully.');
  }

  Future<int> bulkInsert(List<TestResultsCompanion> entries) async {
    print('[Repository] Starting bulk insert of ${entries.length} entries...');
    await db.transaction(() async {
      await db.batch((batch) {
        batch.insertAll(
          db.testResults,
          entries,
          mode: InsertMode.insertOrIgnore,
        );
      });
    });
    print('[Repository] Bulk insert transaction complete.');
    // This is an approximation. The actual number of inserted rows might be less due to conflicts.
    return entries.length;
  }

  Future<void> updateTestResult(int id, TestResultsCompanion entry) async {
    print('[Repository] Updating test result with id: $id');
    await (db.update(db.testResults)..where((t) => t.id.equals(id)))
        .write(entry);
    print('[Repository] Update complete for id: $id');
  }

  Future<void> deleteTestResult(int id) async {
    print('[Repository] Deleting test result with id: $id');
    await (db.delete(db.testResults)..where((t) => t.id.equals(id))).go();
    print('[Repository] Deletion complete for id: $id');
  }
}
