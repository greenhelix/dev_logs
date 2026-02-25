import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/widgets/responsive_list_tile.dart';
import '../../../core/widgets/custom_image_picker.dart';
import '../../../core/widgets/tag_input_widget.dart';
import '../../../data/providers.dart';
import '../domain/news_model.dart';

// Utility to auto-extract top keywords from content
List<String> _extractAutoTags(String text) {
  // Common stop words to ignore
  const stopwords = {
    '은', '는', '이', '가', '을', '를', '의', '에', '에서', '로', '으로',
    '도', '만', '과', '와', '하다', '있다', '없다', '되다', '하고', '이고',
    '그', '저', '것', '수', '및', '등', '더', '또', '이번', '지난',
    'the', 'a', 'an', 'is', 'are', 'was', 'were', 'in', 'on', 'at',
    'to', 'for', 'of', 'and', 'or', 'but', 'with', 'that', 'this',
  };

  // Clean special characters and split by space
  final cleaned = text.replaceAll(RegExp(r'[^\w\s가-힣]'), ' ');
  final words = cleaned
      .split(RegExp(r'\s+'))
      .map((w) => w.trim().toLowerCase())
      .where((w) => w.length > 1 && !stopwords.contains(w))
      .toList();

  // Count frequencies
  final freq = <String, int>{};
  for (final w in words) {
    freq[w] = (freq[w] ?? 0) + 1;
  }

  // Sort and return top 4
  final sorted = freq.entries.toList()
    ..sort((a, b) => b.value.compareTo(a.value));

  return sorted.take(4).map((e) => e.key).toList();
}

class NewsListScreen extends ConsumerStatefulWidget {
  const NewsListScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<NewsListScreen> createState() => _NewsListScreenState();
}

class _NewsListScreenState extends ConsumerState<NewsListScreen> {
  // State for timeline sorting toggle (latest vs oldest)
  bool _isLatestFirst = true;

  @override
  Widget build(BuildContext context) {
    final newsListAsync = ref.watch(newsStreamProvider);
    final dateFormat = DateFormat('yyyy-MM-dd HH:mm');

    return Scaffold(
      appBar: AppBar(
        title: const Text('News Log'),
        actions: [
          // Sort toggle button
          IconButton(
            icon: Icon(_isLatestFirst ? Icons.arrow_downward : Icons.arrow_upward),
            tooltip: _isLatestFirst ? '오래된 순 보기' : '최신 순 보기',
            onPressed: () => setState(() => _isLatestFirst = !_isLatestFirst),
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: '새로고침',
            onPressed: () => ref.invalidate(newsStreamProvider),
          ),
        ],
      ),
      body: newsListAsync.when(
        data: (newsList) {
          // Sort the list based on current state
          final sortedList = List<NewsLog>.from(newsList);
          sortedList.sort((a, b) => _isLatestFirst
              ? b.date.compareTo(a.date)
              : a.date.compareTo(b.date));

          return RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(newsStreamProvider);
              await Future.delayed(const Duration(milliseconds: 500));
            },
            child: sortedList.isEmpty
                ? ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    children: const [
                      SizedBox(height: 200),
                      Center(
                          child: Text('등록된 뉴스가 없습니다.',
                              style: TextStyle(color: Colors.grey))),
                    ],
                  )
                : ListView.builder(
                    physics: const AlwaysScrollableScrollPhysics(),
                    itemCount: sortedList.length,
                    itemBuilder: (context, index) {
                      final news = sortedList[index];
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
                          leading: news.imageUrl != null &&
                                  news.imageUrl!.isNotEmpty
                              ? ClipRRect(
                                  borderRadius: BorderRadius.circular(4),
                                  child: Image.network(
                                    news.imageUrl!,
                                    width: 50,
                                    height: 50,
                                    fit: BoxFit.cover,
                                    errorBuilder: (_, __, ___) =>
                                        const Icon(Icons.broken_image),
                                  ),
                                )
                              : const Icon(Icons.article),
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
                                  // Timeline date
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                        horizontal: 4, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: Colors.grey[200],
                                      borderRadius: BorderRadius.circular(4),
                                    ),
                                    child: Text(dateFormat.format(news.date),
                                        style: const TextStyle(
                                            fontSize: 10, color: Colors.black87)),
                                  ),
                                  // Display Tags
                                  if (news.tags.isNotEmpty)
                                    ...news.tags.take(3).map((t) => Container(
                                          padding: const EdgeInsets.symmetric(
                                              horizontal: 4, vertical: 2),
                                          decoration: BoxDecoration(
                                            color: Colors.blue[50],
                                            borderRadius:
                                                BorderRadius.circular(4),
                                          ),
                                          child: Text('#$t',
                                              style: TextStyle(
                                                  fontSize: 10,
                                                  color: Colors.blue[800])),
                                        )),
                                  // 기사 링크가 있다는 표시만 작은 아이콘으로 (클릭 불가, 상세화면 유도)
                                  if (news.sourceLinks.isNotEmpty)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 2, left: 4),
                                      child: Icon(Icons.link, size: 14, color: Colors.blue[400]),
                                    ),
                                ],
                              ),
                            ],
                          ),
                          onTap: () => context.push('/news/detail', extra: news),
                        ),
                      );
                    },
                  ),
          );
        },
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
    
    // Controllers for new link input
    final linkTitleCtrl = TextEditingController();
    final linkUrlCtrl = TextEditingController();

    DateTime selectedDate = news?.date ?? DateTime.now();
    String? currentImageUrl = news?.imageUrl;
    List<String> currentTags = List.from(news?.tags ?? []);
    List<Map<String, String>> currentSourceLinks = List.from(news?.sourceLinks ?? []);

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
                    // 1. Gallery style image picker
                    CustomImagePicker(
                      initialUrl: currentImageUrl,
                      onImageSelected: (url) {
                        currentImageUrl = url;
                      },
                      isCircle: false,
                    ),
                    const SizedBox(height: 16),

                    // 2. Basic Info
                    TextField(
                      controller: titleCtrl,
                      decoration: const InputDecoration(
                          labelText: 'Title', border: OutlineInputBorder()),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: contentCtrl,
                      decoration: const InputDecoration(
                          labelText: 'Content', border: OutlineInputBorder()),
                      maxLines: 5,
                    ),
                    const SizedBox(height: 4),

                    // Auto Tag Extraction Button
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton.icon(
                        icon: const Icon(Icons.auto_awesome, size: 16),
                        label: const Text('내용에서 자동 태그 추출',
                            style: TextStyle(fontSize: 12)),
                        onPressed: () {
                          final extracted = _extractAutoTags(contentCtrl.text);
                          if (extracted.isEmpty) return;
                          setState(() {
                            // Merge and remove duplicates
                            currentTags = {...currentTags, ...extracted}.toList();
                          });
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                                content: Text(
                                    '태그 추출 완료: ${extracted.join(', ')}')),
                          );
                        },
                      ),
                    ),

                    // 3. Tags Input
                    TagInputWidget(
                      initialTags: currentTags,
                      onChanged: (newTags) {
                        currentTags = newTags;
                      },
                    ),
                    const SizedBox(height: 16),

                    // 4. Source Links (Title + URL)
                    const Text('관련 기사 링크',
                        style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          flex: 1,
                          child: TextField(
                            controller: linkTitleCtrl,
                            decoration: const InputDecoration(
                              labelText: '제목 (선택)',
                              border: OutlineInputBorder(),
                              isDense: true,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          flex: 2,
                          child: TextField(
                            controller: linkUrlCtrl,
                            decoration: const InputDecoration(
                              labelText: 'https://...',
                              border: OutlineInputBorder(),
                              isDense: true,
                            ),
                            keyboardType: TextInputType.url,
                          ),
                        ),
                        IconButton(
                          onPressed: () {
                            if (linkUrlCtrl.text.trim().isNotEmpty) {
                              setState(() {
                                // If title is empty, use URL as fallback
                                final t = linkTitleCtrl.text.trim().isEmpty 
                                    ? linkUrlCtrl.text.trim() 
                                    : linkTitleCtrl.text.trim();
                                currentSourceLinks.add({
                                  'title': t,
                                  'url': linkUrlCtrl.text.trim()
                                });
                                linkTitleCtrl.clear();
                                linkUrlCtrl.clear();
                              });
                            }
                          },
                          icon: const Icon(Icons.add_circle_outline),
                        ),
                      ],
                    ),
                    
                    // Display added links
                    if (currentSourceLinks.isNotEmpty)
                      ...currentSourceLinks.asMap().entries.map((entry) =>
                          Padding(
                            padding: const EdgeInsets.only(top: 8),
                            child: Row(
                              children: [
                                const Icon(Icons.link, size: 14, color: Colors.blue),
                                const SizedBox(width: 4),
                                Expanded(
                                  child: Text(
                                    entry.value['title']!,
                                    style: const TextStyle(
                                        fontSize: 12, color: Colors.blue),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                                InkWell(
                                  onTap: () => setState(() =>
                                      currentSourceLinks.removeAt(entry.key)),
                                  child: const Icon(Icons.close,
                                      size: 14, color: Colors.red),
                                ),
                              ],
                            ),
                          )),
                    const SizedBox(height: 16),

                    // 5. Date Picker
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
                    sourceLinks: currentSourceLinks, // Save links
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
