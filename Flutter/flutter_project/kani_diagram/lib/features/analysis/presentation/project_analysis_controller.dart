import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/code_parser_service.dart';
import '../data/project_folder_picker.dart';
import '../domain/source_code_file.dart';
import 'project_analysis_state.dart';

final codeParserServiceProvider = Provider<CodeParserService>((ref) {
  return CodeParserService();
});

final projectAnalysisControllerProvider =
    StateNotifierProvider<ProjectAnalysisController, ProjectAnalysisState>((ref) {
  return ProjectAnalysisController(
    ref.read(codeParserServiceProvider),
    createProjectFolderPicker(),
  );
});

class ProjectAnalysisController extends StateNotifier<ProjectAnalysisState> {
  ProjectAnalysisController(this._codeParser, this._folderPicker)
      : super(const ProjectAnalysisState());

  final CodeParserService _codeParser;
  final ProjectFolderPicker _folderPicker;

  Future<void> pickProjectFolder() async {
    state = state.copyWith(
      isLoading: true,
      clearError: true,
      clearNotice: true,
    );

    try {
      final sourceFiles = await _folderPicker.pickCodeFilesFromFolder(
        allowedExtensions: const {'dart', 'yaml', 'yml', 'json', 'kt', 'java', 'swift'},
      );
      if (sourceFiles.isEmpty) {
        state = state.copyWith(
          isLoading: false,
          notice: '폴더에서 분석 가능한 코드 파일을 찾지 못했습니다.',
        );
        return;
      }

      final analyzed = _codeParser.analyzeCoreFiles(sourceFiles);

      state = state.copyWith(
        files: sourceFiles,
        analyzedFiles: analyzed,
        selectedPaths: analyzed.take(8).map((f) => f.source.path).toSet(),
        isLoading: false,
        notice: '폴더 업로드 완료: ${sourceFiles.length}개 파일 임시 적재',
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: '파일 분석 중 오류가 발생했습니다: $e',
      );
    }
  }

  void toggleSelection(String path) {
    final next = {...state.selectedPaths};
    if (next.contains(path)) {
      next.remove(path);
    } else {
      next.add(path);
    }
    state = state.copyWith(selectedPaths: next);
  }

  void selectAllAnalyzedFiles() {
    final all = state.analyzedFiles.map((e) => e.source.path).toSet();
    state = state.copyWith(
      selectedPaths: all,
      notice: '전체 선택 완료: ${all.length}개',
      clearError: true,
    );
  }

  void clearAllSelections() {
    state = state.copyWith(
      selectedPaths: const {},
      notice: '선택 해제 완료',
      clearError: true,
    );
  }

  List<SourceCodeFile> selectedFilesForDiagram() {
    return state.selectedFiles;
  }

  void clearNotice() {
    state = state.copyWith(clearNotice: true);
  }

  void setNotice(String message) {
    state = state.copyWith(
      notice: message,
      clearError: true,
    );
  }

  void clearAllTemporaryFiles() {
    state = state.copyWith(
      files: const [],
      analyzedFiles: const [],
      selectedPaths: const {},
      clearError: true,
      notice: '임시 업로드 파일을 모두 삭제했습니다.',
    );
  }

  void clearTemporaryAfterDiagramSuccess(int analyzedFileCount) {
    state = state.copyWith(
      files: const [],
      analyzedFiles: const [],
      selectedPaths: const {},
      clearError: true,
      notice: '다이어그램 생성 성공: $analyzedFileCount개 임시 파일 삭제 완료',
    );
  }
}
