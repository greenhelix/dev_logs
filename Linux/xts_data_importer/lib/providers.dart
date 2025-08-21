import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'database.dart';
import 'database_repository.dart';
import 'csv_import_service.dart';

part 'providers.g.dart';

@Riverpod(keepAlive: true)
AppDatabase appDatabase(AppDatabaseRef ref) {
  // AppDatabase를 앱 전체에서 하나만 유지
  return AppDatabase();
}

@Riverpod(keepAlive: true)
DatabaseRepository databaseRepository(DatabaseRepositoryRef ref) {
  return DatabaseRepository(ref.watch(appDatabaseProvider));
}

@Riverpod(keepAlive: true)
CsvImportService csvImportService(CsvImportServiceRef ref) {
  return CsvImportService(ref.watch(databaseRepositoryProvider));
}

@riverpod
Stream<List<TestResult>> testResults(TestResultsRef ref) {
  return ref.watch(databaseRepositoryProvider).watchAllResults();
}

@riverpod
Future<List<String>> categories(CategoriesRef ref) {
  return ref.watch(databaseRepositoryProvider).getCategories();
}

@riverpod
class SelectedCategory extends _$SelectedCategory {
  @override
  String build() => 'All';
  void set(String category) {
    state = category;
  }
}

@riverpod
List<TestResult> filteredResults(FilteredResultsRef ref) {
  final allResults = ref.watch(testResultsProvider).asData?.value ?? [];
  final selectedCategory = ref.watch(selectedCategoryProvider);
  if (selectedCategory == 'All') {
    return allResults;
  }
  return allResults.where((r) => r.category == selectedCategory).toList();
}
