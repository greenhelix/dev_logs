import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../domain/news_model.dart';

// 나중에 로컬로 돌아가고 싶으면 이 Provider의 반환값만 Drift Repository로 바꾸면 됨 (인터페이스 통일 시)
final newsRepositoryProvider = Provider<NewsFirestoreRepository>((ref) {
  final firestore = FirebaseFirestore.instanceFor(
    app: Firebase.app(),
    databaseId: 'db-data-collector', // default안쓰는 경우 따로 지정해야한다.
  );
  return NewsFirestoreRepository(firestore);
  // return PersonFirestoreRepository(FirebaseFirestore.instance); //default 인 경우
});

class NewsFirestoreRepository {
  final FirebaseFirestore _firestore;

  NewsFirestoreRepository(this._firestore);

  // 컬렉션 참조
  CollectionReference<Map<String, dynamic>> _getCollection() {
    return _firestore.collection('news');
  }

  // 1. 목록 조회 (Stream)
  Stream<List<NewsLog>> watchNews() {
    return _getCollection()
        .orderBy('date', descending: true)
        .snapshots()
        .map((snapshot) {
      return snapshot.docs.map((doc) {
        // NewsModel에 추가한 fromMap 사용
        return NewsLog.fromMap(doc.data(), doc.id);
      }).toList();
    });
  }

  // 2. 추가
  Future<void> addNews(NewsLog news) async {
    await _getCollection().add(news.toMap());
  }

  // 3. 수정
  Future<void> updateNews(NewsLog news) async {
    if (news.id == null) return;
    await _getCollection().doc(news.id).set(news.toMap());
  }

  // 4. 삭제
  Future<void> deleteNews(String id) async {
    await _getCollection().doc(id).delete();
  }
}

final newsListProvider = StreamProvider<List<NewsLog>>((ref) {
  final repository = ref.watch(newsRepositoryProvider);
  return repository.watchNews();
});
