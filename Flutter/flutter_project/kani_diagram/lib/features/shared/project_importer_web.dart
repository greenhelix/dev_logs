import '../analysis/domain/source_code_file.dart';

Future<List<SourceCodeFile>> importProjectFromPath(String rootPath) async {
  // 웹에서는 보안 제약으로 임의 로컬 경로 접근이 불가능합니다.
  return const [];
}
