// lib/main.dart

import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:csv/csv.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:drift/drift.dart' as drift;
import 'database.dart';
import 'data_view_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'XTS Data Importer',
      theme: ThemeData(primarySwatch: Colors.blue, useMaterial3: true, colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue)),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _dbFileCount = 0;
  bool _isLoading = false;
  String _message = '버튼을 눌러 CSV 데이터 가져오기를 시작하세요.';

  @override
  void initState() {
    super.initState();
    _updateDbFileCount();
  }

  Future<void> _updateDbFileCount() async {
    final files = await findXtsDatabaseFiles();
    if (mounted) setState(() => _dbFileCount = files.length);
  }

  String _getCategoryFromModule(String module) {
    if (module.toLowerCase().startsWith('cts')) return 'CTS';
    if (module.toLowerCase().startsWith('gts')) return 'GTS';
    if (module.toLowerCase().startsWith('vts')) return 'VTS';
    return 'ETC';
  }

  Future<void> _importCsvData() async {
    if (_isLoading) return;

    try {
      if (Platform.isAndroid || Platform.isIOS) {
        var status = await Permission.storage.status;
        if (!status.isGranted) status = await Permission.storage.request();
        if (!status.isGranted) {
          if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('저장소 권한이 필요합니다.')));
          return;
        }
      }

      setState(() { _isLoading = true; _message = '파일 선택기를 여는 중...'; });

      FilePickerResult? result;
      try {
        result = await FilePicker.platform.pickFiles(type: FileType.any);
      } on PlatformException catch (e) {
        setState(() { _message = '파일 선택기를 열 수 없습니다.\nLinux에서는 \'zenity\'가 설치되어 있는지 확인하세요.'; });
        print('File Picker PlatformException: $e');
        return;
      }

      if (result == null) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('파일 선택이 취소되었습니다.')));
        setState(() => _message = '버튼을 눌러 CSV 데이터 가져오기를 시작하세요.');
        return;
      }

      if (result.files.single.extension?.toLowerCase() != 'csv') {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('오류: CSV 파일만 선택 가능합니다.')));
        setState(() => _isLoading = false);
        return;
      }

      setState(() { _message = '선택된 CSV 파일을 분석하고 있습니다...'; });
      
      final file = File(result.files.single.path!);
      final rawCsv = await file.readAsString();
      List<List<dynamic>> rows;
      try {
        // CsvToListConverter의 eol 파라미터를 제거하여 \n과 \r\n 모두 자동 처리하도록 함
        rows = const CsvToListConverter(shouldParseNumbers: false).convert(rawCsv);
      } catch (e) {
        throw Exception('CSV 파일 파싱 중 오류가 발생했습니다: $e');
      }

      if (rows.length < 2) throw Exception('유효한 데이터가 없는 파일입니다. (헤더 포함 최소 2줄 필요)');
      
      final bool hasCategoryRow = !(rows[0].any((cell) => cell.toString().toLowerCase() == 'test date'));
      final String suiteName = rows[0][0].toString();
      final String category = _getCategoryFromModule(suiteName);
      final String dbName = 'xts_${category.toLowerCase()}.sqlite';
      final AppDatabase importDb = AppDatabase(dbName: dbName);
      
      final dataRows = hasCategoryRow ? rows.sublist(2) : rows.sublist(1); // 카테고리 행 유무에 따라 데이터 시작 위치 조정

      setState(() => _message = '총 ${dataRows.length}개 행을 \'${dbName}\'에 저장합니다...');

      int successCount = 0;
      int failedCount = 0;
      final List<TestResultsCompanion> entriesToInsert = [];

      for (final row in dataRows) {
        if (row.length > 12) {
          failedCount++;
          // [핵심 수정 2] 문제의 행을 정확히 출력하여 디버깅 지원
          print("SKIPPING ROW: Column count is ${row.length}. Row data: $row");
          continue;
        }
        try {
          final entry = TestResultsCompanion.insert(
            category: drift.Value(category),
            testDate: row.toString(),
            abi: row[3].toString(),
            module: row[4].toString(),
            testName: row[5].toString(),
            result: row[6].toString(),
            detail: drift.Value(row[7]?.toString()),
            description: drift.Value(row[8]?.toString()),
            fwVersion: drift.Value(row[9]?.toString()),
            testToolVersion: drift.Value(row[10]?.toString()),
            securityPatch: drift.Value(row[11]?.toString()),
            sdkVersion: drift.Value(row[1]?.toString()),
          );
          entriesToInsert.add(entry);
        } catch (e) {
          failedCount++;
          print("Error processing row: $row, error: $e");
        }
      }

      if (entriesToInsert.isNotEmpty) {
        await importDb.batch((batch) {
          batch.insertAll(importDb.testResults, entriesToInsert, mode: drift.InsertMode.insertOrIgnore);
        });
        successCount = entriesToInsert.length;
      }

      await importDb.close();
      await _updateDbFileCount();

      String finalMessage = '$successCount개 데이터 처리 완료! (${dbName})';
      if (failedCount > 0) {
        finalMessage += '\n$failedCount개 행은 형식이 맞지 않아 건너뛰었습니다.';
      }
      setState(() => _message = finalMessage);

    } catch (e, s) {
      print('CSV 처리 중 심각한 오류 발생: $e\n$s');
      setState(() => _message = '오류 발생: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _navigateToViewPage() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const DataViewPage()),
    ).then((_) => _updateDbFileCount());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('XTS 데이터베이스 구축')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text('현재 생성된 데이터베이스 파일 수:', style: TextStyle(fontSize: 18)),
              const SizedBox(height: 10),
              Text('$_dbFileCount', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 40),
              if (_isLoading)
                const CircularProgressIndicator()
              else
                ElevatedButton.icon(
                  icon: const Icon(Icons.file_upload),
                  label: const Text('CSV 파일 가져오기'),
                  onPressed: _importCsvData,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 15),
                    textStyle: const TextStyle(fontSize: 16),
                  ),
                ),
              const SizedBox(height: 20),
              Text(_message, textAlign: TextAlign.center),
              const SizedBox(height: 50),
              OutlinedButton.icon(
                icon: const Icon(Icons.storage),
                label: const Text('저장된 데이터 조회하기'),
                onPressed: _navigateToViewPage,
                style: OutlinedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 15),
                  textStyle: const TextStyle(fontSize: 16),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
