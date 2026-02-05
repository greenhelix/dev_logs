import 'package:drift/drift.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../../../data/local/app_database.dart';
import '../../../data/providers.dart';

// 1. Provider 정의
final newsRepositoryProvider = Provider<NewsRepository>((ref) {
  final db = ref.watch(databaseProvider);
  return NewsRepository(db);
});

// 2. Repository 클래스
class NewsRepository {
  final AppDatabase _db;
  NewsRepository(this._db);

  // 뉴스 저장
  Future<void> addNews({
    required String title,
    required String content,
    String? imageUrl,
  }) async {
    await _db.into(_db.newsLogs).insert(
          NewsLogsCompanion.insert(
            id: const Uuid().v4(),
            title: title,
            content: content,
            timestamp: DateTime.now(), // 현재 시간 자동 저장
            imageUrl: Value(imageUrl),
          ),
        );
  }

  // 뉴스 전체 조회 (최신순 정렬)
  Future<List<NewsLog>> getAllNews() async {
    return await (_db.select(_db.newsLogs)
          ..orderBy([
            (t) =>
                OrderingTerm(expression: t.timestamp, mode: OrderingMode.desc)
          ]))
        .get();
  }

  // 뉴스 삭제
  Future<void> deleteNews(String id) async {
    await (_db.delete(_db.newsLogs)..where((t) => t.id.equals(id))).go();
  }
}
