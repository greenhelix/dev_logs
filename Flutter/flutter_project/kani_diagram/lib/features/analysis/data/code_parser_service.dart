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
    final declarationRegex = RegExp(
      r'(?:abstract\s+class|class|mixin|enum)\s+([A-Za-z_]\w*)([^{]*)\{?',
      multiLine: true,
    );
    final tokenUsageRegex = RegExp(r'\b([A-Z][A-Za-z0-9_]*)\b');

    final nodes = <DiagramNode>[];
    final relations = <DiagramRelation>[];
    final knownNodes = <String>{};
    final relationKeys = <String>{};
    final ownersByFilePath = <String, List<String>>{};

    void ensureNode(String id, {String group = 'core'}) {
      if (knownNodes.add(id)) {
        nodes.add(DiagramNode(id: id, label: id, group: group));
      }
    }

    void ensureRelation({
      required String from,
      required String to,
      required RelationshipType type,
    }) {
      if (from == to) return;
      final key = '$from|$to|${type.name}';
      if (relationKeys.add(key)) {
        relations.add(DiagramRelation(from: from, to: to, type: type));
      }
    }

    String? sanitizeTypeName(String raw) {
      var type = raw.trim();
      if (type.isEmpty) return null;
      type = type.replaceAll('?', '');
      if (type.contains('<')) {
        type = type.substring(0, type.indexOf('<'));
      }
      type = type.split(' ').first;
      type = type.replaceAll(RegExp(r'[^A-Za-z0-9_]'), '');
      if (type.isEmpty) return null;
      if (!RegExp(r'^[A-Za-z_]\w*$').hasMatch(type)) return null;
      final ignored = <String>{
        'void',
        'dynamic',
        'Object',
        'String',
        'int',
        'double',
        'bool',
        'num',
        'Map',
        'List',
        'Set',
        'Future',
        'Stream',
      };
      if (ignored.contains(type)) return null;
      return type;
    }

    Iterable<String> parseTypeList(String raw) sync* {
      for (final item in raw.split(',')) {
        final name = sanitizeTypeName(item);
        if (name != null) {
          yield name;
        }
      }
    }

    List<String> ownersForFile(String path) => ownersByFilePath[path] ?? const [];

    // First pass: collect declarations so dependency edges can target known nodes.
    for (final file in selectedFiles) {
      final fileGroup = file.path.contains('/presentation/')
          ? 'presentation'
          : file.path.contains('/data/')
              ? 'data'
              : 'domain';
      final owners = <String>[];
      for (final match in declarationRegex.allMatches(file.content)) {
        final owner = sanitizeTypeName(match.group(1) ?? '');
        if (owner == null) continue;
        owners.add(owner);
        ensureNode(owner, group: fileGroup);
      }
      ownersByFilePath[file.path] = owners;
    }

    for (final file in selectedFiles) {
      final fileGroup = file.path.contains('/presentation/')
          ? 'presentation'
          : file.path.contains('/data/')
              ? 'data'
              : 'domain';
      final owners = ownersForFile(file.path);

      for (final match in declarationRegex.allMatches(file.content)) {
        final owner = sanitizeTypeName(match.group(1) ?? '');
        if (owner == null) continue;
        final clause = (match.group(2) ?? '').trim();
        ensureNode(owner, group: fileGroup);

        final extendsMatch = RegExp(
          r'extends\s+([A-Za-z_][A-Za-z0-9_]*(?:\s*<[^>{}]+>)?)',
        ).firstMatch(clause);
        if (extendsMatch != null) {
          final parent = sanitizeTypeName(extendsMatch.group(1) ?? '');
          if (parent != null) {
            ensureNode(parent, group: 'external');
            ensureRelation(
              from: owner,
              to: parent,
              type: RelationshipType.extendsClass,
            );
          }
        }

        final withMatch = RegExp(
          r'with\s+([A-Za-z0-9_<>,\s]+?)(?:\s+implements|$)',
        ).firstMatch(clause);
        if (withMatch != null) {
          for (final mixinType in parseTypeList(withMatch.group(1) ?? '')) {
            ensureNode(mixinType, group: 'external');
            ensureRelation(
              from: owner,
              to: mixinType,
              type: RelationshipType.dependsOn,
            );
          }
        }

        final implementsMatch = RegExp(r'implements\s+([^{]+)').firstMatch(clause);
        if (implementsMatch != null) {
          for (final target in parseTypeList(implementsMatch.group(1) ?? '')) {
            ensureNode(target, group: 'interface');
            ensureRelation(
              from: owner,
              to: target,
              type: RelationshipType.implementsClass,
            );
          }
        }
      }

      // File-level dependency: type usages in file content -> known declarations.
      final seenPerOwner = <String, Set<String>>{
        for (final owner in owners) owner: <String>{},
      };
      for (final tokenMatch in tokenUsageRegex.allMatches(file.content)) {
        final token = sanitizeTypeName(tokenMatch.group(1) ?? '');
        if (token == null) continue;
        if (!knownNodes.contains(token)) continue;

        for (final owner in owners) {
          if (owner == token) continue;
          final seen = seenPerOwner[owner];
          if (seen == null || !seen.add(token)) continue;
          ensureRelation(
            from: owner,
            to: token,
            type: RelationshipType.dependsOn,
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
