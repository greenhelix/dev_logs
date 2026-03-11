import 'local_file_gateway_web.dart'
    if (dart.library.io) 'local_file_gateway_io.dart';

abstract class LocalFileGateway {
  bool get supportsLocalFiles;

  Future<bool> fileExists(String filePath);

  Future<String> readAsString(String filePath);

  Future<List<String>> findFiles(
    String rootDirectory,
    Set<String> fileNames,
  );

  Future<String?> findFirstFile(
    String rootDirectory,
    String fileName,
  );

  Future<String?> findLatestFile(
    String rootDirectory,
    String fileName,
  );

  String normalizePath(String filePath);
}

LocalFileGateway createLocalFileGateway() => createGateway();
