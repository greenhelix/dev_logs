import 'dart:convert';

import 'package:file_selector/file_selector.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../models/import_bundle.dart';
import '../../../models/tool_config.dart';
import '../../../models/upload_target.dart';
import '../../../providers/app_providers.dart';

class ResultsScreen extends ConsumerWidget {
  const ResultsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(resultsControllerProvider);
    final settings = ref.watch(appSettingsControllerProvider).settings;
    final controller = ref.read(resultsControllerProvider.notifier);
    final config = settings.toolConfigFor(state.selectedTool);
    final bundle = state.previewBundle;

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
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
                    FilledButton.tonalIcon(
                      onPressed:
                          state.isLoading ? null : controller.loadPreview,
                      icon: const Icon(Icons.preview_rounded),
                      label: const Text('미리보기 불러오기'),
                    ),
                    FilledButton.icon(
                      onPressed:
                          state.isUploading || !state.hasUploadablePreview
                              ? null
                              : controller.uploadPreview,
                      icon: const Icon(Icons.cloud_upload_rounded),
                      label: Text(
                        state.isUploading
                            ? '업로드 중...'
                            : state.uploadTarget.actionLabel,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                _UploadDropZone(
                  isLoading: state.isLoading,
                  onTap: () => _pickZipAndImport(context, controller),
                ),
                const SizedBox(height: 16),
                _InfoLine('도구 루트',
                    config.toolRoot.isEmpty ? '(empty)' : config.toolRoot),
                _InfoLine(
                  '결과 경로',
                  config.resultsDir.isEmpty ? '(empty)' : config.resultsDir,
                ),
                _InfoLine('로그 경로',
                    config.logsDir.isEmpty ? '(empty)' : config.logsDir),
                _InfoLine(
                  '선택된 zip',
                  state.selectedArchiveName ??
                      state.lastArchiveName ??
                      '(none)',
                ),
                _InfoLine('로드 단계', state.loadStage.name),
                _InfoLine(
                  '웹 정책',
                  kIsWeb
                      ? '브라우저 제약으로 업로드가 실패할 수 있으며, 실패 시 경고창을 표시합니다.'
                      : '데스크톱 파일 선택을 사용합니다.',
                ),
                if (state.message != null) ...[
                  const SizedBox(height: 12),
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
                  const SizedBox(height: 12),
                  _ErrorBanner(message: state.loadError!),
                ],
                if (state.history.isNotEmpty) ...[
                  const SizedBox(height: 16),
                  Text('업로드 기록',
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: state.history
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
          _PreviewTabs(bundle: bundle, markdown: state.redmineMarkdown),
          const SizedBox(height: 12),
          _FailureEditorCard(bundle: bundle),
        ],
      ],
    );
  }

  Future<void> _pickZipAndImport(
    BuildContext context,
    ResultsController controller,
  ) async {
    const typeGroup = XTypeGroup(
      label: 'zip',
      extensions: ['zip'],
    );
    try {
      final file = await openFile(acceptedTypeGroups: [typeGroup]);
      if (file == null) {
        return;
      }
      controller.registerSelectedArchive(file.name);
      try {
        final bytes = await file.readAsBytes();
        await controller.importArchive(
          fileName: file.name,
          bytes: bytes,
        );
      } catch (error) {
        controller.registerArchiveReadFailure(file.name, error);
        if (!context.mounted) {
          return;
        }
        await _showUploadErrorDialog(
          context,
          title: 'zip 읽기 실패',
          message: '파일은 선택되었지만 내용을 읽지 못했습니다.\n$error',
        );
      }
    } catch (error) {
      if (!context.mounted) {
        return;
      }
      await _showUploadErrorDialog(
        context,
        title: kIsWeb ? '브라우저 파일 접근 실패' : '파일 선택 실패',
        message: kIsWeb
            ? '브라우저가 파일 접근을 막았거나 선택기가 실패했습니다.\n$error'
            : '파일 선택기를 열지 못했습니다.\n$error',
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

class _UploadDropZone extends StatelessWidget {
  const _UploadDropZone({
    required this.onTap,
    required this.isLoading,
  });

  final Future<void> Function() onTap;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: isLoading ? null : onTap,
      borderRadius: BorderRadius.circular(24),
      child: Ink(
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFFF4F8FF), Color(0xFFE8F0FF)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: const Color(0xFFB8CCF3)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Row(
            children: [
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  color: const Color(0xFF1459FF),
                  borderRadius: BorderRadius.circular(18),
                ),
                child: const Icon(Icons.folder_zip_rounded,
                    color: Colors.white, size: 28),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('zip 업로드',
                        style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 6),
                    Text(
                      kIsWeb
                          ? '웹은 best-effort 업로드입니다. 브라우저가 파일 접근을 막으면 경고창으로 안내합니다.'
                          : '데스크톱은 파싱 실패여도 선택된 파일 상태를 유지합니다.',
                    ),
                  ],
                ),
              ),
              const Icon(Icons.upload_file_rounded,
                  size: 30, color: Color(0xFF1459FF)),
            ],
          ),
        ),
      ),
    );
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
            Text('파일 상태', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            _InfoLine('선택된 zip', state.selectedArchiveName ?? '(none)'),
            _InfoLine(
              '선택 시각',
              state.selectedAt?.toLocal().toString() ?? '(none)',
            ),
            _InfoLine('로드 단계', state.loadStage.name),
            _InfoLine(
              '원본 zip 로드',
              state.rawArchiveAvailable ? 'yes' : 'no',
            ),
            _InfoLine(
              '마지막 정상 미리보기',
              bundle?.resultPath ?? '(none)',
            ),
            _InfoLine(
              '현재 빌드',
              bundle?.metric.primaryBuildLabel ?? '(none)',
            ),
            _InfoLine('현재 오류', state.loadError ?? '(none)'),
            if (state.previewWarnings.isNotEmpty) ...[
              const SizedBox(height: 8),
              ...state.previewWarnings.map(
                (warning) => Padding(
                  padding: const EdgeInsets.only(bottom: 6),
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
            Text('Summary', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 14),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _StatTile(label: 'Build', value: bundle.metric.compactBuildLabel),
                _StatTile(label: 'FW', value: bundle.metric.fwVersion),
                _StatTile(label: 'Device', value: bundle.metric.buildDevice),
                _StatTile(
                    label: 'Android', value: bundle.metric.androidVersion),
                _StatTile(label: 'Type', value: bundle.metric.buildType),
                _StatTile(
                    label: 'Fail (Raw)', value: '${bundle.metric.failCount}'),
                _StatTile(
                    label: 'Excluded',
                    value: '${bundle.excludedFailedTests.length}'),
                _StatTile(
                    label: 'Upload Fail',
                    value: '${bundle.activeFailedTests.length}'),
              ],
            ),
            const SizedBox(height: 16),
            _InfoLine('Suite',
                '${bundle.metric.suiteName} ${bundle.metric.suiteVersion}'),
            _InfoLine('Devices', bundle.metric.devices.join(', ')),
            _InfoLine(
              'Fingerprint',
              bundle.metric.buildFingerprint.isEmpty
                  ? '(none)'
                  : bundle.metric.buildFingerprint,
            ),
            _InfoLine('Count Source', bundle.metric.countSource),
            _InfoLine('Result File', bundle.resultPath),
            _InfoLine('Log File', bundle.logPath ?? '(none)'),
          ],
        ),
      ),
    );
  }
}

class _PreviewTabs extends StatelessWidget {
  const _PreviewTabs({
    required this.bundle,
    required this.markdown,
  });

  final ImportBundle bundle;
  final String markdown;

  @override
  Widget build(BuildContext context) {
    final firestoreJson = const JsonEncoder.withIndent('  ').convert({
      'metric': bundle.metric
          .copyWith(
            excludedFailureCount: bundle.excludedFailedTests.length,
          )
          .toMap(),
      'testCases':
          bundle.testCases.map((item) => item.toMap()).toList(growable: false),
      'failedTests': bundle.activeFailedTests
          .map((item) => item.toMap())
          .toList(growable: false),
      'warnings': bundle.previewWarnings,
    });

    return DefaultTabController(
      length: 2,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Upload Preview',
                  style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 12),
              const TabBar(
                tabs: [
                  Tab(text: 'Redmine'),
                  Tab(text: 'Firestore'),
                ],
              ),
              const SizedBox(height: 12),
              SizedBox(
                height: 360,
                child: TabBarView(
                  children: [
                    _PreviewPane(text: markdown),
                    _PreviewPane(text: firestoreJson),
                  ],
                ),
              ),
            ],
          ),
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
                    'Failure Editor',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                Text(
                  'Showing ${failures.length} / ${bundle.failedTests.length}',
                  style: const TextStyle(color: Color(0xFF5D6779)),
                ),
                const SizedBox(width: 12),
                FilledButton.tonal(
                  onPressed: controller.resetFailureOverrides,
                  child: const Text('Reset'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: DataTable(
                columns: const [
                  DataColumn(label: Text('Exclude')),
                  DataColumn(label: Text('Display Module')),
                  DataColumn(label: Text('Method')),
                  DataColumn(label: Text('XTS Module')),
                  DataColumn(label: Text('Snippet')),
                  DataColumn(label: Text('Memo')),
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
                          width: 180, child: Text(item.displayModuleName))),
                      DataCell(
                          SizedBox(width: 220, child: Text(item.testName))),
                      DataCell(SizedBox(
                          width: 180, child: Text(item.suiteModuleName))),
                      DataCell(
                        SizedBox(
                          width: 360,
                          child: Text(
                            item.errorLogSnippet.isEmpty
                                ? item.failureMessage
                                : item.errorLogSnippet,
                            maxLines: 6,
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
                              hintText: 'Optional memo',
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

class _PreviewPane extends StatelessWidget {
  const _PreviewPane({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFF0F1C36),
        borderRadius: BorderRadius.circular(18),
      ),
      child: SelectableText(
        text,
        style: const TextStyle(
          color: Color(0xFFD8E4FF),
          fontFamily: 'Consolas',
          fontSize: 12,
        ),
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
            child:
                Text(label, style: const TextStyle(color: Color(0xFF5D6779))),
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
