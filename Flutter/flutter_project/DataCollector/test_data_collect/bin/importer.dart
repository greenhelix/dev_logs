// bin/importer.dart
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:test_data_collect/database.dart'; // 프로젝트 이름에 맞게 수정
import 'package:path/path.dart' as p;
import 'dart:io';

void main() async {
  // macOS 환경에서도 동일하게 동작합니다.
  final dbPath = p.join(Directory.current.path, 'test_results.sqlite');
  final db = MyDatabase(NativeDatabase(File(dbPath)));

  print('macOS에서 데이터베이스에 새로운 테스트 결과를 추가합니다...');

  final newEntry = TestResultsCompanion.insert(
    testDate: '2025-05-07',
    abi: 'arm64-v8a',
    module: 'CtsLibcoreTestCases',
    testName: 'libcore.java.text.DateFormatTest#testFormat_forBug266731719',
    result: 'pass',
    description: Value('retry >> PASS on macOS'),
    fwVersion: '204_A',
    testToolVersion: '14_r7 / 13033356',
    securityPatch: '2025-03-01',
    sdkVersion: '1.2.0',
  );

  final id = await db.addTestResult(newEntry);
  print('성공적으로 데이터를 추가했습니다. ID: $id');

  print('\n--- 현재까지 저장된 모든 데이터 ---');
  final allResults = await db.getAllResults();
  allResults.forEach((row) {
    print(row);
  });

  await db.close();
}

