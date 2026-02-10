import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:go_router/go_router.dart';
import '../data/news_firestore_repository.dart';
import '../domain/news_model.dart';

class NewsListScreen extends ConsumerWidget {
  const NewsListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // A방식 Provider 호출
    final newsAsync = ref.watch(newsListProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('News Archive')),
      body: newsAsync.when(
        data: (newsList) {
          if (newsList.isEmpty) {
            return const Center(child: Text('No news logs yet.'));
          }
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: newsList.length,
            separatorBuilder: (_, __) => const SizedBox(height: 12),
            itemBuilder: (context, index) {
              final news = newsList[index];
              return _NewsCard(news: news);
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error: $err')),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // 다이얼로그 호출
          showDialog(
            context: context,
            builder: (_) => _NewsAddDialog(ref: ref),
          );
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}

// 뉴스 카드 위젯
class _NewsCard extends StatelessWidget {
  final NewsLog news;
  const _NewsCard({required this.news});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        onTap: () => context.go('/news/detail', extra: news),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    DateFormat('yyyy-MM-dd').format(news.date),
                    style: TextStyle(color: Colors.grey[600], fontSize: 12),
                  ),
                  if (news.tags.isNotEmpty)
                    Row(
                      children: news.tags
                          .take(3)
                          .map((t) => Padding(
                                padding: const EdgeInsets.only(left: 4),
                                child: Text('#$t',
                                    style: const TextStyle(
                                        color: Colors.blue, fontSize: 12)),
                              ))
                          .toList(),
                    )
                ],
              ),
              const SizedBox(height: 8),
              Text(news.title,
                  style: const TextStyle(
                      fontWeight: FontWeight.bold, fontSize: 16)),
              const SizedBox(height: 4),
              Text(news.content, maxLines: 2, overflow: TextOverflow.ellipsis),
            ],
          ),
        ),
      ),
    );
  }
}

// 추가 다이얼로그 (Person 스타일처럼 파일 내부에 포함)
class _NewsAddDialog extends StatefulWidget {
  final WidgetRef ref;
  const _NewsAddDialog({required this.ref});

  @override
  State<_NewsAddDialog> createState() => _NewsAddDialogState();
}

class _NewsAddDialogState extends State<_NewsAddDialog> {
  final _titleCtrl = TextEditingController();
  final _contentCtrl = TextEditingController();
  final _tagCtrl = TextEditingController();
  DateTime _date = DateTime.now();
  final List<String> _tags = [];

  void _addTag() {
    final t = _tagCtrl.text.trim();
    if (t.isNotEmpty && !_tags.contains(t)) {
      setState(() {
        _tags.add(t);
        _tagCtrl.clear();
      });
    }
  }

  Future<void> _save() async {
    if (_titleCtrl.text.isEmpty) return;
    if (_tagCtrl.text.isNotEmpty) _addTag();

    final news = NewsLog(
      title: _titleCtrl.text,
      content: _contentCtrl.text,
      date: _date,
      tags: _tags,
    );

    try {
      // A방식 Provider 사용
      await widget.ref.read(newsRepositoryProvider).addNews(news);
      if (mounted) Navigator.pop(context);
    } catch (e) {
      debugPrint('Err: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Add News'),
      content: SizedBox(
        width: 400,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: _titleCtrl,
                decoration: const InputDecoration(labelText: 'Title'),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: _contentCtrl,
                decoration: const InputDecoration(labelText: 'Content'),
                maxLines: 3,
              ),
              const SizedBox(height: 8),
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('Date: ${DateFormat('yyyy-MM-dd').format(_date)}'),
                trailing: const Icon(Icons.calendar_month),
                onTap: () async {
                  final d = await showDatePicker(
                      context: context,
                      initialDate: _date,
                      firstDate: DateTime(2000),
                      lastDate: DateTime(2100));
                  if (d != null) setState(() => _date = d);
                },
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _tagCtrl,
                      decoration: const InputDecoration(
                          labelText: 'Tags (#)', hintText: 'Enter to add'),
                      onSubmitted: (_) => _addTag(),
                    ),
                  ),
                  IconButton(
                      onPressed: _addTag, icon: const Icon(Icons.add_circle)),
                ],
              ),
              Wrap(
                spacing: 8,
                children: _tags
                    .map((t) => Chip(
                          label: Text(t),
                          onDeleted: () => setState(() => _tags.remove(t)),
                        ))
                    .toList(),
              )
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel')),
        FilledButton(onPressed: _save, child: const Text('Save')),
      ],
    );
  }
}
