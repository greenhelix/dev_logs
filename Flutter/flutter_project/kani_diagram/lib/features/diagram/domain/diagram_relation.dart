import 'diagram_enums.dart';

class DiagramRelation {
  const DiagramRelation({
    required this.from,
    required this.to,
    required this.type,
  });

  final String from;
  final String to;
  final RelationshipType type;
}
