import 'dart:io';

import '../analysis/domain/source_code_file.dart';

Future<List<SourceCodeFile>> importProjectFromPath(String rootPath) async {
  final root = Directory(rootPath);
  if (!await root.exists()) {
    return const [];
  }

  final files = <SourceCodeFile>[];
  await for (final entity in root.list(recursive: true, followLinks: false)) {
    if (entity is! File) continue;
    final path = entity.path;
    if (!path.endsWith('.dart') &&
        !path.endsWith('.yaml') &&
        !path.endsWith('.json')) {
      continue;
    }

    final content = await entity.readAsString();
    final stat = await entity.stat();
    files.add(
      SourceCodeFile(
        name: path.split(Platform.pathSeparator).last,
        path: path,
        content: content,
        size: stat.size,
      ),
    );
  }

  return files;
}
