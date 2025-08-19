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
		return result..sort();
	}

	Future<void> addTestResult(TestResultsCompanion entry) async {
		await db.into(db.testResults).insert(entry);
	}

	Future<int> bulkInsert(List<TestResultsCompanion> entries) async {
		int successCount = 0;
		await db.batch((batch) {
			batch.insertAll(db.testResults, entries, mode: InsertMode.insertOrIgnore);
		});
		// batch insert는 영향을 받은 row 수를 정확히 알기 어려우므로,
		// UNIQUE 제약조건 충돌을 제외한 삽입 시도 갯수를 성공으로 간주합니다.
		successCount = entries.length;
		return successCount;
	}
}
