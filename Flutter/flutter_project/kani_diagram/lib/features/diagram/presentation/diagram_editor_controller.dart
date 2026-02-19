import 'dart:ui' as ui;

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/firebase_config.dart';
import '../../../core/services/diagram_exporter.dart';
import '../../analysis/data/code_parser_service.dart';
import '../../analysis/domain/source_code_file.dart';
import '../../analysis/presentation/project_analysis_controller.dart';
import '../data/firestore_diagram_repository.dart';
import '../domain/diagram_enums.dart';
import 'diagram_editor_state.dart';

final diagramRepositoryProvider = Provider<DiagramRepository>((ref) {
  try {
    final firestore = FirebaseFirestore.instanceFor(
      app: Firebase.app(),
      databaseId: FirebaseConfig.firestoreDatabaseId,
    );
    return FirestoreDiagramRepository(firestore);
  } catch (_) {
    return InMemoryDiagramRepository();
  }
});

final diagramEditorControllerProvider =
    StateNotifierProvider<DiagramEditorController, DiagramEditorState>((ref) {
  return DiagramEditorController(
    codeParser: ref.read(codeParserServiceProvider),
    repository: ref.read(diagramRepositoryProvider),
  );
});

class DiagramEditorController extends StateNotifier<DiagramEditorState> {
  DiagramEditorController({
    required CodeParserService codeParser,
    required DiagramRepository repository,
  })  : _codeParser = codeParser,
        _repository = repository,
        super(const DiagramEditorState());

  final CodeParserService _codeParser;
  final DiagramRepository _repository;

  void setPreviewType(DiagramType type) {
    state = state.copyWith(previewType: type);
    final current = state.current;
    if (current != null) {
      state = state.copyWith(current: current.copyWith(type: type));
    }
  }

  String? buildFromFiles(List<SourceCodeFile> files) {
    final filtered = state.validateInputForScale(files);
    if (filtered.isEmpty) {
      const message = '선택된 파일이 없습니다. 파일을 선택하거나 전체 선택을 눌러주세요.';
      state = state.copyWith(error: message);
      return message;
    }

    final diagram = _codeParser.buildDiagram(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      name: '분석 다이어그램 ${DateTime.now().toIso8601String()}',
      type: state.previewType,
      selectedFiles: filtered,
    );

    if (diagram.nodes.isEmpty) {
      const message = '노드를 추출하지 못했습니다. 선택 파일을 바꾸거나 범위를 넓혀주세요.';
      state = state.copyWith(error: message);
      return message;
    }

    state = state.copyWith(current: diagram, clearError: true);
    return null;
  }

  void renameNode(String nodeId, String newLabel) {
    final current = state.current;
    if (current == null) return;

    final updatedNodes = current.nodes
        .map((node) => node.id == nodeId ? node.copyWith(label: newLabel) : node)
        .toList();
    state = state.copyWith(current: current.copyWith(nodes: updatedNodes));
  }

  Future<void> saveCurrentDiagram() async {
    final current = state.current;
    if (current == null) return;

    try {
      state = state.copyWith(isSaving: true, clearError: true);
      await _repository.saveDiagram(current);
      final recent = await _repository.loadRecentDiagrams();
      state = state.copyWith(isSaving: false, history: recent);
    } catch (e) {
      state = state.copyWith(
        isSaving: false,
        error: '저장 중 오류가 발생했습니다: $e',
      );
    }
  }

  Future<void> exportDiagram({
    required GlobalKey repaintKey,
    required bool asJpg,
  }) async {
    final current = state.current;
    if (current == null) return;

    final boundary = repaintKey.currentContext?.findRenderObject() as RenderRepaintBoundary?;
    if (boundary == null) {
      state = state.copyWith(error: '내보내기 대상을 찾을 수 없습니다.');
      return;
    }

    final image = await boundary.toImage(pixelRatio: 2.0);
    final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    if (byteData == null) {
      state = state.copyWith(error: '이미지 변환에 실패했습니다.');
      return;
    }

    await exportDiagramImage(
      pngBytes: byteData.buffer.asUint8List(),
      fileName: current.id,
      asJpg: asJpg,
    );
  }
}
