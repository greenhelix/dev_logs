// lib/data/providers.dart
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

// --- Imports (Clean Architecture 구조 반영) ---
import '../features/person/data/person_firestore_repository.dart';
import '../features/person/domain/person_model.dart';

import '../features/news/data/news_firestore_repository.dart';
import '../features/news/domain/news_model.dart';

// [DB Instance Provider]
// 여러 Repository에서 공통으로 사용할 Firestore 인스턴스 (Named DB)
final firestoreProvider = Provider<FirebaseFirestore>((ref) {
  return FirebaseFirestore.instanceFor(
    app: Firebase.app(),
    databaseId: 'db-data-collector', // ★ 사용자 지정 DB
  );
});

// --- [Person Feature Providers] ---

// 1. Repository Provider
// Firestore 인스턴스를 주입받아 Repository 생성
final personRepositoryProvider = Provider<PersonFirestoreRepository>((ref) {
  final firestore = ref.watch(firestoreProvider);
  return PersonFirestoreRepository(firestore: firestore);
});

// 2. Stream Provider (UI 구독용)
final personStreamProvider = StreamProvider<List<PersonModel>>((ref) {
  final repository = ref.read(personRepositoryProvider);
  return repository.streamPeople();
});


// --- [News Feature Providers] ---

// 1. Repository Provider
final newsRepositoryProvider = Provider<NewsFirestoreRepository>((ref) {
  final firestore = ref.watch(firestoreProvider);
  return NewsFirestoreRepository(firestore: firestore);
});

// 2. Stream Provider (UI 구독용)
final newsStreamProvider = StreamProvider<List<NewsLog>>((ref) {
  final repository = ref.read(newsRepositoryProvider);
  return repository.streamNews();
});
