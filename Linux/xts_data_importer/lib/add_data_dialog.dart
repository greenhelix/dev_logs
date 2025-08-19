// lib/add_data_dialog.dart
import 'package:flutter/material.dart';
import 'package:drift/drift.dart' as drift;
import 'database.dart';

class AddDataDialog extends StatefulWidget {
  final AppDatabase db;
  const AddDataDialog({super.key, required this.db});

  @override
  State<AddDataDialog> createState() => _AddDataDialogState();
}

class _AddDataDialogState extends State<AddDataDialog> {
  final _formKey = GlobalKey<FormState>();
  final _controllers = {
    'category': TextEditingController(text: 'ETC'),
    'testDate': TextEditingController(text: DateTime.now().toIso8601String().substring(0, 10)),
    'abi': TextEditingController(text: 'arm64-v8a'),
    'module': TextEditingController(),
    'testName': TextEditingController(),
    'result': TextEditingController(text: 'pass'),
  };

  @override
  void dispose() {
    _controllers.forEach((key, value) => value.dispose());
    super.dispose();
  }

  Future<void> _submitForm() async {
    if (_formKey.currentState!.validate()) {
      try {
        await widget.db.into(widget.db.testResults).insert(
          TestResultsCompanion.insert(
            // 기본값이 있는 category는 Value()로 감쌉니다.
            category: drift.Value(_controllers['category']!.text),
            
            // 필수 항목들은 원본 타입 그대로 전달합니다.
            testDate: _controllers['testDate']!.text,
            abi: _controllers['abi']!.text,
            module: _controllers['module']!.text,
            testName: _controllers['testName']!.text,
            result: _controllers['result']!.text,
            
            // nullable 필드들은 비어있을 수 있으므로 Value()로 감싸야 합니다.
            // 여기서는 폼에 없으므로 기본값(null)으로 들어갑니다.
          ),
        );
        Navigator.of(context).pop(true); // 성공적으로 추가됨을 알림
      } catch (e) {
        Navigator.of(context).pop(); // 창을 닫고
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('데이터 추가 실패: $e')),
        );
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
                controller: _controllers['testName'],
                decoration: const InputDecoration(labelText: 'Test Name'),
                validator: (value) => value!.isEmpty ? '필수 항목입니다.' : null,
              ),
              TextFormField(
                controller: _controllers['module'],
                decoration: const InputDecoration(labelText: 'Module'),
                 validator: (value) => value!.isEmpty ? '필수 항목입니다.' : null,
              ),
              TextFormField(
                controller: _controllers['abi'],
                decoration: const InputDecoration(labelText: 'ABI'),
              ),
              TextFormField(
                controller: _controllers['category'],
                decoration: const InputDecoration(labelText: 'Category'),
              ),
               TextFormField(
                controller: _controllers['result'],
                decoration: const InputDecoration(labelText: 'Result'),
              ),
              TextFormField(
                controller: _controllers['testDate'],
                decoration: const InputDecoration(labelText: 'Test Date'),
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
