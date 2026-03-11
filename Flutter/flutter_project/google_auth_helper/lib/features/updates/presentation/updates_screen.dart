import 'package:file_selector/file_selector.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../models/release_watch_snapshot.dart';
import '../../../models/release_watch_target.dart';
import '../../../providers/app_providers.dart';

class UpdatesScreen extends ConsumerStatefulWidget {
  const UpdatesScreen({super.key});

  @override
  ConsumerState<UpdatesScreen> createState() => _UpdatesScreenState();
}

class _UpdatesScreenState extends ConsumerState<UpdatesScreen> {
  final TextEditingController _sheetNameController = TextEditingController();
  final TextEditingController _sheetUrlController = TextEditingController();
  final TextEditingController _sheetRuleController = TextEditingController();

  @override
  void dispose() {
    _sheetNameController.dispose();
    _sheetUrlController.dispose();
    _sheetRuleController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final snapshotAsync = ref.watch(releaseWatcherSnapshotProvider);
    final targetsState = ref.watch(releaseWatchTargetsControllerProvider);

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('릴리즈 감시', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                const Text(
                  '초기 버전은 Excel 업로드와 Google Sheet 링크를 받아 watcher 대상 초안을 관리합니다. 실제 주기 실행은 tools/release_watcher에서 담당합니다.',
                ),
                const SizedBox(height: 14),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    FilledButton.tonalIcon(
                      onPressed: _pickExcelTarget,
                      icon: const Icon(Icons.table_view_rounded),
                      label: const Text('Excel 업로드'),
                    ),
                    FilledButton.tonalIcon(
                      onPressed: () =>
                          ref.invalidate(releaseWatcherSnapshotProvider),
                      icon: const Icon(Icons.refresh_rounded),
                      label: const Text('Watcher 다시 읽기'),
                    ),
                  ],
                ),
                if (kIsWeb) ...[
                  const SizedBox(height: 10),
                  const Text(
                    '웹에서는 Excel 업로드가 브라우저 제약에 따라 실패할 수 있습니다. 이 경우 Google Sheet 링크 등록을 우선 사용합니다.',
                    style: TextStyle(
                      color: Color(0xFFB54708),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Google Sheet 링크 등록',
                    style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _sheetNameController,
                  decoration: const InputDecoration(labelText: '표시 이름'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _sheetUrlController,
                  decoration:
                      const InputDecoration(labelText: 'Google Sheet URL'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _sheetRuleController,
                  decoration: const InputDecoration(
                    labelText: '파싱 규칙 메모',
                    hintText: '예: sheet1!A:C / version, notes 컬럼 사용',
                  ),
                ),
                const SizedBox(height: 12),
                FilledButton.icon(
                  onPressed: _addGoogleSheetTarget,
                  icon: const Icon(Icons.add_link_rounded),
                  label: const Text('링크 등록'),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('감시 대상 초안', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                if (targetsState.isLoading)
                  const LinearProgressIndicator()
                else if (targetsState.targets.isEmpty)
                  const Text('등록된 감시 대상이 없습니다.')
                else
                  ...targetsState.targets.map(
                    (target) => _TargetTile(
                      target: target,
                      onRemove: () {
                        ref
                            .read(
                                releaseWatchTargetsControllerProvider.notifier)
                            .removeTarget(target.id);
                      },
                    ),
                  ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        snapshotAsync.when(
          data: (snapshot) => _WatcherSnapshotCard(snapshot: snapshot),
          loading: () => const Card(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: CircularProgressIndicator(),
            ),
          ),
          error: (error, _) => Card(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Text('Watcher artifact를 불러오지 못했습니다: $error'),
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _pickExcelTarget() async {
    const typeGroup = XTypeGroup(
      label: 'excel',
      extensions: ['xlsx', 'xls', 'csv'],
    );
    try {
      final file = await openFile(acceptedTypeGroups: const [typeGroup]);
      if (file == null) {
        return;
      }
      final target = ReleaseWatchTarget(
        id: 'excel-${DateTime.now().millisecondsSinceEpoch}',
        name: file.name,
        sourceType: ReleaseWatchSourceType.excel,
        sourceRef: file.path.isEmpty ? 'local:${file.name}' : file.path,
        parserRule: 'spreadsheet draft',
        status: 'draft',
      );
      await ref
          .read(releaseWatchTargetsControllerProvider.notifier)
          .addTarget(target);
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${file.name} 대상을 추가했습니다.')),
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      showDialog<void>(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: const Text('Excel 업로드 실패'),
            content: Text('$error'),
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

  Future<void> _addGoogleSheetTarget() async {
    final name = _sheetNameController.text.trim();
    final url = _sheetUrlController.text.trim();
    final parserRule = _sheetRuleController.text.trim();
    final uri = Uri.tryParse(url);
    final isGoogleSheet = uri != null &&
        (uri.host.contains('docs.google.com') ||
            uri.host.contains('google.com'));
    if (name.isEmpty || url.isEmpty || !isGoogleSheet) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('표시 이름과 유효한 Google Sheet 링크를 입력하세요.')),
      );
      return;
    }

    final target = ReleaseWatchTarget(
      id: 'gsheet-${DateTime.now().millisecondsSinceEpoch}',
      name: name,
      sourceType: ReleaseWatchSourceType.gsheet,
      sourceRef: url,
      parserRule: parserRule,
      status: 'draft',
    );
    await ref
        .read(releaseWatchTargetsControllerProvider.notifier)
        .addTarget(target);
    _sheetNameController.clear();
    _sheetUrlController.clear();
    _sheetRuleController.clear();
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('$name 링크를 등록했습니다.')),
    );
  }
}

class _TargetTile extends StatelessWidget {
  const _TargetTile({
    required this.target,
    required this.onRemove,
  });

  final ReleaseWatchTarget target;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      color: const Color(0xFFF7F9FF),
      child: ListTile(
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        title: Text(target.name),
        subtitle: Text(
          '${target.sourceType.label} | ${target.sourceRef}\n'
          '규칙: ${target.parserRule.isEmpty ? "(미정)" : target.parserRule}\n'
          '상태: ${target.status}',
        ),
        isThreeLine: true,
        trailing: IconButton(
          onPressed: onRemove,
          icon: const Icon(Icons.delete_outline_rounded),
        ),
      ),
    );
  }
}

class _WatcherSnapshotCard extends StatelessWidget {
  const _WatcherSnapshotCard({required this.snapshot});

  final ReleaseWatchSnapshot snapshot;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('최근 watcher artifact',
                    style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                _InfoLine('Source', snapshot.sourceLabel),
                _InfoLine('Version', snapshot.version),
                _InfoLine('Release Notes Hash', snapshot.releaseNotesHash),
                _InfoLine('Last Checked',
                    snapshot.lastCheckedAt.toLocal().toString()),
                _InfoLine(
                  'Last Uploaded',
                  snapshot.lastUploadedAt?.toLocal().toString() ??
                      '(not uploaded)',
                ),
                _InfoLine('Upload Status', snapshot.uploadStatus),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('감지된 변경', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                if (snapshot.changes.isEmpty)
                  const Text('변경이 감지되지 않았습니다.')
                else
                  ...snapshot.changes.map(
                    (change) => ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.update_rounded),
                      title: Text(change.kind),
                      subtitle: Text(change.summary),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ],
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
            width: 140,
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
