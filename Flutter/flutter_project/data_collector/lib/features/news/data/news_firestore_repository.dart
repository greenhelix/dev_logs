import 'package:cloud_firestore/cloud_firestore.dart';
import '../domain/news_model.dart';

class NewsFirestoreRepository {
  final FirebaseFirestore _firestore;

  NewsFirestoreRepository({required FirebaseFirestore firestore})
      : _firestore = firestore;

  CollectionReference get _collection => _firestore.collection('news');

  Future<void> addNews(NewsLog news) async {
    // ID가 있으면 지정, 없으면 자동 생성
    if (news.id != null && news.id!.isNotEmpty) {
      await _collection.doc(news.id).set(news.toMap());
    } else {
      await _collection.add(news.toMap());
    }
  }

  Stream<List<NewsLog>> streamNews() {
    return _collection.orderBy('date', descending: true).snapshots().map((snapshot) {
      return snapshot.docs.map((doc) {
        return NewsLog.fromMap(doc.data() as Map<String, dynamic>, doc.id);
      }).toList();
    });
  }

  Future<void> updateNews(NewsLog news) async {
    if (news.id == null) return;
    await _collection.doc(news.id).update(news.toMap());
  }

  Future<void> deleteNews(String id) async {
    await _collection.doc(id).delete();
  }
}
