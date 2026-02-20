import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../domain/diagram_enums.dart';
import '../../domain/diagram_model.dart';
import '../../domain/diagram_relation.dart';

class DiagramPreviewCanvas extends StatelessWidget {
  const DiagramPreviewCanvas({
    super.key,
    required this.diagram,
    required this.previewScale,
    required this.hideExternalNodes,
    required this.hideInterfaceNodes,
    required this.maxPreviewNodes,
  });

  final DiagramModel diagram;
  final double previewScale;
  final bool hideExternalNodes;
  final bool hideInterfaceNodes;
  final int maxPreviewNodes;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final previewGraph = _buildPreviewGraph(
          hideExternalNodes: hideExternalNodes,
          hideInterfaceNodes: hideInterfaceNodes,
          maxPreviewNodes: maxPreviewNodes,
        );
        final virtualSize = _virtualCanvasSize(
          nodeCount: previewGraph.nodes.length,
          previewScale: previewScale,
        );

        return InteractiveViewer(
          minScale: 0.35,
          maxScale: 3.2,
          constrained: false,
          boundaryMargin: const EdgeInsets.all(200),
          child: SizedBox(
            width: virtualSize.width,
            height: virtualSize.height,
            child: CustomPaint(
              painter: _DiagramPainter(
                diagramType: diagram.type,
                nodes: previewGraph.nodes,
                relations: previewGraph.relations,
              ),
            ),
          ),
        );
      },
    );
  }

  Size _virtualCanvasSize({
    required int nodeCount,
    required double previewScale,
  }) {
    if (nodeCount <= 12) {
      return Size(900 * previewScale, 620 * previewScale);
    }
    if (nodeCount <= 40) {
      return Size(1300 * previewScale, 820 * previewScale);
    }
    if (nodeCount <= 80) {
      return Size(1800 * previewScale, 1100 * previewScale);
    }
    final extra = math.sqrt(nodeCount).clamp(9, 24);
    return Size((extra * 220) * previewScale, (extra * 165) * previewScale);
  }

  _PreviewGraph _buildPreviewGraph({
    required bool hideExternalNodes,
    required bool hideInterfaceNodes,
    required int maxPreviewNodes,
  }) {
    final filteredNodes = diagram.nodes.where((node) {
      if (hideExternalNodes && node.group == 'external') return false;
      if (hideInterfaceNodes && node.group == 'interface') return false;
      return true;
    }).toList();

    final allowedIds = filteredNodes.map((e) => e.id).toSet();
    final filteredRelations = diagram.relations
        .where((r) => allowedIds.contains(r.from) && allowedIds.contains(r.to))
        .toList();

    if (filteredNodes.length <= maxPreviewNodes) {
      return _PreviewGraph(nodes: filteredNodes, relations: filteredRelations);
    }

    final degree = <String, int>{for (final node in filteredNodes) node.id: 0};
    for (final relation in filteredRelations) {
      degree[relation.from] = (degree[relation.from] ?? 0) + 1;
      degree[relation.to] = (degree[relation.to] ?? 0) + 1;
    }

    final sortedNodes = [...filteredNodes]
      ..sort((a, b) {
        final degreeCompare = (degree[b.id] ?? 0).compareTo(degree[a.id] ?? 0);
        if (degreeCompare != 0) return degreeCompare;
        return a.id.compareTo(b.id);
      });

    final reducedNodes = sortedNodes.take(maxPreviewNodes).toList();
    final reducedIds = reducedNodes.map((e) => e.id).toSet();
    final reducedRelations = filteredRelations
        .where((r) => reducedIds.contains(r.from) && reducedIds.contains(r.to))
        .toList();

    return _PreviewGraph(nodes: reducedNodes, relations: reducedRelations);
  }
}

class _DiagramPainter extends CustomPainter {
  _DiagramPainter({
    required this.diagramType,
    required this.nodes,
    required this.relations,
  });

  final DiagramType diagramType;
  final List<DiagramNode> nodes;
  final List<DiagramRelation> relations;

  @override
  void paint(Canvas canvas, Size size) {
    final nodePaint = Paint()
      ..color = const Color(0xFF0F766E).withValues(alpha: 0.12);
    final borderPaint = Paint()
      ..color = const Color(0xFF0F766E)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.2;
    final linePaint = Paint()
      ..color = const Color(0xFF94A3B8)
      ..strokeWidth = 1.0;

    if (nodes.isEmpty) {
      final tp = TextPainter(
        text: const TextSpan(
          text: '선택한 파일에서 구조를 찾지 못했습니다.',
          style: TextStyle(color: Color(0xFF475467), fontSize: 14),
        ),
        textDirection: TextDirection.ltr,
      )..layout(maxWidth: size.width - 32);
      tp.paint(
        canvas,
        Offset((size.width - tp.width) / 2, (size.height - tp.height) / 2),
      );
      return;
    }

    final nodeCount = nodes.length;
    final nodeWidth = nodeCount > 100
        ? 92.0
        : nodeCount > 55
        ? 102.0
        : 118.0;
    final nodeHeight = nodeCount > 100 ? 30.0 : 36.0;
    final textMaxWidth = nodeWidth - 16;

    final positions = _calculatePositions(
      size,
      nodeWidth: nodeWidth,
      nodeHeight: nodeHeight,
    );
    final byId = <String, Offset>{};

    for (var i = 0; i < nodes.length; i++) {
      byId[nodes[i].id] = positions[i];
    }

    for (final relation in relations) {
      final from = byId[relation.from];
      final to = byId[relation.to];
      if (from == null || to == null) continue;
      canvas.drawLine(from, to, linePaint);
    }

    for (var i = 0; i < nodes.length; i++) {
      final node = nodes[i];
      final center = positions[i];
      final rect = Rect.fromCenter(
        center: center,
        width: nodeWidth,
        height: nodeHeight,
      );
      canvas.drawRRect(
        RRect.fromRectAndRadius(rect, const Radius.circular(10)),
        nodePaint,
      );
      canvas.drawRRect(
        RRect.fromRectAndRadius(rect, const Radius.circular(10)),
        borderPaint,
      );

      final tp = TextPainter(
        text: TextSpan(
          text: _normalizedLabel(node.label),
          style: TextStyle(
            fontSize: nodeCount > 100 ? 9 : 11,
            color: const Color(0xFF164E63),
          ),
        ),
        textDirection: TextDirection.ltr,
        maxLines: 1,
        ellipsis: '...',
      )..layout(maxWidth: textMaxWidth);
      tp.paint(
        canvas,
        Offset(center.dx - tp.width / 2, center.dy - tp.height / 2),
      );
    }
  }

  String _normalizedLabel(String source) {
    if (diagramType != DiagramType.classMap) {
      return source;
    }

    var label = source.replaceAll('.dart', '');
    label = label.replaceAll('_', ' ');
    label = label.replaceAllMapped(
      RegExp(r'([a-z0-9])([A-Z])'),
      (m) => '${m.group(1)} ${m.group(2)}',
    );

    final rawWords = label
        .split(RegExp(r'\s+'))
        .where((w) => w.trim().isNotEmpty)
        .map((w) => w.trim())
        .toList();

    final stopwords = <String>{'class', 'dart', 'popup', 'table'};

    final seen = <String>{};
    final kept = <String>[];
    for (final word in rawWords) {
      final lower = word.toLowerCase();
      if (stopwords.contains(lower)) continue;
      if (!seen.add(lower)) continue;
      kept.add(word);
    }

    final result = kept.isEmpty ? source : kept.join(' ');
    return result.length > 24 ? '${result.substring(0, 21)}...' : result;
  }

  List<Offset> _calculatePositions(
    Size size, {
    required double nodeWidth,
    required double nodeHeight,
  }) {
    final count = nodes.length;
    final positions = <Offset>[];
    final safeX = nodeWidth * 0.6;
    final safeY = nodeHeight * 0.8;

    switch (diagramType) {
      case DiagramType.flow:
        final step = (size.width - (safeX * 2)) / (count + 1);
        for (var i = 0; i < count; i++) {
          final y = i.isEven ? size.height * 0.36 : size.height * 0.64;
          positions.add(Offset(safeX + (step * (i + 1)), y));
        }
        break;
      case DiagramType.classMap:
        final cols = math.max(3, (math.sqrt(count)).ceil());
        final rows = (count / cols).ceil();
        for (var i = 0; i < count; i++) {
          final c = i % cols;
          final r = i ~/ cols;
          positions.add(
            Offset(
              safeX + ((size.width - safeX * 2) / cols) * (c + 0.5),
              safeY + ((size.height - safeY * 2) / rows) * (r + 0.5),
            ),
          );
        }
        break;
      case DiagramType.dependency:
        final radius = math.min(size.width, size.height) * 0.38;
        for (var i = 0; i < count; i++) {
          final angle = (2 * math.pi * i) / count;
          positions.add(
            Offset(
              size.width / 2 + radius * math.cos(angle),
              size.height / 2 + radius * math.sin(angle),
            ),
          );
        }
        break;
      case DiagramType.layered:
        final groups = <String, List<int>>{};
        for (var i = 0; i < nodes.length; i++) {
          groups.putIfAbsent(nodes[i].group, () => []).add(i);
        }
        final layers = groups.values.toList();
        for (var layerIndex = 0; layerIndex < layers.length; layerIndex++) {
          final layer = layers[layerIndex];
          for (var i = 0; i < layer.length; i++) {
            final x =
                safeX + ((size.width - safeX * 2) / layer.length) * (i + 0.5);
            final y =
                safeY +
                ((size.height - safeY * 2) / layers.length) *
                    (layerIndex + 0.5);
            positions.add(Offset(x, y));
          }
        }
        while (positions.length < count) {
          positions.add(Offset(size.width / 2, size.height / 2));
        }
        break;
      case DiagramType.sequence:
        final stepX = (size.width - (safeX * 2)) / (count + 1);
        for (var i = 0; i < count; i++) {
          positions.add(Offset(safeX + stepX * (i + 1), size.height / 2));
        }
        break;
    }

    return positions;
  }

  @override
  bool shouldRepaint(covariant _DiagramPainter oldDelegate) {
    return oldDelegate.nodes != nodes ||
        oldDelegate.relations != relations ||
        oldDelegate.diagramType != diagramType;
  }
}

class _PreviewGraph {
  const _PreviewGraph({required this.nodes, required this.relations});

  final List<DiagramNode> nodes;
  final List<DiagramRelation> relations;
}
