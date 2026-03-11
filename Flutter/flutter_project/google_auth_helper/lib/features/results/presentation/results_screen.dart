import 'package:file_selector/file_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as path;

import '../../../core/runtime/runtime_capabilities.dart';
import '../../../models/import_bundle.dart';
import '../../../models/import_source.dart';
import '../../../models/tool_config.dart';
import '../../../models/upload_target.dart';
import '../../../providers/app_providers.dart';

class ResultsScreen extends ConsumerWidget {
  const ResultsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(resultsControllerProvider);
    final controller = ref.read(resultsControllerProvider.notifier);
    final capabilities = ref.watch(runtimeCapabilitiesProvider);
    final bundle = state.previewBundle;

    if (capabilities.profile == RuntimePlatformProfile.webHosting) {
      return const _WebDisabledCard();
    }

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('결과 업로드', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 10),
                const Text('결과와 로그는 압축파일 또는 일반 폴더로 각각 올릴 수 있습니다.'),
                const SizedBox(height: 16),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    SizedBox(
                      width: 220,
                      child: DropdownButtonFormField<ToolType>(
                        initialValue: state.selectedTool,
                        decoration: const InputDecoration(labelText: '도구'),
                        items: ToolType.values
                            .map(
                              (toolType) => DropdownMenuItem(
                                value: toolType,
                                child: Text(toolType.label),
                              ),
                            )
                            .toList(growable: false),
                        onChanged: (value) {
                          if (value != null) {
                            controller.selectTool(value);
                          }
                        },
                      ),
                    ),
                    SizedBox(
                      width: 220,
                      child: DropdownButtonFormField<UploadTarget>(
                        initialValue: state.uploadTarget,
                        decoration: const InputDecoration(labelText: '업로드 대상'),
                        items: UploadTarget.values
                            .map(
                              (target) => DropdownMenuItem(
                                value: target,
                                child: Text(target.label),
                              ),
                            )
                            .toList(growable: false),
                        onChanged: (value) {
                          if (value != null) {
                            controller.selectUploadTarget(value);
                          }
                        },
                      ),
                    ),
                    FilledButton.icon(
                      onPressed: state.isUploading || !state.hasUploadablePreview
                          ? null
                          : controller.uploadPreview,
                      icon: const Icon(Icons.cloud_upload_rounded),
                      label: Text(state.isUploading ? '업로드 중...' : '업로드'),
                    ),
                    FilledButton.tonalIcon(
                      onPressed: controller.resetUploadState,
                      icon: const Icon(Icons.restart_alt_rounded),
                      label: const Text('리셋'),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    _SourceCard(
                      title: '결과 원본',
                      selectedName: state.resultArchiveName,
                      sourceKind: state.resultSourceKind,
                      isReady: state.resultArchiveLoaded,
                      isLoading: state.isLoading,
                      onPickZip: () => _pickZipAndImport(
                        context,
                        controller,
                        UploadArchiveSlot.result,
                      ),
                      onPickDirectory: () => _pickDirectoryAndImport(
                        context,
                        controller,
                        UploadArchiveSlot.result,
                      ),
                    ),
                    _SourceCard(
                      title: '로그 원본',
                      selectedName: state.logArchiveName,
                      sourceKind: state.logSourceKind,
                      isReady: state.logArchiveLoaded,
                      isLoading: state.isLoading,
                      onPickZip: () => _pickZipAndImport(
                        context,
                        controller,
                        UploadArchiveSlot.log,
                      ),
                      onPickDirectory: () => _pickDirectoryAndImport(
                        context,
                        controller,
                        UploadArchiveSlot.log,
                      ),
                    ),
                  ],
                ),
                if (state.message != null) ...[
                  const SizedBox(height: 14),
                  Text(
                    state.message!,
                    style: TextStyle(
                      color: state.loadError == null
                          ? const Color(0xFF1459FF)
                          : const Color(0xFFB42318),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
                if (state.loadError != null) ...[
                  const SizedBox(height: 14),
                  _ErrorBanner(message: state.loadError!),
                ],
                if (state.history.isNotEmpty) ...[
                  const SizedBox(height: 14),
                  Text('최근 업로드', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: state.history
                        .take(8)
                        .map((item) => Chip(label: Text(item)))
                        .toList(growable: false),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        _BundleStateCard(state: state, bundle: bundle),
        if (bundle != null) ...[
          const SizedBox(height: 12),
          _BundleSummaryCard(bundle: bundle),
          const SizedBox(height: 12),
          _UploadPreviewCard(
            target: state.uploadTarget,
            previewText: state.uploadPreviewText,
          ),
          const SizedBox(height: 12),
          _FailureEditorCard(bundle: bundle),
        ],
      ],
    );
  }

  Future<void> _pickZipAndImport(
    BuildContext context,
    ResultsController controller,
    UploadArchiveSlot slot,
  ) async {
    const typeGroup = XTypeGroup(
      label: 'zip',
      extensions: ['zip'],
    );
    try {
      final file = await openFile(acceptedTypeGroups: const [typeGroup]);
      if (file == null) {
        return;
      }
      controller.registerSelectedArchive(slot, file.name);
      try {
        final bytes = await file.readAsBytes();
        await controller.importArchivePart(
          slot: slot,
          fileName: file.name,
          bytes: bytes,
        );
      } catch (error) {
        controller.registerArchiveReadFailure(slot, file.name, error);
        if (!context.mounted) {
          return;
        }
        await _showUploadErrorDialog(
          context,
          title: '압축파일 읽기 실패',
          message: '선택한 압축파일을 읽지 못했습니다.\n$error',
        );
      }
    } catch (error) {
      if (!context.mounted) {
        return;
      }
      await _showUploadErrorDialog(
        context,
        title: '파일 선택 실패',
        message: '$error',
      );
    }
  }

  Future<void> _pickDirectoryAndImport(
    BuildContext context,
    ResultsController controller,
    UploadArchiveSlot slot,
  ) async {
    try {
      final directoryPath = await getDirectoryPath();
      if (directoryPath == null || directoryPath.isEmpty) {
        return;
      }
      final label = path.basename(directoryPath);
      controller.registerSelectedDirectory(slot, label);
      await controller.importDirectoryPart(
        slot: slot,
        directoryPath: directoryPath,
        label: label,
      );
    } catch (error) {
      if (!context.mounted) {
        return;
      }
      await _showUploadErrorDialog(
        context,
        title: '폴더 선택 실패',
        message: '$error',
      );
    }
  }

  Future<void> _showUploadErrorDialog(
    BuildContext context, {
    required String title,
    required String message,
  }) {
    return showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(title),
          content: Text(message),
          actions: [
            FilledButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('닫기'),
            ),
          ],
        );
      },
    );
  }
}

class _WebDisabledCard extends StatelessWidget {
  const _WebDisabledCard();

  @override
  Widget build(BuildContext context) {
    return const Card(
      child: Padding(
        padding: EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '웹에서는 결과 업로드를 지원하지 않습니다.',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w800,
              ),
            ),
            SizedBox(height: 10),
            Text('웹 버전은 조회 전용으로만 사용해 주세요.'),
          ],
        ),
      ),
    );
  }
}

class _SourceCard extends StatelessWidget {
  const _SourceCard({
    required this.title,
    required this.selectedName,
    required this.sourceKind,
    required this.isReady,
    required this.isLoading,
    required this.onPickZip,
    required this.onPickDirectory,
  });

  final String title;
  final String? selectedName;
  final ImportSourceKind? sourceKind;
  final bool isReady;
  final bool isLoading;
  final Future<void> Function() onPickZip;
  final Future<void> Function() onPickDirectory;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 420,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              Text(
                selectedName ?? '(선택 안 됨)',
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 6),
              Text('종류: ${_kindLabel(sourceKind)}'),
              const SizedBox(height: 14),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  FilledButton.tonalIcon(
                    onPressed: isLoading ? null : () => onPickZip(),
                    icon: const Icon(Icons.archive_rounded),
                    label: const Text('압축파일 선택'),
                  ),
                  FilledButton.tonalIcon(
                    onPressed: isLoading ? null : () => onPickDirectory(),
                    icon: const Icon(Icons.folder_open_rounded),
                    label: const Text('폴더 선택'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(
                    isReady ? Icons.check_circle_rounded : Icons.pending_outlined,
                    color:
                        isReady ? const Color(0xFF067647) : const Color(0xFF98A2B3),
                  ),
                  const SizedBox(width: 8),
                  Text(isReady ? '준비됨' : '대기 중'),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _kindLabel(ImportSourceKind? kind) {
    switch (kind) {
      case ImportSourceKind.archive:
        return '압축파일';
      case ImportSourceKind.directory:
        return '일반 폴더';
      case null:
        return '없음';
    }
  }
}

class _BundleStateCard extends StatelessWidget {
  const _BundleStateCard({
    required this.state,
    required this.bundle,
  });

  final ResultsState state;
  final ImportBundle? bundle;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('처리 상태', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            _InfoLine('결과 원본', state.resultArchiveName ?? '(없음)'),
            _InfoLine('로그 원본', state.logArchiveName ?? '(없음)'),
            _InfoLine('결과 준비', state.resultArchiveLoaded ? '예' : '아니오'),
            _InfoLine('로그 준비', state.logArchiveLoaded ? '예' : '아니오'),
            _InfoLine('처리 단계', _stageLabel(state.loadStage)),
            _InfoLine(
              '선택 시각',
              state.selectedAt?.toLocal().toString() ?? '(없음)',
            ),
            _InfoLine('결과 파일', bundle?.resultPath ?? '(없음)'),
            _InfoLine('로그 파일', bundle?.logPath ?? '(없음)'),
            if (state.previewWarnings.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text('경고', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 6),
              ...state.previewWarnings.map(
                (warning) => Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Text(
                    warning,
                    style: const TextStyle(
                      color: Color(0xFFB54708),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _stageLabel(ResultsLoadStage stage) {
    switch (stage) {
      case ResultsLoadStage.idle:
        return '대기';
      case ResultsLoadStage.selectingFile:
        return '선택 중';
      case ResultsLoadStage.fileLoaded:
        return '원본 준비';
      case ResultsLoadStage.parsing:
        return '파싱 중';
      case ResultsLoadStage.ready:
        return '준비 완료';
      case ResultsLoadStage.error:
        return '오류';
    }
  }
}

class _BundleSummaryCard extends StatelessWidget {
  const _BundleSummaryCard({required this.bundle});

  final ImportBundle bundle;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('요약', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 14),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _StatTile(label: '빌드', value: bundle.metric.compactBuildLabel),
                _StatTile(label: '통과', value: '${bundle.metric.passCount}'),
                _StatTile(label: '실패', value: '${bundle.activeFailedTests.length}'),
                _StatTile(label: '제외', value: '${bundle.excludedFailedTests.length}'),
                _StatTile(label: '장치 수', value: '${bundle.metric.devices.length}'),
                _StatTile(label: '모듈 수', value: '${bundle.metric.moduleCount}'),
              ],
            ),
            const SizedBox(height: 14),
            _InfoLine('스위트', '${bundle.metric.suiteName} ${bundle.metric.suiteVersion}'),
            _InfoLine('기기', bundle.metric.buildDevice.isEmpty ? '-' : bundle.metric.buildDevice),
            _InfoLine('안드로이드', bundle.metric.androidVersion.isEmpty ? '-' : bundle.metric.androidVersion),
            _InfoLine('빌드 타입', bundle.metric.buildType.isEmpty ? '-' : bundle.metric.buildType),
            _InfoLine('핑거프린트',
                bundle.metric.buildFingerprint.isEmpty ? '-' : bundle.metric.buildFingerprint),
            _InfoLine('집계 기준', bundle.metric.countSource),
          ],
        ),
      ),
    );
  }
}

class _UploadPreviewCard extends StatelessWidget {
  const _UploadPreviewCard({
    required this.target,
    required this.previewText,
  });

  final UploadTarget target;
  final String previewText;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    target == UploadTarget.redmine
                        ? '레드마인 업로드 본문'
                        : '파이어스토어 업로드 본문',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                FilledButton.tonalIcon(
                  onPressed: previewText.isEmpty
                      ? null
                      : () async {
                          await Clipboard.setData(ClipboardData(text: previewText));
                          if (!context.mounted) {
                            return;
                          }
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('본문을 복사했습니다.')),
                          );
                        },
                  icon: const Icon(Icons.copy_rounded),
                  label: const Text('복사'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFF0F1C36),
                borderRadius: BorderRadius.circular(18),
              ),
              child: SelectableText(
                previewText.isEmpty ? '미리보기가 없습니다.' : previewText,
                style: const TextStyle(
                  color: Color(0xFFD8E4FF),
                  fontFamily: 'Consolas',
                  fontSize: 12,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FailureEditorCard extends ConsumerWidget {
  const _FailureEditorCard({required this.bundle});

  final ImportBundle bundle;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = ref.read(resultsControllerProvider.notifier);
    final failures = bundle.failedTests.take(25).toList(growable: false);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    '실패 항목 편집',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                FilledButton.tonal(
                  onPressed: controller.resetFailureOverrides,
                  child: const Text('편집 초기화'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: DataTable(
                columns: const [
                  DataColumn(label: Text('제외')),
                  DataColumn(label: Text('모듈')),
                  DataColumn(label: Text('테스트')),
                  DataColumn(label: Text('메시지')),
                  DataColumn(label: Text('메모')),
                ],
                rows: failures.map((item) {
                  return DataRow(
                    color: WidgetStateProperty.resolveWith(
                      (_) => item.excluded
                          ? const Color(0x11B42318)
                          : Colors.transparent,
                    ),
                    cells: [
                      DataCell(
                        Checkbox(
                          value: item.excluded,
                          onChanged: (value) {
                            controller.toggleFailedTestExcluded(
                              item.id,
                              value ?? false,
                            );
                          },
                        ),
                      ),
                      DataCell(SizedBox(
                        width: 180,
                        child: Text(item.displayModuleName),
                      )),
                      DataCell(SizedBox(
                        width: 220,
                        child: Text(item.testName),
                      )),
                      DataCell(
                        SizedBox(
                          width: 360,
                          child: Text(
                            item.errorLogSnippet.isEmpty
                                ? item.failureMessage
                                : item.errorLogSnippet,
                            maxLines: 5,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ),
                      DataCell(
                        SizedBox(
                          width: 220,
                          child: TextFormField(
                            key: ValueKey('${item.id}::${item.manualMemo}'),
                            initialValue: item.manualMemo,
                            maxLines: 2,
                            onChanged: (value) {
                              controller.updateFailedTestMemo(item.id, value);
                            },
                            decoration: const InputDecoration(
                              isDense: true,
                              hintText: '메모',
                            ),
                          ),
                        ),
                      ),
                    ],
                  );
                }).toList(growable: false),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatTile extends StatelessWidget {
  const _StatTile({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 160,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: const Color(0xFFF7F9FF),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFFD8E3F4)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: const TextStyle(color: Color(0xFF5D6779))),
              const SizedBox(height: 8),
              Text(
                value.isEmpty ? '-' : value,
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _InfoLine extends StatelessWidget {
  const _InfoLine(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 130,
            child: Text(label, style: const TextStyle(color: Color(0xFF5D6779))),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFEE4E2),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFFDA29B)),
      ),
      child: Text(
        message,
        style: const TextStyle(
          color: Color(0xFFB42318),
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
