import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../domain/diagram_enums.dart';
import '../../domain/diagram_model.dart';

class DiagramPreviewCanvas extends StatelessWidget {
  const DiagramPreviewCanvas({
    super.key,
    required this.diagram,
  });

  final DiagramModel diagram;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _DiagramPainter(diagram),
      child: const SizedBox.expand(),
    );
  }
}

class _DiagramPainter extends CustomPainter {
  _DiagramPainter(this.diagram);

  final DiagramModel diagram;

  @override
  void paint(Canvas canvas, Size size) {
    final nodePaint = Paint()..color = const Color(0xFF0F766E).withValues(alpha: 0.12);
    final borderPaint = Paint()
      ..color = const Color(0xFF0F766E)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.2;
    final linePaint = Paint()
      ..color = const Color(0xFF94A3B8)
      ..strokeWidth = 1.0;

    if (diagram.nodes.isEmpty) {
      final tp = TextPainter(
        text: const TextSpan(
          text: '선택한 파일에서 구조를 찾지 못했습니다.',
          style: TextStyle(color: Color(0xFF475467), fontSize: 14),
        ),
        textDirection: TextDirection.ltr,
      )..layout(maxWidth: size.width - 32);
      tp.paint(canvas, Offset((size.width - tp.width) / 2, (size.height - tp.height) / 2));
      return;
    }

    final positions = _calculatePositions(size);
    final byId = <String, Offset>{};

    for (var i = 0; i < diagram.nodes.length; i++) {
      byId[diagram.nodes[i].id] = positions[i];
    }

    for (final relation in diagram.relations) {
      final from = byId[relation.from];
      final to = byId[relation.to];
      if (from == null || to == null) continue;
      canvas.drawLine(from, to, linePaint);
    }

    for (var i = 0; i < diagram.nodes.length; i++) {
      final node = diagram.nodes[i];
      final center = positions[i];
      final rect = Rect.fromCenter(center: center, width: 110, height: 36);
      canvas.drawRRect(RRect.fromRectAndRadius(rect, const Radius.circular(10)), nodePaint);
      canvas.drawRRect(RRect.fromRectAndRadius(rect, const Radius.circular(10)), borderPaint);

      final tp = TextPainter(
        text: TextSpan(
          text: node.label,
          style: const TextStyle(fontSize: 11, color: Color(0xFF164E63)),
        ),
        textDirection: TextDirection.ltr,
        maxLines: 1,
        ellipsis: '...',
      )..layout(maxWidth: 96);
      tp.paint(canvas, Offset(center.dx - tp.width / 2, center.dy - tp.height / 2));
    }
  }

  List<Offset> _calculatePositions(Size size) {
    final count = diagram.nodes.length;
    final positions = <Offset>[];

    switch (diagram.type) {
      case DiagramType.flow:
        final step = size.width / (count + 1);
        for (var i = 0; i < count; i++) {
          final y = i.isEven ? size.height * 0.38 : size.height * 0.62;
          positions.add(Offset(step * (i + 1), y));
        }
        break;
      case DiagramType.classMap:
        final cols = math.max(2, (math.sqrt(count)).ceil());
        final rows = (count / cols).ceil();
        for (var i = 0; i < count; i++) {
          final c = i % cols;
          final r = i ~/ cols;
          positions.add(
            Offset(
              (size.width / (cols + 1)) * (c + 1),
              (size.height / (rows + 1)) * (r + 1),
            ),
          );
        }
        break;
      case DiagramType.dependency:
        final radius = math.min(size.width, size.height) * 0.35;
        for (var i = 0; i < count; i++) {
          final angle = (2 * math.pi * i) / count;
          positions.add(Offset(
            size.width / 2 + radius * math.cos(angle),
            size.height / 2 + radius * math.sin(angle),
          ));
        }
        break;
      case DiagramType.layered:
        final groups = <String, List<int>>{};
        for (var i = 0; i < diagram.nodes.length; i++) {
          groups.putIfAbsent(diagram.nodes[i].group, () => []).add(i);
        }
        final layers = groups.values.toList();
        for (var layerIndex = 0; layerIndex < layers.length; layerIndex++) {
          final layer = layers[layerIndex];
          for (var i = 0; i < layer.length; i++) {
            positions.add(
              Offset(
                (size.width / (layer.length + 1)) * (i + 1),
                (size.height / (layers.length + 1)) * (layerIndex + 1),
              ),
            );
          }
        }
        while (positions.length < count) {
          positions.add(Offset(size.width / 2, size.height / 2));
        }
        break;
      case DiagramType.sequence:
        final stepX = size.width / (count + 1);
        for (var i = 0; i < count; i++) {
          positions.add(Offset(stepX * (i + 1), size.height / 2));
        }
        break;
    }

    return positions;
  }

  @override
  bool shouldRepaint(covariant _DiagramPainter oldDelegate) {
    return oldDelegate.diagram != diagram;
  }
}
