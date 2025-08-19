// lib/data_view_page.dart

import 'package:flutter/material.dart';
import 'database.dart';
import 'add_data_dialog.dart';

class DataViewPage extends StatefulWidget {
  const DataViewPage({super.key});

  @override
  State<DataViewPage> createState() => _DataViewPageState();
}

class _DataViewPageState extends State<DataViewPage> {
  List<TestResult> _allResults = [];
  List<TestResult> _filteredResults = [];
  List<String> _categories = [];
  String? _selectedCategory;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _refreshData();
  }

  Future<void> _refreshData() async {
    setState(() => _isLoading = true);

    final List<TestResult> allData = [];
    final Set<String> allCategories = {};

    final dbFiles = await findXtsDatabaseFiles();
    for (final dbName in dbFiles) {
      final db = AppDatabase(dbName: dbName);
      final results = await db.getAllResults();
      allData.addAll(results);
      
      final categoriesInDb = await db.getCategories();
      allCategories.addAll(categoriesInDb);
      
      await db.close();
    }
    
    // Sort data for consistent view, for example by date
    allData.sort((a, b) => b.testDate.compareTo(a.testDate));

    setState(() {
      _allResults = allData;
      // Sort categories alphabetically
      var sortedCategories = allCategories.toList()..sort();
      _categories = ['All', ...sortedCategories];
      _filterResults();
      _isLoading = false;
    });
  }

  void _filterResults() {
    if (_selectedCategory == null || _selectedCategory == 'All') {
      _filteredResults = List.from(_allResults);
    } else {
      _filteredResults = _allResults.where((r) => r.category == _selectedCategory).toList();
    }
  }

  Future<void> _showAddDataDialog() async {
    final AppDatabase defaultDb = AppDatabase(dbName: 'xts_etc.sqlite');
    
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AddDataDialog(db: defaultDb),
    );
    
    await defaultDb.close();

    if (result == true) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('데이터가 성공적으로 추가되었습니다.')),
        );
      }
      await _refreshData();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('DB 데이터 조회 (${_filteredResults.length}개)'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _refreshData,
            tooltip: '새로고침',
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.all(8.0),
                  child: Wrap(
                    spacing: 8.0,
                    children: _categories.map((category) {
                      return FilterChip(
                        label: Text(category),
                        selected: _selectedCategory == category,
                        onSelected: (selected) {
                          setState(() {
                            _selectedCategory = selected ? category : null;
                            _filterResults();
                          });
                        },
                      );
                    }).toList(),
                  ),
                ),
                Expanded(
                  child: _filteredResults.isEmpty
                      ? const Center(child: Text('표시할 데이터가 없습니다.'))
                      : ListView.builder(
                          itemCount: _filteredResults.length,
                          itemBuilder: (context, index) {
                            final item = _filteredResults[index];
                            return Card(
                              margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                              child: ListTile(
                                leading: CircleAvatar(
                                  backgroundColor: item.result.toLowerCase() == 'pass' ? Colors.green : Colors.red,
                                  child: Text(item.category.substring(0, 1)),
                                ),
                                title: Text(item.testName, style: const TextStyle(fontWeight: FontWeight.bold)),
                                subtitle: Text("${item.module} | ${item.result.toUpperCase()}"),
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddDataDialog,
        tooltip: '새 데이터 추가',
        child: const Icon(Icons.add),
      ),
    );
  }
}
