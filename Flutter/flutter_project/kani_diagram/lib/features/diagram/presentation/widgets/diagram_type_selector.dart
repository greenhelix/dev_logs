import 'package:flutter/material.dart';

import '../../domain/diagram_enums.dart';

class DiagramTypeSelector extends StatelessWidget {
  const DiagramTypeSelector({
    super.key,
    required this.current,
    required this.onChanged,
  });

  final DiagramType current;
  final ValueChanged<DiagramType> onChanged;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: DiagramType.values.map((type) {
        return ChoiceChip(
          label: Text(_label(type)),
          selected: current == type,
          onSelected: (_) => onChanged(type),
        );
      }).toList(),
    );
  }

  String _label(DiagramType type) {
    switch (type) {
      case DiagramType.flow:
        return 'Flow';
      case DiagramType.classMap:
        return 'Class';
      case DiagramType.dependency:
        return 'Dependency';
      case DiagramType.layered:
        return 'Layered';
      case DiagramType.sequence:
        return 'Sequence';
    }
  }
}
