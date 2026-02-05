import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart'; // 날짜 포맷팅용
import 'package:go_router/go_router.dart';
import '../data/news_repository.dart';
import '../../../data/local/app_database.dart'; // NewsLog 타입 사용

// 뉴스 목록 데이터 제공자
final newsListProvider = FutureProvider.autoDispose<List<NewsLog>>((ref) async {
  return ref.watch(newsRepositoryProvider).getAllNews();
});

class NewsListScreen extends ConsumerWidget {
  const NewsListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final newsAsync = ref.watch(newsListProvider);

    return Scaffold(
      appBar: AppBar(title: const Text("News Archive")),
      body: newsAsync.when(
        data: (newsList) {
          if (newsList.isEmpty) {
            return const Center(child: Text("No news archived yet."));
          }
          return ListView.builder(
            itemCount: newsList.length,
            itemBuilder: (context, index) {
              final news = newsList[index];
              return Card(
                clipBehavior: Clip.antiAlias, //물결 효과가 카드 박으로 안나가게하는 부분
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: InkWell(
                  onTap: () {
                    context.push('/news/detail', extra: news);
                  },
                  child: Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // 1. 날짜 및 시간
                        Text(
                          DateFormat('yyyy-MM-dd HH:mm').format(news.timestamp),
                          style:
                              TextStyle(color: Colors.grey[600], fontSize: 12),
                        ),
                        const SizedBox(height: 8),
                        // 2. 제목
                        Text(
                          news.title,
                          style: const TextStyle(
                              fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 8),
                        // 3. 내용
                        Text(news.content),
                        // 4. 이미지 (있으면 표시)
                        if (news.imageUrl != null && news.imageUrl!.isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(top: 8.0),
                            child: Image.network(
                              news.imageUrl!,
                              height: 150,
                              width: double.infinity,
                              fit: BoxFit.cover,
                              errorBuilder: (context, error, stackTrace) =>
                                  const Text("Failed to load image"),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text("Error: $err")),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // 뉴스 추가 화면으로 이동 (아직 안 만듦)
          context.push('/news/add');
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
