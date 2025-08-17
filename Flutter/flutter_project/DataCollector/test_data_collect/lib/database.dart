// lib/database.dart
import 'package:drift/drift.dart';
import 'dart:io';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;

part 'database.g.dart';

// 테이블 정의 (변경 없음)
class TestResults extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get testDate => text()();
  TextColumn get abi => text()();
  TextColumn get module => text()();
  TextColumn get testName => text()();
  TextColumn get result => text()();
  TextColumn get detail => text().nullable()();
  TextColumn get description => text().nullable()();
  TextColumn get fwVersion => text()();
  TextColumn get testToolVersion => text()();
  TextColumn get securityPatch => text()();
  TextColumn get sdkVersion => text()();
}

// 데이터베이스 클래스 정의 (변경 없음)
@DriftDatabase(tables: [TestResults])
class MyDatabase extends _$MyDatabase {
  MyDatabase(QueryExecutor e) : super(e);

  @override
  int get schemaVersion => 1;

  Future<int> addTestResult(TestResultsCompanion entry) {
    return into(testResults).insert(entry);
  }

  Future<List<TestResult>> getAllResults() {
    return select(testResults).get();
  }
}

