import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/firebase_config.dart';
import '../../../core/theme/theme_mode_provider.dart';
import '../../analysis/presentation/project_analysis_controller.dart';
import '../../analysis/presentation/project_analysis_state.dart';
import 'diagram_editor_controller.dart';
import 'diagram_editor_state.dart';
import 'widgets/diagram_preview_canvas.dart';
import 'widgets/diagram_type_selector.dart';

class DiagramWorkspacePage extends ConsumerStatefulWidget {
  const DiagramWorkspacePage({super.key});

  @override
  ConsumerState<DiagramWorkspacePage> createState() =>
      _DiagramWorkspacePageState();
}

class _DiagramWorkspacePageState extends ConsumerState<DiagramWorkspacePage> {
  final GlobalKey _previewKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(diagramEditorControllerProvider.notifier).refreshHistory();
    });
  }

  @override
  Widget build(BuildContext context) {
    final analysisState = ref.watch(projectAnalysisControllerProvider);
    final analysisController = ref.read(
      projectAnalysisControllerProvider.notifier,
    );

    final editorState = ref.watch(diagramEditorControllerProvider);
    final editorController = ref.read(diagramEditorControllerProvider.notifier);
    final appThemeMode = ref.watch(appThemeModeProvider);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final canGenerateDiagram =
        !analysisState.isLoading && analysisState.selectedPaths.isNotEmpty;

    return Scaffold(
      appBar: AppBar(
        title: const Text('코드 다이어그램 분석기'),
        actions: [
          IconButton(
            tooltip: isDark ? '라이트 모드' : '다크 모드',
            onPressed: () {
              final next = appThemeMode == ThemeMode.dark
                  ? ThemeMode.light
                  : ThemeMode.dark;
              ref.read(appThemeModeProvider.notifier).state = next;
            },
            icon: Icon(isDark ? Icons.light_mode : Icons.dark_mode),
          ),
          TextButton.icon(
            onPressed: editorState.isSaving
                ? null
                : editorController.saveCurrentDiagram,
            icon: const Icon(Icons.cloud_upload_outlined),
            label: const Text('Firestore 저장'),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: isDark
                ? const [Color(0xFF111827), Color(0xFF0F172A)]
                : const [Color(0xFFF3FCF9), Color(0xFFF8FAFC)],
          ),
        ),
        child: LayoutBuilder(
          builder: (context, constraints) {
            final isCompact = constraints.maxWidth < 1100;

            if (isCompact) {
              return ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  SizedBox(
                    height: 420,
                    child: _buildAnalysisCard(
                      analysisState,
                      analysisController,
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    height: 520,
                    child: _buildDiagramCard(
                      analysisController: analysisController,
                      editorController: editorController,
                      editorState: editorState,
                      canGenerateDiagram: canGenerateDiagram,
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    height: 540,
                    child: _buildSideCard(
                      analysisState: analysisState,
                      editorState: editorState,
                      editorController: editorController,
                    ),
                  ),
                ],
              );
            }

            return Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    flex: 3,
                    child: _buildAnalysisCard(
                      analysisState,
                      analysisController,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    flex: 5,
                    child: _buildDiagramCard(
                      analysisController: analysisController,
                      editorController: editorController,
                      editorState: editorState,
                      canGenerateDiagram: canGenerateDiagram,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    flex: 2,
                    child: _buildSideCard(
                      analysisState: analysisState,
                      editorState: editorState,
                      editorController: editorController,
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildAnalysisCard(
    ProjectAnalysisState analysisState,
    ProjectAnalysisController analysisController,
  ) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '1) 파일 분석 대상 선택',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 10),
            FilledButton.icon(
              onPressed: analysisState.isLoading
                  ? null
                  : analysisController.pickProjectFolder,
              icon: const Icon(Icons.folder_open),
              label: const Text('프로젝트 폴더 업로드'),
            ),
            const SizedBox(height: 8),
            const Text(
              '업로드 파일은 임시 메모리에만 저장되며 생성 성공 시 자동 삭제됩니다.',
              style: TextStyle(fontSize: 12, color: Color(0xFF475467)),
            ),
            const SizedBox(height: 12),
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
                Chip(
                  visualDensity: VisualDensity.compact,
                  label: Text(
                    '선택 ${analysisState.selectedPaths.length}/${analysisState.analyzedFiles.length}',
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Expanded(
              child: ListView.builder(
                itemCount: analysisState.analyzedFiles.length,
                itemBuilder: (context, index) {
                  final item = analysisState.analyzedFiles[index];
                  final selected = analysisState.selectedPaths.contains(
                    item.source.path,
                  );
                  return CheckboxListTile(
                    value: selected,
                    dense: true,
                    title: Text(item.source.name),
                    subtitle: Text(
                      '점수 ${item.score} | ${item.reasons.join(', ')}',
                    ),
                    onChanged: (_) =>
                        analysisController.toggleSelection(item.source.path),
                  );
                },
              ),
            ),
            if (analysisState.error != null)
              Text(
                analysisState.error!,
                style: const TextStyle(color: Colors.red),
              ),
            if (analysisState.notice != null)
              Text(
                analysisState.notice!,
                style: const TextStyle(color: Color(0xFF067647)),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildDiagramCard({
    required ProjectAnalysisController analysisController,
    required DiagramEditorController editorController,
    required DiagramEditorState editorState,
    required bool canGenerateDiagram,
  }) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '2) 다이어그램 생성 및 미리보기',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                FilledButton.icon(
                  onPressed: canGenerateDiagram
                      ? () {
                          final selected = analysisController
                              .selectedFilesForDiagram();
                          final error = editorController.buildFromFiles(
                            selected,
                          );
                          if (error == null) {
                            analysisController
                                .clearTemporaryAfterDiagramSuccess(
                                  selected.length,
                                );
                          } else {
                            analysisController.setNotice('생성 실패: $error');
                            ScaffoldMessenger.of(
                              context,
                            ).showSnackBar(SnackBar(content: Text(error)));
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
            const SizedBox(height: 8),
            Row(
              children: [
                const Text(
                  '미리보기 배율',
                  style: TextStyle(fontSize: 12, color: Color(0xFF475467)),
                ),
                Expanded(
                  child: Slider(
                    value: editorState.previewScale,
                    min: 0.5,
                    max: 1.8,
                    divisions: 13,
                    label: '${(editorState.previewScale * 100).round()}%',
                    onChanged: editorController.setPreviewScale,
                  ),
                ),
                Text(
                  '${(editorState.previewScale * 100).round()}%',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                FilterChip(
                  label: const Text('external 숨김'),
                  selected: editorState.hideExternalNodes,
                  onSelected: editorController.setHideExternalNodes,
                ),
                FilterChip(
                  label: const Text('interface 숨김'),
                  selected: editorState.hideInterfaceNodes,
                  onSelected: editorController.setHideInterfaceNodes,
                ),
                SizedBox(
                  width: 220,
                  child: Row(
                    children: [
                      const Text(
                        '최대 노드',
                        style: TextStyle(
                          fontSize: 12,
                          color: Color(0xFF475467),
                        ),
                      ),
                      Expanded(
                        child: Slider(
                          value: editorState.maxPreviewNodes.toDouble(),
                          min: 20,
                          max: 300,
                          divisions: 14,
                          label: '${editorState.maxPreviewNodes}',
                          onChanged: editorController.setMaxPreviewNodes,
                        ),
                      ),
                      Text(
                        '${editorState.maxPreviewNodes}',
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
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
                      ? const Center(
                          child: Text('다이어그램을 생성하면 이곳에서 미리보기가 표시됩니다.'),
                        )
                      : DiagramPreviewCanvas(
                          diagram: editorState.current!,
                          previewScale: editorState.previewScale,
                          hideExternalNodes: editorState.hideExternalNodes,
                          hideInterfaceNodes: editorState.hideInterfaceNodes,
                          maxPreviewNodes: editorState.maxPreviewNodes,
                        ),
                ),
              ),
            ),
            if (editorState.error != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  editorState.error!,
                  style: const TextStyle(color: Colors.red),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSideCard({
    required ProjectAnalysisState analysisState,
    required DiagramEditorState editorState,
    required DiagramEditorController editorController,
  }) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _DebugPanel(analysisState: analysisState, editorState: editorState),
            const SizedBox(height: 12),
            _HistoryPanel(
              editorState: editorState,
              controller: editorController,
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
    );
  }
}

class _DebugPanel extends StatelessWidget {
  const _DebugPanel({required this.analysisState, required this.editorState});

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
          const Text(
            '디버그/진행 상태',
            style: TextStyle(fontWeight: FontWeight.w700),
          ),
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
          _line('미리보기 배율: ${(editorState.previewScale * 100).round()}%'),
          _line('external 숨김: ${editorState.hideExternalNodes ? 'on' : 'off'}'),
          _line(
            'interface 숨김: ${editorState.hideInterfaceNodes ? 'on' : 'off'}',
          ),
          _line('최대 노드: ${editorState.maxPreviewNodes}'),
          _line(
            '노드/관계: ${current?.nodes.length ?? 0}/${current?.relations.length ?? 0}',
          ),
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

class _HistoryPanel extends StatelessWidget {
  const _HistoryPanel({required this.editorState, required this.controller});

  final DiagramEditorState editorState;
  final DiagramEditorController controller;

  @override
  Widget build(BuildContext context) {
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
          Row(
            children: [
              const Expanded(
                child: Text(
                  '최근 저장 이력',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
              IconButton(
                tooltip: '새로고침',
                visualDensity: VisualDensity.compact,
                onPressed: controller.refreshHistory,
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          if (editorState.history.isEmpty)
            const Padding(
              padding: EdgeInsets.only(top: 2),
              child: Text(
                '저장된 이력이 없습니다.',
                style: TextStyle(fontSize: 12, color: Color(0xFF475467)),
              ),
            )
          else
            ...editorState.history.take(3).map((item) {
              final isCurrent = editorState.current?.id == item.id;
              return ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                title: Text(
                  item.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                subtitle: Text(
                  '${item.type.name} • ${(item.sizeInBytes / (1024 * 1024)).toStringAsFixed(2)}MB',
                ),
                trailing: TextButton(
                  onPressed: isCurrent
                      ? null
                      : () => controller.openFromHistory(item.id),
                  child: Text(isCurrent ? '현재' : '불러오기'),
                ),
              );
            }),
        ],
      ),
    );
  }
}

class _EditPanel extends StatelessWidget {
  const _EditPanel({required this.editorState, required this.controller});

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
                  onPressed: () =>
                      _showRenameDialog(context, node.id, node.label),
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
