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
        title: Text('DB 데이터 조회 (${filteredResults.length}개)'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.invalidate(testResultsProvider),
            tooltip: '새로고침',
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
                    onSelected: (selected) {
                      ref.read(selectedCategoryProvider.notifier).set(category);
                    },
                  );
                }).toList(),
              ),
            ),
            loading: () => const SizedBox.shrink(),
            error: (e, s) => Text('카테고리 로딩 실패: $e'),
          ),
          Expanded(
            child: filteredResults.isEmpty
                ? const Center(child: Text('표시할 데이터가 없습니다.'))
                : ListView.builder(
                    itemCount: filteredResults.length,
                    itemBuilder: (context, index) {
                      final item = filteredResults[index];
                      return Card(
                        margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        child: ListTile(
                          onTap: () async {
                            final changed = await showDialog<bool>(
                              context: context,
                              builder: (context) => ViewEditDialog(item: item),
                            );
                            if (changed == true) {
                              // 변경/삭제 후 즉시 재조회 유도
                              ref.invalidate(testResultsProvider);
                              ref.invalidate(categoriesProvider);
                            }
                          },
                          leading: CircleAvatar(
                            backgroundColor:
                                item.result.toLowerCase() == 'pass' ? Colors.green : Colors.red,
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
        tooltip: '새 데이터 추가',
        child: const Icon(Icons.add),
      ),
    );
  }
}
