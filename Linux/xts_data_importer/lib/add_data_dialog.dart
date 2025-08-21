import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:drift/drift.dart' as drift;
import 'database.dart';
import 'providers.dart';

class AddDataDialog extends ConsumerStatefulWidget {
  const AddDataDialog({super.key});

  @override
  ConsumerState createState() => _AddDataDialogState();
}

class _AddDataDialogState extends ConsumerState<AddDataDialog> {
  final _formKey = GlobalKey<FormState>();
  final _controllers = {
    'category': TextEditingController(text: 'ETC'),
    'testDate': TextEditingController(text: DateTime.now().toIso8601String().substring(0, 10)),
    'module': TextEditingController(),
    'testName': TextEditingController(),
    'result': TextEditingController(text: 'pass'),
    'description': TextEditingController(),
    'abi': TextEditingController(text: 'arm64-v8a'),
  };

  @override
  void dispose() {
    _controllers.forEach((key, value) => value.dispose());
    super.dispose();
  }

  Future<void> _submitForm() async {
    if (_formKey.currentState!.validate()) {
      final repo = ref.read(databaseRepositoryProvider);
      final newEntry = TestResultsCompanion.insert(
        category: drift.Value(_controllers['category']!.text),
        testDate: _controllers['testDate']!.text,
        module: _controllers['module']!.text,
        testName: _controllers['testName']!.text,
        result: _controllers['result']!.text,
        abi: _controllers['abi']!.text,
        description: _controllers['description']!.text.isEmpty
            ? const drift.Value.absent()
            : drift.Value(_controllers['description']!.text),
      );
      try {
        await repo.addTestResult(newEntry);
        if (mounted) Navigator.of(context).pop();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text('데이터 추가 실패: $e')));
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('새 테스트 결과 추가'),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: _controllers['category'],
                decoration: const InputDecoration(labelText: 'Category'),
                validator: (value) => value!.isEmpty ? '카테고리 항목입니다.' : null,
              ),
              TextFormField(
                controller: _controllers['module'],
                decoration: const InputDecoration(labelText: 'Module'),
                validator: (value) => value!.isEmpty ? '필수 항목입니다.' : null,
              ),
              TextFormField(
                controller: _controllers['testName'],
                decoration: const InputDecoration(labelText: 'Test Name'),
                validator: (value) => value!.isEmpty ? '필수 항목입니다.' : null,
              ),
              TextFormField(
                controller: _controllers['result'],
                decoration: const InputDecoration(labelText: 'Result'),
                validator: (value) => value!.isEmpty ? '결과 항목입니다.' : null,
              ),
              TextFormField(
                controller: _controllers['description'],
                decoration: const InputDecoration(labelText: 'Description'),
              ),
              TextFormField(
                controller: _controllers['testDate'],
                decoration: const InputDecoration(labelText: 'Test Date'),
                validator: (value) => value!.isEmpty ? '날짜 항목입니다.' : null,
              ),
              TextFormField(
                controller: _controllers['abi'],
                decoration: const InputDecoration(labelText: 'ABI'),
                validator: (value) => value!.isEmpty ? '필수 항목입니다.' : null,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('취소')),
        ElevatedButton(onPressed: _submitForm, child: const Text('저장')),
      ],
    );
  }
}
