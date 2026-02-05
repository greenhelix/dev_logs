import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../data/local/app_database.dart'; // NewsLog 타입

class NewsDetailScreen extends StatelessWidget {
  final NewsLog news;

  const NewsDetailScreen({super.key, required this.news});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("News Detail"),
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 1. 이미지가 있다면 상단에 크게 표시
            if (news.imageUrl != null && news.imageUrl!.isNotEmpty)
              Image.network(
                news.imageUrl!,
                height: 300,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) => Container(
                    height: 200,
                    color: Colors.grey.shade200,
                    child: const Icon(Icons.broken_image)),
              ),

            Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 2. 날짜 (작은 글씨)
                  Text(
                    DateFormat('yyyy-MM-dd HH:mm').format(news.timestamp),
                    style: TextStyle(color: Colors.grey[600], fontSize: 14),
                  ),
                  const SizedBox(height: 12),

                  // 3. 제목 (큰 글씨)
                  Text(
                    news.title,
                    style: const TextStyle(
                        fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  const Divider(height: 30),

                  // 4. 본문 내용
                  Text(
                    news.content,
                    style: const TextStyle(fontSize: 16, height: 1.6),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
