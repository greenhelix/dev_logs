// lib/home_page.dart
import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'data_view_page.dart';
import 'providers.dart';

class HomePage extends ConsumerStatefulWidget {
	const HomePage({super.key});

	@override
	ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
	bool _isLoading = false;
	String _message = '버튼을 눌러 CSV 데이터 가져오기를 시작하세요.';

	Future<void> _importCsvData() async {
		if (_isLoading) return;

		setState(() => _isLoading = true);

		try {
			final result = await FilePicker.platform.pickFiles(
			type: FileType.custom, allowedExtensions: ['csv']);

			if (result == null) {
				setState(() => _message = '파일 선택이 취소되었습니다.');
				return;
			}

			final file = File(result.files.single.path!);
			final category = await _promptForCategory(context, 'ETC');

			if (category == null) {
				setState(() => _message = '카테고리 입력이 취소되었습니다.');
				return;
			}

			setState(() => _message = "'$category' 카테고리로 데이터 가져오는 중...");

			final importService = ref.read(csvImportServiceProvider);
			final (success, failed) = await importService.importFromCsv(file, category);

			setState(() {
				_message = '$success개 데이터 처리 완료. (실패: $failed개)';
			});

		} catch (e) {
			setState(() => _message = '오류 발생: $e');
		} finally {
			setState(() => _isLoading = false);
		}
	}

	Future<String?> _promptForCategory(BuildContext context, String suggested) async {
		final controller = TextEditingController(text: suggested);
		return showDialog<String>(
			context: context,
			builder: (context) => AlertDialog(
				title: const Text('카테고리 입력'),
				content: TextFormField(controller: controller, autofocus: true),
				actions: [
					TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('취소')),
					ElevatedButton(onPressed: () => Navigator.of(context).pop(controller.text), child: const Text('확인')),
				],
			),
		);
	}

	@override
	Widget build(BuildContext context) {
		final dbFileCount = ref.watch(testResultsProvider).asData?.value.length ?? 0;

		return Scaffold(
			appBar: AppBar(title: const Text('XTS 데이터베이스 구축')),
			body: Center(
				child: Padding(
					padding: const EdgeInsets.all(16.0),
					child: Column(
						mainAxisAlignment: MainAxisAlignment.center,
						children: [
							const Text('저장된 총 데이터 수:', style: TextStyle(fontSize: 18)),
							const SizedBox(height: 10),
							Text('$dbFileCount', style: Theme.of(context).textTheme.headlineMedium),
							const SizedBox(height: 40),
							if (_isLoading)
							const CircularProgressIndicator()
							else
							ElevatedButton.icon(
								icon: const Icon(Icons.file_upload),
								label: const Text('CSV 파일 가져오기'),
								onPressed: _importCsvData,
							),
							const SizedBox(height: 20),
							Text(_message, textAlign: TextAlign.center),
							const SizedBox(height: 50),
							OutlinedButton.icon(
								icon: const Icon(Icons.storage),
								label: const Text('저장된 데이터 조회하기'),
								onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (context) => const DataViewPage())),
							),
						],
					),
				),
			),
		);
	}
}
