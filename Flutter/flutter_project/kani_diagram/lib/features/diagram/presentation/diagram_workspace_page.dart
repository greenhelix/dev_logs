import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/firebase_config.dart';
import '../../analysis/presentation/project_analysis_controller.dart';
import '../../analysis/presentation/project_analysis_state.dart';
import 'diagram_editor_controller.dart';
import 'diagram_editor_state.dart';
import 'widgets/diagram_preview_canvas.dart';
import 'widgets/diagram_type_selector.dart';

class DiagramWorkspacePage extends ConsumerStatefulWidget {
  const DiagramWorkspacePage({super.key});

  @override
  ConsumerState<DiagramWorkspacePage> createState() => _DiagramWorkspacePageState();
}

class _DiagramWorkspacePageState extends ConsumerState<DiagramWorkspacePage> {
  final GlobalKey _previewKey = GlobalKey();

  @override
  Widget build(BuildContext context) {
    final analysisState = ref.watch(projectAnalysisControllerProvider);
    final analysisController = ref.read(projectAnalysisControllerProvider.notifier);

    final editorState = ref.watch(diagramEditorControllerProvider);
    final editorController = ref.read(diagramEditorControllerProvider.notifier);
    final canGenerateDiagram =
        !analysisState.isLoading && analysisState.selectedPaths.isNotEmpty;

    return Scaffold(
      appBar: AppBar(
        title: const Text('코드 다이어그램 분석기'),
        actions: [
          TextButton.icon(
            onPressed: editorState.isSaving ? null : editorController.saveCurrentDiagram,
            icon: const Icon(Icons.cloud_upload_outlined),
            label: const Text('Firestore 저장'),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 3,
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      FilledButton.icon(
                        onPressed: analysisState.isLoading ? null : analysisController.pickProjectFolder,
                        icon: const Icon(Icons.folder_open),
                        label: const Text('프로젝트 폴더 업로드'),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        '웹에서 폴더 업로드 시 임시 메모리 저장 후 분석하고, 다이어그램 생성 뒤 즉시 삭제합니다.',
                        style: TextStyle(fontSize: 12, color: Color(0xFF475467)),
                      ),
                      const SizedBox(height: 16),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          OutlinedButton.icon(
                            onPressed: analysisState.analyzedFiles.isEmpty
                                ? null
                                : analysisController.selectAllAnalyzedFiles,
                            icon: const Icon(Icons.select_all),
                            label: const Text('전체 선택'),
                          ),
                          OutlinedButton.icon(
                            onPressed: analysisState.selectedPaths.isEmpty
                                ? null
                                : analysisController.clearAllSelections,
                            icon: const Icon(Icons.deselect),
                            label: const Text('전체 해제'),
                          ),
                          Text(
                            '선택 ${analysisState.selectedPaths.length}/${analysisState.analyzedFiles.length}',
                            style: const TextStyle(fontSize: 12, color: Color(0xFF475467)),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Expanded(
                        child: ListView.builder(
                          itemCount: analysisState.analyzedFiles.length,
                          itemBuilder: (context, index) {
                            final item = analysisState.analyzedFiles[index];
                            final selected = analysisState.selectedPaths.contains(item.source.path);
                            return CheckboxListTile(
                              value: selected,
                              dense: true,
                              title: Text(item.source.name),
                              subtitle: Text('점수 ${item.score} | ${item.reasons.join(', ')}'),
                              onChanged: (_) => analysisController.toggleSelection(item.source.path),
                            );
                          },
                        ),
                      ),
                      if (analysisState.error != null)
                        Text(analysisState.error!, style: const TextStyle(color: Colors.red)),
                      if (analysisState.notice != null)
                        Text(analysisState.notice!, style: const TextStyle(color: Color(0xFF067647))),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              flex: 5,
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          FilledButton.icon(
                            onPressed: canGenerateDiagram
                                ? () {
                              final selected = analysisController.selectedFilesForDiagram();
                              final error = editorController.buildFromFiles(selected);
                              if (error == null) {
                                analysisController.clearTemporaryAfterDiagramSuccess(selected.length);
                              } else {
                                analysisController.setNotice('생성 실패: $error');
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(content: Text(error)),
                                );
                              }
                            }
                                : null,
                            icon: const Icon(Icons.hub_outlined),
                            label: const Text('다이어그램 생성'),
                          ),
                          OutlinedButton(
                            onPressed: () => editorController.exportDiagram(
                              repaintKey: _previewKey,
                              asJpg: false,
                            ),
                            child: const Text('PNG 내보내기'),
                          ),
                          OutlinedButton(
                            onPressed: () => editorController.exportDiagram(
                              repaintKey: _previewKey,
                              asJpg: true,
                            ),
                            child: const Text('JPG 내보내기'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        canGenerateDiagram
                            ? '현재 선택 파일로 다이어그램 생성이 가능합니다.'
                            : '파일을 1개 이상 선택해야 다이어그램 생성 버튼이 활성화됩니다.',
                        style: const TextStyle(fontSize: 12, color: Color(0xFF475467)),
                      ),
                      const SizedBox(height: 12),
                      DiagramTypeSelector(
                        current: editorState.previewType,
                        onChanged: editorController.setPreviewType,
                      ),
                      const SizedBox(height: 12),
                      if (editorState.current != null && editorState.current!.isTooLarge)
                        Container(
                          width: double.infinity,
                          margin: const EdgeInsets.only(bottom: 10),
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFEF3C7),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            '경고: 현재 다이어그램 크기(${(editorState.current!.sizeInBytes / (1024 * 1024)).toStringAsFixed(2)}MB)가 '
                            '권장치(4MB)를 초과했습니다. 파일 수를 줄이거나 핵심 파일만 선택하세요.',
                          ),
                        ),
                      Expanded(
                        child: RepaintBoundary(
                          key: _previewKey,
                          child: Container(
                            width: double.infinity,
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: const Color(0xFFD0D5DD)),
                            ),
                            child: editorState.current == null
                                ? const Center(child: Text('다이어그램을 생성하면 이곳에서 미리보기가 표시됩니다.'))
                                : DiagramPreviewCanvas(diagram: editorState.current!),
                          ),
                        ),
                      ),
                      if (editorState.error != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 8),
                          child: Text(editorState.error!, style: const TextStyle(color: Colors.red)),
                        ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              flex: 2,
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _DebugPanel(
                        analysisState: analysisState,
                        editorState: editorState,
                      ),
                      const Divider(height: 24),
                      Expanded(
                        child: _EditPanel(
                          editorState: editorState,
                          controller: editorController,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DebugPanel extends StatelessWidget {
  const _DebugPanel({
    required this.analysisState,
    required this.editorState,
  });

  final ProjectAnalysisState analysisState;
  final DiagramEditorState editorState;

  @override
  Widget build(BuildContext context) {
    final current = editorState.current;
    final sizeMb = current == null
        ? '0.00'
        : (current.sizeInBytes / (1024 * 1024)).toStringAsFixed(2);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('디버그/진행 상태', style: TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          _line('Project: ${FirebaseConfig.projectId}'),
          _line('Firestore DB: ${FirebaseConfig.firestoreDatabaseId}'),
          _line('Hosting Site: ${FirebaseConfig.hostingSiteId}'),
          const SizedBox(height: 8),
          _line('업로드 파일: ${analysisState.files.length}개'),
          _line('핵심 후보: ${analysisState.analyzedFiles.length}개'),
          _line('선택 파일: ${analysisState.selectedPaths.length}개'),
          const SizedBox(height: 8),
          _line('다이어그램 타입: ${editorState.previewType.name}'),
          _line('노드/관계: ${current?.nodes.length ?? 0}/${current?.relations.length ?? 0}'),
          _line('다이어그램 크기: ${sizeMb}MB'),
          _line('저장 상태: ${editorState.isSaving ? '저장중' : '대기'}'),
          if (analysisState.error != null)
            _line(
              '분석 오류: ${analysisState.error}',
              style: const TextStyle(color: Colors.red),
            ),
          if (editorState.error != null)
            _line(
              '편집 오류: ${editorState.error}',
              style: const TextStyle(color: Colors.red),
            ),
        ],
      ),
    );
  }

  Widget _line(String text, {TextStyle? style}) {
    return Text(
      text,
      maxLines: 1,
      softWrap: false,
      overflow: TextOverflow.ellipsis,
      style: style,
    );
  }
}

class _EditPanel extends StatelessWidget {
  const _EditPanel({
    required this.editorState,
    required this.controller,
  });

  final DiagramEditorState editorState;
  final DiagramEditorController controller;

  @override
  Widget build(BuildContext context) {
    final current = editorState.current;
    if (current == null) {
      return const Text('편집할 다이어그램이 없습니다.');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('노드 레이블 편집', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Expanded(
          child: ListView.builder(
            itemCount: current.nodes.length,
            itemBuilder: (context, index) {
              final node = current.nodes[index];
              return ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                title: Text(node.id, style: const TextStyle(fontSize: 12)),
                subtitle: Text(node.label),
                trailing: IconButton(
                  icon: const Icon(Icons.edit_outlined, size: 18),
                  onPressed: () => _showRenameDialog(context, node.id, node.label),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Future<void> _showRenameDialog(
    BuildContext context,
    String nodeId,
    String currentLabel,
  ) async {
    final textController = TextEditingController(text: currentLabel);
    await showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text('$nodeId 이름 수정'),
          content: TextField(
            controller: textController,
            decoration: const InputDecoration(border: OutlineInputBorder()),
            autofocus: true,
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('취소'),
            ),
            FilledButton(
              onPressed: () {
                controller.renameNode(nodeId, textController.text.trim());
                Navigator.of(context).pop();
              },
              child: const Text('적용'),
            ),
          ],
        );
      },
    );
    textController.dispose();
  }
}
