import 'package:flutter_test/flutter_test.dart';
import 'package:kani_diagram/features/analysis/data/code_parser_service.dart';
import 'package:kani_diagram/features/analysis/domain/source_code_file.dart';
import 'package:kani_diagram/features/diagram/domain/diagram_enums.dart';

void main() {
  group('CodeParserService', () {
    test('analyzeCoreFiles excludes generated/test files', () {
      final service = CodeParserService();
      final files = [
        const SourceCodeFile(
          name: 'main.dart',
          path: 'lib/main.dart',
          content: 'class MainController { Future<void> load() async {} }',
          size: 100,
        ),
        const SourceCodeFile(
          name: 'model.g.dart',
          path: 'lib/model.g.dart',
          content: 'class Generated {}',
          size: 100,
        ),
        const SourceCodeFile(
          name: 'sample_test.dart',
          path: 'test/sample_test.dart',
          content: 'class TestFile {}',
          size: 100,
        ),
      ];

      final analyzed = service.analyzeCoreFiles(files);

      expect(analyzed.length, 1);
      expect(analyzed.first.source.path, 'lib/main.dart');
    });

    test('buildDiagram parses extends/with/implements and dependencies', () {
      final service = CodeParserService();
      final files = [
        const SourceCodeFile(
          name: 'user_repo.dart',
          path: 'lib/data/user_repo.dart',
          content: '''
abstract class Base {}
class UserRepo extends Base with Loggable implements Repository {
  final ApiClient client = ApiClient();
  void run() {
    client.fetch();
    ApiClient();
  }
}
''',
          size: 512,
        ),
        const SourceCodeFile(
          name: 'contracts.dart',
          path: 'lib/domain/contracts.dart',
          content: '''
mixin Loggable {}
abstract class Repository {}
class ApiClient {
  void fetch() {}
}
''',
          size: 512,
        ),
      ];

      final diagram = service.buildDiagram(
        id: 'd1',
        name: 'test',
        type: DiagramType.classMap,
        selectedFiles: files,
      );

      expect(diagram.nodes.any((n) => n.id == 'UserRepo'), isTrue);
      expect(diagram.nodes.any((n) => n.id == 'Base'), isTrue);
      expect(diagram.nodes.any((n) => n.id == 'Loggable'), isTrue);
      expect(diagram.nodes.any((n) => n.id == 'Repository'), isTrue);
      expect(diagram.nodes.any((n) => n.id == 'ApiClient'), isTrue);

      expect(
        diagram.relations.any(
          (r) =>
              r.from == 'UserRepo' &&
              r.to == 'Base' &&
              r.type == RelationshipType.extendsClass,
        ),
        isTrue,
      );
      expect(
        diagram.relations.any(
          (r) =>
              r.from == 'UserRepo' &&
              r.to == 'Loggable' &&
              r.type == RelationshipType.dependsOn,
        ),
        isTrue,
      );
      expect(
        diagram.relations.any(
          (r) =>
              r.from == 'UserRepo' &&
              r.to == 'Repository' &&
              r.type == RelationshipType.implementsClass,
        ),
        isTrue,
      );

      final apiDependencyCount = diagram.relations
          .where(
            (r) =>
                r.from == 'UserRepo' &&
                r.to == 'ApiClient' &&
                r.type == RelationshipType.dependsOn,
          )
          .length;
      expect(apiDependencyCount, 1);
    });
  });
}
