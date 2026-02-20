import '../../analysis/domain/source_code_file.dart';
import '../domain/diagram_enums.dart';
import '../domain/diagram_model.dart';

class DiagramEditorState {
  const DiagramEditorState({
    this.current,
    this.previewType = DiagramType.classMap,
    this.previewScale = 1.0,
    this.hideExternalNodes = false,
    this.hideInterfaceNodes = false,
    this.maxPreviewNodes = 120,
    this.history = const [],
    this.isSaving = false,
    this.error,
  });

  final DiagramModel? current;
  final DiagramType previewType;
  final double previewScale;
  final bool hideExternalNodes;
  final bool hideInterfaceNodes;
  final int maxPreviewNodes;
  final List<DiagramModel> history;
  final bool isSaving;
  final String? error;

  DiagramEditorState copyWith({
    DiagramModel? current,
    bool clearCurrent = false,
    DiagramType? previewType,
    double? previewScale,
    bool? hideExternalNodes,
    bool? hideInterfaceNodes,
    int? maxPreviewNodes,
    List<DiagramModel>? history,
    bool? isSaving,
    String? error,
    bool clearError = false,
  }) {
    return DiagramEditorState(
      current: clearCurrent ? null : current ?? this.current,
      previewType: previewType ?? this.previewType,
      previewScale: previewScale ?? this.previewScale,
      hideExternalNodes: hideExternalNodes ?? this.hideExternalNodes,
      hideInterfaceNodes: hideInterfaceNodes ?? this.hideInterfaceNodes,
      maxPreviewNodes: maxPreviewNodes ?? this.maxPreviewNodes,
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
