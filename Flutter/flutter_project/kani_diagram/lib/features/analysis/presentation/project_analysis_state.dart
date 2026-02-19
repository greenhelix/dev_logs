import '../data/code_parser_service.dart';
import '../domain/source_code_file.dart';

class ProjectAnalysisState {
  const ProjectAnalysisState({
    this.files = const [],
    this.analyzedFiles = const [],
    this.selectedPaths = const {},
    this.isLoading = false,
    this.notice,
    this.error,
  });

  final List<SourceCodeFile> files;
  final List<AnalyzedCodeFile> analyzedFiles;
  final Set<String> selectedPaths;
  final bool isLoading;
  final String? notice;
  final String? error;

  List<SourceCodeFile> get selectedFiles {
    return files.where((f) => selectedPaths.contains(f.path)).toList();
  }

  ProjectAnalysisState copyWith({
    List<SourceCodeFile>? files,
    List<AnalyzedCodeFile>? analyzedFiles,
    Set<String>? selectedPaths,
    bool? isLoading,
    String? notice,
    String? error,
    bool clearNotice = false,
    bool clearError = false,
  }) {
    return ProjectAnalysisState(
      files: files ?? this.files,
      analyzedFiles: analyzedFiles ?? this.analyzedFiles,
      selectedPaths: selectedPaths ?? this.selectedPaths,
      isLoading: isLoading ?? this.isLoading,
      notice: clearNotice ? null : notice ?? this.notice,
      error: clearError ? null : error ?? this.error,
    );
  }
}
