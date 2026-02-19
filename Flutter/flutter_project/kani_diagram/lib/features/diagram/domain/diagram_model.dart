import 'diagram_enums.dart';
import 'diagram_relation.dart';

class DiagramNode {
  const DiagramNode({
    required this.id,
    required this.label,
    required this.group,
  });

  final String id;
  final String label;
  final String group;

  DiagramNode copyWith({String? label, String? group}) {
    return DiagramNode(
      id: id,
      label: label ?? this.label,
      group: group ?? this.group,
    );
  }
}

class DiagramModel {
  const DiagramModel({
    required this.id,
    required this.name,
    required this.type,
    required this.nodes,
    required this.relations,
    required this.sizeInBytes,
  });

  final String id;
  final String name;
  final DiagramType type;
  final List<DiagramNode> nodes;
  final List<DiagramRelation> relations;
  final int sizeInBytes;

  int get recommendedMaxBytes => 4 * 1024 * 1024;
  bool get isTooLarge => sizeInBytes > recommendedMaxBytes;

  DiagramModel copyWith({
    String? name,
    DiagramType? type,
    List<DiagramNode>? nodes,
    List<DiagramRelation>? relations,
  }) {
    return DiagramModel(
      id: id,
      name: name ?? this.name,
      type: type ?? this.type,
      nodes: nodes ?? this.nodes,
      relations: relations ?? this.relations,
      sizeInBytes: sizeInBytes,
    );
  }
}
