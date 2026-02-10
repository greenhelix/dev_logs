// lib/features/news/presentation/news_detail_screen.dart

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../data/news_firestore_repository.dart';
import '../domain/news_model.dart';

class NewsDetailScreen extends ConsumerWidget {
  final NewsLog news;

  const NewsDetailScreen({
    super.key,
    required this.news,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // 날짜 포맷팅
    final dateStr = DateFormat('yyyy년 MM월 dd일 (E)').format(news.date);

    return Scaffold(
      appBar: AppBar(
        title: const Text('News Detail'),
        actions: [
          // 삭제 버튼
          IconButton(
            icon: const Icon(Icons.delete_outline),
            onPressed: () => _confirmDelete(context, ref),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 1. 날짜 표시
            Row(
              children: [
                const Icon(Icons.calendar_today, size: 16, color: Colors.grey),
                const SizedBox(width: 8),
                Text(
                  dateStr,
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                        color: Colors.grey[700],
                      ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // 2. 제목
            Text(
              news.title,
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 16),

            // 3. 태그 (Chip)
            if (news.tags.isNotEmpty)
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: news.tags.map((tag) {
                  return Chip(
                    label: Text('#$tag'),
                    backgroundColor:
                        Theme.of(context).colorScheme.surfaceContainerHighest,
                    labelStyle: TextStyle(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                      fontSize: 12,
                    ),
                    visualDensity: VisualDensity.compact,
                  );
                }).toList(),
              ),
            const SizedBox(height: 24),
            const Divider(),
            const SizedBox(height: 24),

            // 4. 본문 내용
            Text(
              news.content,
              style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                    height: 1.6, // 줄간격 확보
                  ),
            ),
          ],
        ),
      ),
      // (선택사항) 수정 버튼을 FloatingActionButton으로 둘 수도 있음
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // 수정 기능은 추후 구현 (Update Dialog 등)
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Edit feature coming soon!')),
          );
        },
        child: const Icon(Icons.edit),
      ),
    );
  }

  // 삭제 확인 다이얼로그
  Future<void> _confirmDelete(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete News'),
        content: const Text('Are you sure you want to delete this log?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Delete', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmed == true && context.mounted) {
      if (news.id != null) {
        await ref.read(newsRepositoryProvider).deleteNews(news.id!);
        if (context.mounted) context.pop(); // 리스트 화면으로 복귀
      }
    }
  }
}
