import 'dart:typed_data';

enum ImportSourceKind { archive, directory }

abstract class ImportSource {
  const ImportSource({
    required this.kind,
    required this.displayName,
  });

  final ImportSourceKind kind;
  final String displayName;
}

class ArchiveImportSource extends ImportSource {
  const ArchiveImportSource({
    required this.fileName,
    required this.bytes,
  }) : super(
          kind: ImportSourceKind.archive,
          displayName: fileName,
        );

  final String fileName;
  final Uint8List bytes;
}

class DirectoryImportSource extends ImportSource {
  const DirectoryImportSource({
    required this.directoryPath,
    required this.label,
  }) : super(
          kind: ImportSourceKind.directory,
          displayName: label,
        );

  final String directoryPath;
  final String label;
}
