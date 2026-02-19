import '../../analysis/domain/source_code_file.dart';
import '../domain/diagram_enums.dart';
import '../domain/diagram_model.dart';

class DiagramEditorState {
  const DiagramEditorState({
    this.current,
    this.previewType = DiagramType.classMap,
    this.history = const [],
    this.isSaving = false,
    this.error,
  });

  final DiagramModel? current;
  final DiagramType previewType;
  final List<DiagramModel> history;
  final bool isSaving;
  final String? error;

  DiagramEditorState copyWith({
    DiagramModel? current,
    bool clearCurrent = false,
    DiagramType? previewType,
    List<DiagramModel>? history,
    bool? isSaving,
    String? error,
    bool clearError = false,
  }) {
    return DiagramEditorState(
      current: clearCurrent ? null : current ?? this.current,
      previewType: previewType ?? this.previewType,
      history: history ?? this.history,
      isSaving: isSaving ?? this.isSaving,
      error: clearError ? null : error ?? this.error,
    );
  }

  List<SourceCodeFile> validateInputForScale(List<SourceCodeFile> files) {
    const recommendedMaxFileCount = 2000;
    if (files.length > recommendedMaxFileCount) {
      return files.take(recommendedMaxFileCount).toList();
    }
    return files;
  }
}
