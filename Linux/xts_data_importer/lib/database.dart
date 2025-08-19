// lib/database.dart

import 'dart:io';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

part 'database.g.dart';

@DataClassName('TestResult')
class TestResults extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get category => text().withDefault(const Constant('ETC'))();
  TextColumn get testDate => text().named('test_date')();
  TextColumn get module => text()();
  TextColumn get testName => text().named('test_name')();
  TextColumn get result => text()();
  TextColumn get detail => text().nullable()();
  TextColumn get description => text().nullable()();
  TextColumn get fwVersion => text().named('fw_version').nullable()();
  TextColumn get testToolVersion => text().named('test_tool_version').nullable()();
  TextColumn get securityPatch => text().named('security_patch').nullable()();
  TextColumn get sdkVersion => text().named('sdk_version').nullable()();
  TextColumn get abi => text().named('abi').nullable()();

  @override
  List<String> get customConstraints => ['UNIQUE(test_name, abi)'];
}

@DriftDatabase(tables: [TestResults])
class AppDatabase extends _$AppDatabase {
  AppDatabase({String dbName = 'xts_default.sqlite'}) : super(_openConnection(dbName));

  @override
  int get schemaVersion => 2; // 'category' 컬럼이 포함된 버전

  @override
  MigrationStrategy get migration {
    return MigrationStrategy(
      onCreate: (m) async {
        await m.createAll();
      },
      onUpgrade: (m, from, to) async {
        if (from < 2) {
          await m.addColumn(testResults, testResults.category);
        }
      },
    );
  }

  Future<List<TestResult>> getAllResults() => select(testResults).get();

  Future<List<String>> getCategories() async {
    final query = selectOnly(testResults, distinct: true)..addColumns([testResults.category]);
    return await query.map((row) => row.read(testResults.category)!).get();
  }
}

LazyDatabase _openConnection(String dbName) {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, dbName));
    print('DB File Path: ${file.path}');
    return NativeDatabase(file);
  });
}

// 생성된 모든 XTS 데이터베이스 파일 목록을 찾는 헬퍼 함수
Future<List<String>> findXtsDatabaseFiles() async {
  try {
    final dbFolder = await getApplicationDocumentsDirectory();
    final files = dbFolder.listSync();
    return files
        .where((f) => f is File && p.basename(f.path).startsWith('xts_') && p.basename(f.path).endsWith('.sqlite'))
        .map((f) => p.basename(f.path))
        .toList();
  } catch (e) {
    print("Error finding database files: $e");
    return [];
  }
}
