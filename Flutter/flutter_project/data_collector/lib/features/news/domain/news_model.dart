import 'package:cloud_firestore/cloud_firestore.dart';

class NewsLog {
  final String? id;
  final String title;
  final String content;
  final DateTime date;
  final List<String> tags;
  final String? relatedPersonId;

  NewsLog({
    this.id,
    required this.title,
    required this.content,
    required this.date,
    this.tags = const [],
    this.relatedPersonId,
  });

  // 📌 Firestore 데이터 -> 객체 변환 (Timestamp 처리 포함)
  factory NewsLog.fromMap(Map<String, dynamic> map, String docId) {
    // Timestamp -> DateTime 변환
    DateTime parsedDate = DateTime.now();

    if (map['date'] is Timestamp) {
      parsedDate = (map['date'] as Timestamp).toDate();
    } else if (map['date'] is String) {
      parsedDate = DateTime.tryParse(map['date']) ?? DateTime.now();
    }

    return NewsLog(
      id: docId,
      title: map['title'] ?? '',
      content: map['content'] ?? '',
      date: parsedDate,
      tags: List<String>.from(map['tags'] ?? []),
      relatedPersonId: map['relatedPersonId'] as String?,
    );
  }

  // 📌 객체 -> Firestore 저장용 Map 변환 (DateTime을 Timestamp로 변환)
  Map<String, dynamic> toMap() {
    return {
      'title': title,
      'content': content,
      'date': Timestamp.fromDate(date), // DateTime -> Firestore Timestamp
      'tags': tags,
      'relatedPersonId': relatedPersonId,
    };
  }
}
