import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';
import '../domain/news_model.dart';

class NewsDetailScreen extends StatelessWidget {
  final NewsLog news;

  const NewsDetailScreen({Key? key, required this.news}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy년 MM월 dd일 HH:mm');

    return Scaffold(
      appBar: AppBar(
        title: const Text('News Detail'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 1. 이미지 표시 영역 (이미지가 있을 경우에만)
            if (news.imageUrl != null && news.imageUrl!.isNotEmpty) ...[
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.network(
                  news.imageUrl!,
                  width: double.infinity,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) => Container(
                    height: 200,
                    color: Colors.grey[200],
                    child: const Center(
                      child: Icon(Icons.broken_image, size: 48, color: Colors.grey),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
            ],

            // 2. 제목
            Text(
              news.title,
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),

            // 3. 날짜 및 태그
            Row(
              children: [
                Icon(Icons.access_time, size: 16, color: Colors.grey[600]),
                const SizedBox(width: 4),
                Text(
                  dateFormat.format(news.date),
                  style: TextStyle(color: Colors.grey[600], fontSize: 14),
                ),
              ],
            ),
            const SizedBox(height: 12),
            
            // 태그 리스트
            if (news.tags.isNotEmpty)
              Wrap(
                spacing: 8,
                runSpacing: 4,
                children: news.tags.map((tag) => Chip(
                  label: Text('#$tag', style: const TextStyle(fontSize: 12)),
                  backgroundColor: Colors.blue[50],
                  side: BorderSide.none,
                  padding: EdgeInsets.zero,
                )).toList(),
              ),
            
            const Divider(height: 32),

            // 4. 본문 내용
            Text(
              news.content,
              style: const TextStyle(fontSize: 16, height: 1.6),
            ),
            
            const SizedBox(height: 32),

            // 5. 관련 기사 링크 (Links)
            if (news.sourceLinks.isNotEmpty) ...[
              const Text(
                '🔗 관련 기사 링크',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 12),
              Container(
                decoration: BoxDecoration(
                  color: Colors.grey[50],
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.grey[200]!),
                ),
                child: Column(
                  children: news.sourceLinks.map((link) {
                    return ListTile(
                      leading: const CircleAvatar(
                        backgroundColor: Colors.blue,
                        radius: 16,
                        child: Icon(Icons.link, size: 16, color: Colors.white),
                      ),
                      title: Text(
                        link['title'] ?? link['url'] ?? '링크',
                        style: const TextStyle(
                          color: Colors.blue,
                          decoration: TextDecoration.underline,
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      trailing: const Icon(Icons.open_in_new, size: 16),
                      onTap: () async {
                        final urlStr = link['url'];
                        if (urlStr != null && urlStr.isNotEmpty) {
                          final Uri url = Uri.parse(urlStr);
                          if (await canLaunchUrl(url)) {
                            // 웹, 모바일 상관없이 안전하게 띄워줌
                            await launchUrl(url, mode: LaunchMode.externalApplication);
                          } else {
                            if (context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(content: Text('링크를 열 수 없습니다.')),
                              );
                            }
                          }
                        }
                      },
                    );
                  }).toList(),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
