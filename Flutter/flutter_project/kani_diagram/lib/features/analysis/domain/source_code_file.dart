class SourceCodeFile {
  const SourceCodeFile({
    required this.name,
    required this.path,
    required this.content,
    required this.size,
  });

  final String name;
  final String path;
  final String content;
  final int size;

  bool get isDartFile => path.endsWith('.dart');
}
