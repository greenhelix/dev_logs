// lib/data_view_page.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'add_data_dialog.dart';
import 'view_edit_dialog.dart';
import 'providers.dart';

class DataViewPage extends ConsumerWidget {
  const DataViewPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final filteredResults = ref.watch(filteredResultsProvider);
    final asyncCategories = ref.watch(categoriesProvider);
    final selectedCategory = ref.watch(selectedCategoryProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text('View DB Data (${filteredResults.length})'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.invalidate(testResultsProvider),
            tooltip: 'Refresh',
          ),
        ],
      ),
      body: Column(
        children: [
          asyncCategories.when(
            data: (categories) => SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.all(8.0),
              child: Wrap(
                spacing: 8.0,
                children: ['All', ...categories].map((category) {
                  return FilterChip(
                    label: Text(category),
                    selected: selectedCategory == category,
                    onSelected: (selected) => ref.read(selectedCategoryProvider.notifier).set(category),
                  );
                }).toList(),
              ),
            ),
            loading: () => const Center(child: Padding(padding: EdgeInsets.all(8.0), child: CircularProgressIndicator())),
            error: (e, s) => Text('Failed to load categories: $e'),
          ),
          Expanded(
            child: filteredResults.isEmpty
                ? const Center(child: Text('No data to display.'))
                : ListView.builder(
                    itemCount: filteredResults.length,
                    itemBuilder: (context, index) {
                      final item = filteredResults[index];
                      return Card(
                        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        child: ListTile(
                          onTap: () => showDialog(
                            context: context,
                            builder: (context) => ViewEditDialog(item: item),
                          ),
                          leading: CircleAvatar(
                            backgroundColor: item.result.toLowerCase() == 'pass' ? Colors.green : Colors.red,
                            child: Text(item.category.substring(0, 1).toUpperCase()),
                          ),
                          title: Text(item.testName, style: const TextStyle(fontWeight: FontWeight.bold)),
                          subtitle: Text("${item.module} | ${item.result.toUpperCase()}"),
                          trailing: const Icon(Icons.chevron_right),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => showDialog(
          context: context,
          builder: (context) => const AddDataDialog(),
        ),
        tooltip: 'Add New Data',
        child: const Icon(Icons.add),
      ),
    );
  }
}
