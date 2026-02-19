import 'package:cloud_firestore/cloud_firestore.dart';

import '../domain/diagram_enums.dart';
import '../domain/diagram_model.dart';
import '../domain/diagram_relation.dart';

abstract class DiagramRepository {
  Future<void> saveDiagram(DiagramModel diagram);
  Future<List<DiagramModel>> loadRecentDiagrams();
}

class FirestoreDiagramRepository implements DiagramRepository {
  FirestoreDiagramRepository(this._firestore);

  final FirebaseFirestore _firestore;

  CollectionReference<Map<String, dynamic>> get _collection =>
      _firestore.collection('diagrams');

  @override
  Future<void> saveDiagram(DiagramModel diagram) async {
    await _collection.doc(diagram.id).set({
      'name': diagram.name,
      'type': diagram.type.name,
      'sizeInBytes': diagram.sizeInBytes,
      'updatedAt': FieldValue.serverTimestamp(),
      'nodes': diagram.nodes
          .map((n) => {'id': n.id, 'label': n.label, 'group': n.group})
          .toList(),
      'relations': diagram.relations
          .map((r) => {'from': r.from, 'to': r.to, 'type': r.type.name})
          .toList(),
    });
  }

  @override
  Future<List<DiagramModel>> loadRecentDiagrams() async {
    final snapshot = await _collection
        .orderBy('updatedAt', descending: true)
        .limit(20)
        .get();

    return snapshot.docs.map((doc) {
      final data = doc.data();
      final type = DiagramType.values.firstWhere(
        (e) => e.name == data['type'],
        orElse: () => DiagramType.classMap,
      );

      final nodesRaw = (data['nodes'] as List<dynamic>? ?? const []);
      final relationsRaw = (data['relations'] as List<dynamic>? ?? const []);

      return DiagramModel(
        id: doc.id,
        name: data['name'] as String? ?? 'Untitled',
        type: type,
        sizeInBytes: data['sizeInBytes'] as int? ?? 0,
        nodes: nodesRaw
            .map((item) => DiagramNode(
                  id: item['id'] as String,
                  label: item['label'] as String,
                  group: item['group'] as String? ?? 'core',
                ))
            .toList(),
        relations: relationsRaw
            .map((item) => DiagramRelation(
                  from: item['from'] as String,
                  to: item['to'] as String,
                  type: RelationshipType.values.firstWhere(
                    (e) => e.name == item['type'],
                    orElse: () => RelationshipType.dependsOn,
                  ),
                ))
            .toList(),
      );
    }).toList();
  }
}

class InMemoryDiagramRepository implements DiagramRepository {
  final List<DiagramModel> _items = [];

  @override
  Future<List<DiagramModel>> loadRecentDiagrams() async {
    return List.unmodifiable(_items.reversed.take(20));
  }

  @override
  Future<void> saveDiagram(DiagramModel diagram) async {
    _items.removeWhere((item) => item.id == diagram.id);
    _items.add(diagram);
  }
}
