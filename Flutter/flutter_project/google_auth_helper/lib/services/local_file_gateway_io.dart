import 'dart:io';

import 'package:path/path.dart' as path;

import 'local_file_gateway.dart';

class IoLocalFileGateway implements LocalFileGateway {
  @override
  bool get supportsLocalFiles => true;

  @override
  Future<bool> fileExists(String filePath) async {
    return File(normalizePath(filePath)).exists();
  }

  @override
  Future<List<String>> findFiles(
    String rootDirectory,
    Set<String> fileNames,
  ) async {
    final directory = Directory(normalizePath(rootDirectory));
    if (!await directory.exists()) {
      return const [];
    }

    final matches = <String>[];
    await for (final entity in directory.list(recursive: true)) {
      if (entity is! File) {
        continue;
      }
      if (fileNames.contains(path.basename(entity.path))) {
        matches.add(entity.path);
      }
    }
    return matches;
  }

  @override
  Future<String?> findFirstFile(String rootDirectory, String fileName) async {
    final matches = await findFiles(rootDirectory, {fileName});
    return matches.isEmpty ? null : matches.first;
  }

  @override
  Future<String?> findLatestFile(String rootDirectory, String fileName) async {
    final matches = await findFiles(rootDirectory, {fileName});
    if (matches.isEmpty) {
      return null;
    }
    matches.sort();
    return matches.last;
  }

  @override
  String normalizePath(String filePath) => path.normalize(filePath.trim());

  @override
  Future<String> readAsString(String filePath) async {
    return File(normalizePath(filePath)).readAsString();
  }
}

LocalFileGateway createGateway() => IoLocalFileGateway();
