import 'package:cloud_firestore/cloud_firestore.dart';
import '../domain/track_record_model.dart';

class TrackRecordRepository {
  final FirebaseFirestore _firestore;

  TrackRecordRepository({required FirebaseFirestore firestore})
      : _firestore = firestore;
  
  CollectionReference get _collection => _firestore.collection('track_records');

  Future<void> saveTrackRecord(TrackRecordModel record) async {
    await _collection.doc(record.id).set(record.toMap());
  }

  Stream<List<TrackRecordModel>> streamTrackRecords() {
    return _collection.orderBy('startTime', descending: true).snapshots().map((snapshot) {
      return snapshot.docs.map((doc) => TrackRecordModel.fromMap(doc.data() as Map<String, dynamic>, doc.id)).toList();
    });
  }

  Future<void> updateTrackRecordMemo(String id, String memo) async {
    await _collection.doc(id).update({'memo': memo});
  }

  Future<void> deleteTrackRecord(String id) async {
    await _collection.doc(id).delete();
  }
}
