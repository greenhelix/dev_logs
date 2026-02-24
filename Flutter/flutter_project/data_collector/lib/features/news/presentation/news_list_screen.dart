import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/widgets/responsive_list_tile.dart';
import '../../../core/widgets/custom_image_picker.dart';
import '../../../core/widgets/tag_input_widget.dart';
import '../../../data/providers.dart';
import '../domain/news_model.dart';

class NewsListScreen extends ConsumerWidget {
  const NewsListScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final newsListAsync = ref.watch(newsStreamProvider);
    final dateFormat = DateFormat('yyyy-MM-dd HH:mm');

    return Scaffold(
      appBar: AppBar(
        title: const Text('News Log'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: '새로고침',
            onPressed: () => ref.invalidate(newsStreamProvider),
          ),
        ],
      ),
      body: newsListAsync.when(
        data: (newsList) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(newsStreamProvider);
            await Future.delayed(const Duration(milliseconds: 500));
          },
          child: newsList.isEmpty
              ? ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  children: const [
                    SizedBox(height: 200),
                    Center(child: Text('등록된 뉴스가 없습니다.', style: TextStyle(color: Colors.grey))),
                  ],
                )
              : ListView.builder(
                  physics: const AlwaysScrollableScrollPhysics(),
                  itemCount: newsList.length,
                  itemBuilder: (context, index) {
                    final news = newsList[index];
                    return ResponsiveListTile(
                      onEdit: () =>
                          _showAddOrEditDialog(context, ref, news: news),
                      onDelete: () async {
                        await ref
                            .read(newsRepositoryProvider)
                            .deleteNews(news.id!);
                        ref.invalidate(newsStreamProvider);
                      },
                      child: ListTile(
                        leading: news.imageUrl != null && news.imageUrl!.isNotEmpty
                            ? ClipRRect(
                                borderRadius: BorderRadius.circular(4),
                                child: Image.network(
                                  news.imageUrl!,
                                  width: 50,
                                  height: 50,
                                  fit: BoxFit.cover,
                                  errorBuilder: (context, error, stackTrace) => const Icon(Icons.broken_image),
                                ),
                              )
                            : const Icon(Icons.article),
                        title: Text(
                          news.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              news.content,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                            ),
                            const SizedBox(height: 4),
                            Wrap(
                              spacing: 4,
                              children: [
                                Text(
                                  dateFormat.format(news.date),
                                  style: const TextStyle(
                                      fontSize: 12, color: Colors.grey),
                                ),
                                if (news.tags.isNotEmpty)
                                  ...news.tags.take(3).map((t) => Container(
                                        padding: const EdgeInsets.symmetric(
                                            horizontal: 4, vertical: 2),
                                        decoration: BoxDecoration(
                                          color: Colors.blue[50],
                                          borderRadius: BorderRadius.circular(4),
                                        ),
                                        child: Text(
                                          '#$t',
                                          style: TextStyle(
                                              fontSize: 10,
                                              color: Colors.blue[800]),
                                        ),
                                      )),
                              ],
                            ),
                          ],
                        ),
                        onTap: () => context.push('/news/detail', extra: news),
                      ),
                    );
                  },
                ),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Error: $err'),
              ElevatedButton(
                onPressed: () => ref.invalidate(newsStreamProvider),
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddOrEditDialog(context, ref),
        child: const Icon(Icons.add),
      ),
    );
  }

  void _showAddOrEditDialog(BuildContext context, WidgetRef ref,
      {NewsLog? news}) {
    final isEdit = news != null;
    final titleCtrl = TextEditingController(text: news?.title ?? '');
    final contentCtrl = TextEditingController(text: news?.content ?? '');

    DateTime selectedDate = news?.date ?? DateTime.now();
    String? currentImageUrl = news?.imageUrl;
    List<String> currentTags = List.from(news?.tags ?? []);

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) {
          return AlertDialog(
            title: Text(isEdit ? 'News 수정' : 'News 추가'),
            content: SizedBox(
              width: double.maxFinite,
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Center(
                      child: CustomImagePicker(
                        initialUrl: currentImageUrl,
                        onImageSelected: (url) {
                          currentImageUrl = url;
                        },
                        isCircle: false,
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: titleCtrl,
                      decoration: const InputDecoration(labelText: 'Title'),
                    ),
                    TextField(
                      controller: contentCtrl,
                      decoration: const InputDecoration(labelText: 'Content'),
                      maxLines: 5,
                    ),
                    const SizedBox(height: 16),
                    TagInputWidget(
                      initialTags: currentTags,
                      onChanged: (newTags) {
                        currentTags = newTags;
                      },
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        const Icon(Icons.calendar_today,
                            size: 16, color: Colors.grey),
                        const SizedBox(width: 8),
                        TextButton(
                          onPressed: () async {
                            final picked = await showDatePicker(
                              context: context,
                              initialDate: selectedDate,
                              firstDate: DateTime(2000),
                              lastDate: DateTime(2100),
                            );
                            if (picked != null) {
                              setState(() => selectedDate = picked);
                            }
                          },
                          child: Text(
                              DateFormat('yyyy-MM-dd').format(selectedDate)),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('취소'),
              ),
              ElevatedButton(
                onPressed: () async {
                  if (titleCtrl.text.trim().isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('제목을 입력해주세요.')),
                    );
                    return;
                  }

                  final newNews = NewsLog(
                    id: isEdit ? news.id : null,
                    title: titleCtrl.text.trim(),
                    content: contentCtrl.text.trim(),
                    date: selectedDate,
                    tags: currentTags,
                    imageUrl: currentImageUrl,
                    relatedPersonId: news?.relatedPersonId,
                  );

                  if (isEdit) {
                    await ref.read(newsRepositoryProvider).updateNews(newNews);
                  } else {
                    await ref.read(newsRepositoryProvider).addNews(newNews);
                  }

                  ref.invalidate(newsStreamProvider);

                  if (context.mounted) Navigator.pop(context);
                },
                child: const Text('저장'),
              ),
            ],
          );
        },
      ),
    );
  }
}