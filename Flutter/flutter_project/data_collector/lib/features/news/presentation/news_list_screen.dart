import 'package:data_accumulator_app/core/widgets/custom_image_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../core/widgets/responsive_list_tile.dart';
import '../../../data/providers.dart';
import '../domain/news_model.dart';

class NewsListScreen extends ConsumerWidget {
  const NewsListScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final newsListAsync = ref.watch(newsStreamProvider);
    final dateFormat = DateFormat('yyyy-MM-dd HH:mm');

    return Scaffold(
      appBar: AppBar(title: const Text('News Log')),
      body: newsListAsync.when(
        data: (newsList) => ListView.builder(
          itemCount: newsList.length,
          itemBuilder: (context, index) {
            final news = newsList[index];
            return ResponsiveListTile(
              onEdit: () => _showAddOrEditDialog(context, ref, news: news),
              onDelete: () =>
                  ref.read(newsRepositoryProvider).deleteNews(news.id!),
              child: ListTile(
                title: Text(news.title,
                    maxLines: 1, overflow: TextOverflow.ellipsis),
                subtitle: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(news.content,
                        maxLines: 2, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 4),
                    Wrap(
                      spacing: 4,
                      children: [
                        Text(dateFormat.format(news.date),
                            style: const TextStyle(
                                fontSize: 12, color: Colors.grey)),
                        if (news.tags.isNotEmpty)
                          ...news.tags.take(3).map((t) => Container(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 4, vertical: 2),
                                decoration: BoxDecoration(
                                    color: Colors.blue[50],
                                    borderRadius: BorderRadius.circular(4)),
                                child: Text('#$t',
                                    style: TextStyle(
                                        fontSize: 10, color: Colors.blue[800])),
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
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error: $err')),
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
    final tagsCtrl = TextEditingController(text: news?.tags.join(', ') ?? '');

    // Date Picker Variable
    DateTime selectedDate = news?.date ?? DateTime.now();

    String? currentImageUrl = news?.imageUrl;

    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) {
          return AlertDialog(
            title: Text(isEdit ? 'News 수정' : 'News 추가'),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                      controller: titleCtrl,
                      decoration: const InputDecoration(labelText: 'Title')),
                  TextField(
                      controller: contentCtrl,
                      decoration: const InputDecoration(labelText: 'Content'),
                      maxLines: 5),
                  TextField(
                      controller: tagsCtrl,
                      decoration: const InputDecoration(
                          labelText: 'Tags (comma separated)')),
                  CustomImagePicker(
                    initialUrl: currentImageUrl,
                    onImageSelected: (url) {
                      currentImageUrl = url;
                    },
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      const Text("Date: "),
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
                        child:
                            Text(DateFormat('yyyy-MM-dd').format(selectedDate)),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('취소')),
              ElevatedButton(
                onPressed: () async {
                  final tagsList = tagsCtrl.text
                      .split(',')
                      .map((e) => e.trim())
                      .where((e) => e.isNotEmpty)
                      .toList();

                  final newNews = NewsLog(
                    id: isEdit ? news.id : null, // 추가 시 null, 수정 시 기존 ID
                    title: titleCtrl.text,
                    content: contentCtrl.text,
                    date: selectedDate,
                    tags: tagsList,
                    relatedPersonId:
                        news?.relatedPersonId, // 기존 값 유지 (또는 선택 UI 추가 가능)
                    imageUrl: currentImageUrl,
                  );

                  if (isEdit) {
                    await ref.read(newsRepositoryProvider).updateNews(newNews);
                  } else {
                    await ref.read(newsRepositoryProvider).addNews(newNews);
                  }
                  if (context.mounted) Navigator.pop(context);
                },
                child: Text(isEdit ? '수정' : '추가'),
              ),
            ],
          );
        },
      ),
    );
  }
}
