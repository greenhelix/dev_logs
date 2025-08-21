// lib/view_edit_dialog.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:drift/drift.dart' as drift;
import 'database.dart';
import 'providers.dart';

class ViewEditDialog extends ConsumerStatefulWidget {
  final TestResult item;
  const ViewEditDialog({super.key, required this.item});

  @override
  ConsumerState<ViewEditDialog> createState() => _ViewEditDialogState();
}

class _ViewEditDialogState extends ConsumerState<ViewEditDialog> {
  final _formKey = GlobalKey<FormState>();
  late final Map<String, TextEditingController> _controllers;

  @override
  void initState() {
    super.initState();
    _controllers = {
      'category': TextEditingController(text: widget.item.category),
      'testDate': TextEditingController(text: widget.item.testDate),
      'module': TextEditingController(text: widget.item.module),
      'testName': TextEditingController(text: widget.item.testName),
      'result': TextEditingController(text: widget.item.result),
      'description': TextEditingController(text: widget.item.description ?? ''),
      'abi': TextEditingController(text: widget.item.abi),
    };
  }

  @override
  void dispose() {
    _controllers.forEach((_, controller) => controller.dispose());
    super.dispose();
  }

  Future<void> _submitUpdate() async {
    if (_formKey.currentState!.validate()) {
      final updatedEntry = TestResultsCompanion(
        category: drift.Value(_controllers['category']!.text),
        testDate: drift.Value(_controllers['testDate']!.text),
        module: drift.Value(_controllers['module']!.text),
        testName: drift.Value(_controllers['testName']!.text),
        result: drift.Value(_controllers['result']!.text),
        description: drift.Value(_controllers['description']!.text),
        abi: drift.Value(_controllers['abi']!.text),
      );

      try {
        await ref.read(databaseRepositoryProvider).updateTestResult(widget.item.id, updatedEntry);
        if (mounted) Navigator.of(context).pop(true); // Notify that a change was made
      } catch (e) {
        if(mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Update failed: $e')));
      }
    }
  }

  Future<void> _submitDelete() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Confirm Deletion'),
        content: const Text('Are you sure you want to delete this item?'),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.of(context).pop(true), child: const Text('Delete'), style: TextButton.styleFrom(foregroundColor: Colors.red)),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        await ref.read(databaseRepositoryProvider).deleteTestResult(widget.item.id);
        if (mounted) Navigator.of(context).pop(true); // Notify that a change was made
      } catch (e) {
        if(mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Deletion failed: $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('View/Edit Data'),
      content: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: _controllers.entries.map((entry) {
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 4.0),
                child: TextFormField(
                  controller: entry.value,
                  decoration: InputDecoration(
                    labelText: entry.key[0].toUpperCase() + entry.key.substring(1),
                    border: const OutlineInputBorder(),
                  ),
                  validator: (value) => value!.isEmpty ? 'This field is required.' : null,
                ),
              );
            }).toList(),
          ),
        ),
      ),
      actions: [
        IconButton(onPressed: _submitDelete, icon: const Icon(Icons.delete), color: Colors.red),
        const Spacer(),
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancel')),
        ElevatedButton(onPressed: _submitUpdate, child: const Text('Save')),
      ],
    );
  }
}
