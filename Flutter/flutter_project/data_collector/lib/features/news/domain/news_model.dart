class NewsLog {
  final String? id;
  final String title;
  final String content;
  final DateTime date;
  final List<String> tags;
  final String? imageUrl;
  final String? relatedPersonId;
  // Added to support multiple links with titles and URLs
  final List<Map<String, String>> sourceLinks;

  NewsLog({
    this.id,
    required this.title,
    required this.content,
    required this.date,
    this.tags = const [],
    this.imageUrl,
    this.relatedPersonId,
    this.sourceLinks = const [], // Initialize as empty list
  });

  // Convert from Firestore Map
  factory NewsLog.fromMap(Map<String, dynamic> map, String docId) {
    // Safely parse the list of maps from Firestore
    List<Map<String, String>> parsedLinks = [];
    if (map['sourceLinks'] != null) {
      for (var item in map['sourceLinks']) {
        parsedLinks.add(Map<String, String>.from(item));
      }
    }

    return NewsLog(
      id: docId,
      title: map['title'] ?? '',
      content: map['content'] ?? '',
      date: map['date'] != null
          ? DateTime.fromMillisecondsSinceEpoch(map['date'])
          : DateTime.now(),
      tags: List<String>.from(map['tags'] ?? []),
      imageUrl: map['imageUrl'] as String?,
      relatedPersonId: map['relatedPersonId'] as String?,
      sourceLinks: parsedLinks, // Assign parsed links
    );
  }

  // Convert to Firestore Map
  Map<String, dynamic> toMap() {
    return {
      'title': title,
      'content': content,
      'date': date.millisecondsSinceEpoch,
      'tags': tags,
      'imageUrl': imageUrl,
      'relatedPersonId': relatedPersonId,
      'sourceLinks': sourceLinks, // Save list of maps
    };
  }
}
