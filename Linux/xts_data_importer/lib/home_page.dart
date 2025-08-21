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
  String _message = 'Press the button to start importing CSV data.';

  Future<void> _importCsvData() async {
    if (_isLoading) return;
    setState(() => _isLoading = true);

    try {
      final result = await FilePicker.platform.pickFiles(type: FileType.any);
      if (result == null) {
        setState(() => _message = 'File selection was canceled.');
        return;
      }

      final file = File(result.files.single.path!);
      final suggestedCategory = result.files.single.name.split('.').first.toUpperCase();
      final category = await _promptForCategory(context, suggestedCategory);

      if (category == null || category.trim().isEmpty) {
        setState(() => _message = 'Category input was canceled.');
        return;
      }

      setState(() => _message = "Importing data for category '$category'...");
      final importService = ref.read(csvImportServiceProvider);
      final (success, failed) = await importService.importFromCsv(file, category);
      
      ref.invalidate(testResultsProvider);
      ref.invalidate(categoriesProvider);

      setState(() => _message = 'Import complete: $success succeeded, $failed failed');
    } catch (e) {
      setState(() => _message = 'An error occurred: $e');
    } finally {
      if(mounted) setState(() => _isLoading = false);
    }
  }

  Future<String?> _promptForCategory(BuildContext context, String suggested) async {
    final controller = TextEditingController(text: suggested);
    return showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Enter Category'),
        content: TextFormField(controller: controller, autofocus: true, decoration: const InputDecoration(hintText: 'e.g., CTS, GTS...')),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancel')),
          ElevatedButton(onPressed: () => Navigator.of(context).pop(controller.text), child: const Text('OK')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final totalCount = ref.watch(testResultsProvider).asData?.value.length ?? 0;

    return Scaffold(
      appBar: AppBar(title: const Text('XTS Database Setup')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text('Total number of stored data entries:', style: TextStyle(fontSize: 18)),
              const SizedBox(height: 10),
              Text('$totalCount', style: Theme.of(context).textTheme.headlineMedium),
              const SizedBox(height: 40),
              if (_isLoading)
                const CircularProgressIndicator()
              else
                ElevatedButton.icon(
                  icon: const Icon(Icons.file_upload),
                  label: const Text('Import CSV File'),
                  onPressed: _importCsvData,
                  style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12)),
                ),
              const SizedBox(height: 20),
              Text(_message, textAlign: TextAlign.center),
              const SizedBox(height: 50),
              OutlinedButton.icon(
                icon: const Icon(Icons.storage),
                label: const Text('View Stored Data'),
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => const DataViewPage()),
                ),
                style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
