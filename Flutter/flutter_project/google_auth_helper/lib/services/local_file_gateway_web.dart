import 'local_file_gateway.dart';

class WebLocalFileGateway implements LocalFileGateway {
  @override
  bool get supportsLocalFiles => false;

  @override
  Future<bool> fileExists(String filePath) async => false;

  @override
  Future<List<String>> findFiles(
    String rootDirectory,
    Set<String> fileNames,
  ) async {
    return const [];
  }

  @override
  Future<String?> findFirstFile(String rootDirectory, String fileName) async {
    return null;
  }

  @override
  Future<String?> findLatestFile(String rootDirectory, String fileName) async {
    return null;
  }

  @override
  String normalizePath(String filePath) => filePath;

  @override
  Future<String> readAsString(String filePath) {
    throw UnsupportedError('Local file access is not available on web.');
  }
}

LocalFileGateway createGateway() => WebLocalFileGateway();
