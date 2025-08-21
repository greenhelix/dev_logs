// lib/csv_import_service.dart
import 'dart:io';
import 'package:csv/csv.dart';
import 'package:drift/drift.dart' as drift;
import 'database.dart';
import 'database_repository.dart';

class CsvImportService {
  final DatabaseRepository _repo;
  CsvImportService(this._repo);

  Future<(int success, int failed)> importFromCsv(File file, String category) async {
    print('[CsvImportService] Starting import for category \'$category\' from file: ${file.path}');
    
    final rawCsv = await file.readAsString();
    print('[CsvImportService] File read successfully. Parsing CSV content...');
    
    final rows = const CsvToListConverter(shouldParseNumbers: false, eol: '\n').convert(rawCsv);
    print('[CsvImportService] Parsed ${rows.length} total rows.');

    if (rows.length < 2) {
      throw Exception('Invalid data file. (Requires at least 2 rows including header)');
    }

    final header = rows[0].map((cell) => cell.toString().trim().toLowerCase().replaceAll(RegExp(r'[\s/]+'), '_')).toList();
    print('[CsvImportService] Processed header: $header');

    final columnIndex = {
      'test_date': header.indexOf('test_date'),
      'module': header.indexOf('module'),
      'test_name': header.indexOf('test'),
      'result': header.indexOf('result'),
      'detail': header.indexOf('detail'),
      'description': header.indexOf('desc'),
      'fw_version': header.indexOf('f_w_ver'),
      'test_tool_version': header.indexOf('test_tool_ver'),
      'security_patch': header.indexOf('securitypatch'),
      'sdk_version': header.indexOf('sdk_ver'),
      'abi': header.indexOf('abi'),
    };
    print('[CsvImportService] Column index map: $columnIndex');
    
    int failedCount = 0;
    final List<TestResultsCompanion> entriesToInsert = [];
    final dataRows = rows.sublist(1);
    print('[CsvImportService] Processing ${dataRows.length} data rows...');

    String? safeValue(List row, int? index) {
      if (index == null || index < 0 || index >= row.length || row[index] == null) return null;
      final value = row[index].toString().trim();
      return value.isEmpty ? null : value;
    }

    for (var i = 0; i < dataRows.length; i++) {
      final row = dataRows[i];
      final testName = safeValue(row, columnIndex['test_name']);
      final abi = safeValue(row, columnIndex['abi']);

      if (testName == null || abi == null) {
        print('[CsvImportService] Skipping row ${i+1}: Missing essential data (testName or abi).');
        failedCount++;
        continue;
      }

      entriesToInsert.add(TestResultsCompanion.insert(
        category: drift.Value(category),
        testDate: safeValue(row, columnIndex['test_date']) ?? 'N/A',
        module: safeValue(row, columnIndex['module']) ?? 'N/A',
        testName: testName,
        result: safeValue(row, columnIndex['result']) ?? 'N/A',
        detail: drift.Value(safeValue(row, columnIndex['detail'])),
        description: drift.Value(safeValue(row, columnIndex['description'])),
        fwVersion: drift.Value(safeValue(row, columnIndex['fw_version'])),
        testToolVersion: drift.Value(safeValue(row, columnIndex['test_tool_version'])),
        securityPatch: drift.Value(safeValue(row, columnIndex['security_patch'])),
        sdkVersion: drift.Value(safeValue(row, columnIndex['sdk_version'])),
        abi: abi,
      ));
    }

    print('[CsvImportService] Processed all rows. ${entriesToInsert.length} entries are ready for insertion.');
    final successCount = await _repo.bulkInsert(entriesToInsert);
    print('[CsvImportService] Import finished. Success: $successCount, Failed: $failedCount');
    return (successCount, failedCount);
  }
}
