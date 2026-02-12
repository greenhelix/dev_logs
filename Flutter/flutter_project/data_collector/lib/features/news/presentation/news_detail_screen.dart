import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../domain/news_model.dart';

class NewsDetailScreen extends StatelessWidget {
  final NewsLog news;

  const NewsDetailScreen({Key? key, required this.news}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy-MM-dd HH:mm');

    return Scaffold(
      appBar: AppBar(title: const Text('News Detail')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(news.title, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.calendar_today, size: 16, color: Colors.grey[600]),
                const SizedBox(width: 4),
                Text(dateFormat.format(news.date), style: TextStyle(color: Colors.grey[600])),
              ],
            ),
            const SizedBox(height: 16),
            if (news.tags.isNotEmpty)
              Wrap(
                spacing: 8,
                children: news.tags.map((tag) => Chip(
                  label: Text('#$tag'),
                  backgroundColor: Colors.blue[50],
                  labelStyle: TextStyle(color: Colors.blue[800]),
                )).toList(),
              ),
            const Divider(height: 30),
            Text(news.content, style: const TextStyle(fontSize: 16, height: 1.5)),
            
            if (news.relatedPersonId != null) ...[
               const SizedBox(height: 30),
               const Divider(),
               ListTile(
                 leading: const Icon(Icons.link),
                 title: const Text('관련 인물 ID'),
                 subtitle: Text(news.relatedPersonId!),
               ),
            ],
          ],
        ),
      ),
    );
  }
}
