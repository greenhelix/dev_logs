import 'dart:io';

String? findSampleUploadBundle() {
  final preferred = File('test_sample/sample_upload_bundle.zip');
  if (preferred.existsSync()) {
    return preferred.path;
  }

  final root = Directory('test_sample');
  if (!root.existsSync()) {
    return null;
  }

  final zipFiles = root
      .listSync(recursive: true)
      .whereType<File>()
      .where((file) => file.path.toLowerCase().endsWith('.zip'))
      .toList(growable: false)
    ..sort((a, b) => a.path.compareTo(b.path));
  return zipFiles.isEmpty ? null : zipFiles.first.path;
}

bool hasSampleImportDirectories() {
  final resultsRoot = Directory('test_sample/results');
  final logsRoot = Directory('test_sample/logs');
  return resultsRoot.existsSync() && logsRoot.existsSync();
}
