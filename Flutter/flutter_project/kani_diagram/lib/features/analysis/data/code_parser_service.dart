import '../domain/source_code_file.dart';
import '../../diagram/domain/diagram_enums.dart';
import '../../diagram/domain/diagram_model.dart';
import '../../diagram/domain/diagram_relation.dart';

class AnalyzedCodeFile {
  const AnalyzedCodeFile({
    required this.source,
    required this.score,
    required this.reasons,
  });

  final SourceCodeFile source;
  final int score;
  final List<String> reasons;
}

class CodeParserService {
  List<AnalyzedCodeFile> analyzeCoreFiles(List<SourceCodeFile> files) {
    final ignoredPatterns = <String>[
      'generated',
      '/build/',
      '.g.dart',
      '.freezed.dart',
      '/test/',
      '/example/',
    ];

    final results = <AnalyzedCodeFile>[];
    for (final file in files) {
      final lowerPath = file.path.toLowerCase();
      if (ignoredPatterns.any(lowerPath.contains)) {
        continue;
      }

      final content = file.content;
      final reasons = <String>[];
      var score = 0;

      if (RegExp(r'class\s+\w+').hasMatch(content)) {
        score += 30;
        reasons.add('클래스 선언 포함');
      }
      if (RegExp(r'Widget|ConsumerWidget|StateNotifier|Notifier').hasMatch(content)) {
        score += 20;
        reasons.add('UI 또는 상태 관리 코드 포함');
      }
      if (RegExp(r'Repository|Service|UseCase|Controller').hasMatch(content)) {
        score += 25;
        reasons.add('핵심 로직 계층 키워드 포함');
      }
      if (RegExp(r'Future<|Stream<|async').hasMatch(content)) {
        score += 10;
        reasons.add('비동기 처리 포함');
      }
      if (RegExp(r'firebase|firestore', caseSensitive: false).hasMatch(content)) {
        score += 15;
        reasons.add('데이터 저장소 관련 코드 포함');
      }

      if (score >= 20) {
        results.add(AnalyzedCodeFile(source: file, score: score, reasons: reasons));
      }
    }

    results.sort((a, b) => b.score.compareTo(a.score));
    return results;
  }

  DiagramModel buildDiagram({
    required String id,
    required String name,
    required DiagramType type,
    required List<SourceCodeFile> selectedFiles,
  }) {
    final classNameRegex = RegExp(r'class\s+(\w+)');
    final extendsRegex = RegExp(r'class\s+(\w+)\s+extends\s+(\w+)');
    final implementsRegex = RegExp(r'class\s+(\w+)\s+implements\s+([^\{]+)');

    final nodes = <DiagramNode>[];
    final relations = <DiagramRelation>[];
    final knownNodes = <String>{};

    void ensureNode(String id, {String group = 'core'}) {
      if (knownNodes.add(id)) {
        nodes.add(DiagramNode(id: id, label: id, group: group));
      }
    }

    for (final file in selectedFiles) {
      final fileGroup = file.path.contains('/presentation/')
          ? 'presentation'
          : file.path.contains('/data/')
              ? 'data'
              : 'domain';

      final classNames = classNameRegex
          .allMatches(file.content)
          .map((m) => m.group(1))
          .whereType<String>()
          .toList();

      for (final className in classNames) {
        ensureNode(className, group: fileGroup);
      }

      for (final match in extendsRegex.allMatches(file.content)) {
        final child = match.group(1);
        final parent = match.group(2);
        if (child != null && parent != null) {
          ensureNode(child, group: fileGroup);
          ensureNode(parent, group: 'external');
          relations.add(
            DiagramRelation(from: child, to: parent, type: RelationshipType.extendsClass),
          );
        }
      }

      for (final match in implementsRegex.allMatches(file.content)) {
        final implementer = match.group(1);
        final targetsRaw = match.group(2);
        if (implementer == null || targetsRaw == null) continue;

        final targets = targetsRaw
            .split(',')
            .map((e) => e.trim().split(' ').first)
            .where((e) => e.isNotEmpty);

        for (final target in targets) {
          ensureNode(implementer, group: fileGroup);
          ensureNode(target, group: 'interface');
          relations.add(
            DiagramRelation(from: implementer, to: target, type: RelationshipType.implementsClass),
          );
        }
      }
    }

    final totalBytes = selectedFiles.fold<int>(0, (sum, file) => sum + file.size);

    return DiagramModel(
      id: id,
      name: name,
      type: type,
      nodes: nodes,
      relations: relations,
      sizeInBytes: totalBytes,
    );
  }
}
