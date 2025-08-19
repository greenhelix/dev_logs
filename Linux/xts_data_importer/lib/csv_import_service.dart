import 'dart:io';
import 'package:csv/csv.dart';
import 'package:drift/drift.dart' as drift;
import 'database.dart';
import 'database_repository.dart';

// CSV 임포트 관련 비즈니스 로직을 담당합니다.

class CsvImportService {
	final DatabaseRepository _repo;
	CsvImportService(this._repo);

	Future<(int success, int failed) > importFromCsv(File file, String category) async {
		final rawCsv = await file.readAsString();
		final rows = const CsvToListConverter(shouldParseNumbers: false, eol: '\n').convert(rawCsv);

		if (rows.length < 2) {
			throw Exception('유효한 데이터가 없는 파일입니다. (헤더 포함 최소 2줄 필요)');
		}

		final header = rows[0].map((cell) => cell.toString().trim().toLowerCase().replaceAll(RegExp(r'[\s/]+'), '_')).toList();

		final columnIndex = {
			'test_date': header.indexOf('test_date'),
			'module': header.indexOf('module'),
			'test_name': header.indexOf('test'),
			'result': header.indexOf('result'),
			'detail': header.indexOf('detail'),
			'description': header.indexOf('description'),
			'fw_version': header.indexOf('fw_ver'),
			'test_tool_version': header.indexOf('test_tool_ver'),
			'security_patch': header.indexOf('security_patch'),
			'sdk_version': header.indexOf('sdk_ver'),
			'abi': header.indexOf('abi'),
		};

		final missingCols = columnIndex.entries.where((e) => ['test_date', 'module', 'result'].contains(e.key) && e.value == -1).map((e) => e.key).toList();

		if (missingCols.isNotEmpty) { 
			throw Exception('필수 컬럼 없음: ${missingCols.join(', ')}');
		}

		int failedCount = 0;
		final List<TestResultsCompanion> entriesToInsert = [];
		final dataRows = rows.sublist(1);

		String? safeValue(List<dynamic> row, int? index) {
			if (index == null || index < 0 || index >= row.length || row[index] == null) return null;
			final value = row[index].toString().trim();
			return value.isEmpty ? null : value;
		}

		for (final row in dataRows) {
			final testName = safeValue(row, columnIndex['test_name']);
			final abi = safeValue(row, columnIndex['abi']);

			if (testName == null || abi == null) {
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

		final successCount = await _repo.bulkInsert(entriesToInsert);
		return (successCount, failedCount);
	}
}
