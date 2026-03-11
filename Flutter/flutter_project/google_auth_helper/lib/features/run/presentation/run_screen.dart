import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../models/tool_config.dart';
import '../../../providers/app_providers.dart';

class RunScreen extends ConsumerStatefulWidget {
  const RunScreen({super.key});

  @override
  ConsumerState<RunScreen> createState() => _RunScreenState();
}

class _RunScreenState extends ConsumerState<RunScreen> {
  ToolConfig? _draftConfig;
  String _lastConfigJson = '';

  @override
  Widget build(BuildContext context) {
    final capabilities = ref.watch(runtimeCapabilitiesProvider);
    final runState = ref.watch(runControllerProvider);
    final controller = ref.read(runControllerProvider.notifier);
    final settings = ref.watch(appSettingsControllerProvider).settings;
    final config = settings.toolConfigFor(runState.selectedTool);
    final serialized = jsonEncode(config.toJson());
    if (_draftConfig == null || _lastConfigJson != serialized) {
      _draftConfig = config;
      _lastConfigJson = serialized;
    }

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('자동 테스트', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                Text(
                  capabilities.canRunTests
                      ? '명령, 샤드, 기기 시리얼을 설정하고 자동 실행을 관리합니다.'
                      : '이 플랫폼에서는 실행은 지원하지 않지만, 도구 프로필과 기본 실행값은 미리 관리할 수 있습니다.',
                ),
                const SizedBox(height: 14),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    SizedBox(
                      width: 220,
                      child: DropdownButtonFormField<ToolType>(
                        initialValue: runState.selectedTool,
                        decoration: const InputDecoration(labelText: '도구'),
                        items: ToolType.values
                            .map(
                              (toolType) => DropdownMenuItem(
                                value: toolType,
                                child: Text(toolType.label),
                              ),
                            )
                            .toList(growable: false),
                        onChanged: runState.isRunning
                            ? null
                            : (value) {
                                if (value == null) {
                                  return;
                                }
                                controller.selectTool(value);
                              },
                      ),
                    ),
                    FilledButton.icon(
                      onPressed: runState.isRunning || !capabilities.canRunTests
                          ? null
                          : controller.startRun,
                      icon: const Icon(Icons.play_arrow_rounded),
                      label: const Text('실행'),
                    ),
                    FilledButton.tonalIcon(
                      onPressed: runState.isRunning ? controller.stopRun : null,
                      icon: const Icon(Icons.stop_circle_outlined),
                      label: const Text('중지'),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                TextFormField(
                  key: ValueKey('run-command-${runState.selectedTool.name}'),
                  initialValue: runState.command,
                  decoration: const InputDecoration(labelText: '테스트 명령'),
                  onChanged: controller.updateCommand,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  key: ValueKey('run-serials-${runState.selectedTool.name}'),
                  initialValue: runState.deviceSerials.join(', '),
                  decoration: const InputDecoration(
                    labelText: '기기 시리얼',
                    helperText: '쉼표 또는 공백으로 여러 시리얼을 구분합니다.',
                  ),
                  onChanged: controller.updateSerials,
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: Slider(
                        value: runState.shardCount.toDouble(),
                        min: 1,
                        max: 8,
                        divisions: 7,
                        label: '${runState.shardCount}',
                        onChanged: runState.isRunning
                            ? null
                            : (value) =>
                                controller.updateShardCount(value.round()),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Text('샤드 ${runState.shardCount}'),
                  ],
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  value: runState.autoUploadAfterRun,
                  onChanged:
                      runState.isRunning ? null : controller.updateAutoUpload,
                  title: const Text('실행 후 자동 업로드'),
                ),
                if (runState.message != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(
                      runState.message!,
                      style: TextStyle(
                        color: runState.stage.name == 'error'
                            ? const Color(0xFFB42318)
                            : const Color(0xFF1459FF),
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        _ToolProfileCard(
          config: _draftConfig!,
          onChanged: (updated) {
            setState(() {
              _draftConfig = updated;
            });
          },
          onSave: _saveToolProfile,
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('실행 상태', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                _StatusLine('단계', runState.stage.name),
                _StatusLine('자동 업로드', runState.isUploading ? '진행 중' : '대기'),
                _StatusLine('결과 경로', runState.detectedResultsDir ?? '(미확인)'),
                _StatusLine('로그 경로', runState.detectedLogsDir ?? '(미확인)'),
                _StatusLine(
                  '종료 코드',
                  runState.exitCode == null ? '(없음)' : '${runState.exitCode}',
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
                Text('실시간 로그', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: const Color(0xFF09142C),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: SelectableText(
                    runState.latestLogs.isEmpty
                        ? '로그가 아직 없습니다.'
                        : runState.latestLogs.join('\n'),
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
        ),
      ],
    );
  }

  Future<void> _saveToolProfile() async {
    final draft = _draftConfig;
    if (draft == null) {
      return;
    }
    await ref
        .read(appSettingsControllerProvider.notifier)
        .updateToolConfig(draft);
    ref.read(runControllerProvider.notifier).syncToolConfig();
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('${draft.toolType.label} 프로필을 저장했습니다.')),
    );
  }
}

class _ToolProfileCard extends StatelessWidget {
  const _ToolProfileCard({
    required this.config,
    required this.onChanged,
    required this.onSave,
  });

  final ToolConfig config;
  final ValueChanged<ToolConfig> onChanged;
  final Future<void> Function() onSave;

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
                  child: Text('도구 프로필',
                      style: Theme.of(context).textTheme.titleLarge),
                ),
                FilledButton.tonalIcon(
                  onPressed: onSave,
                  icon: const Icon(Icons.save_rounded),
                  label: const Text('프로필 저장'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            _Field(
              label: '도구 루트',
              initialValue: config.toolRoot,
              onChanged: (value) => onChanged(config.copyWith(toolRoot: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: '결과 경로',
              initialValue: config.resultsDir,
              onChanged: (value) =>
                  onChanged(config.copyWith(resultsDir: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: '로그 경로',
              initialValue: config.logsDir,
              onChanged: (value) => onChanged(config.copyWith(logsDir: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: '기본 명령',
              initialValue: config.defaultCommand,
              onChanged: (value) =>
                  onChanged(config.copyWith(defaultCommand: value)),
            ),
          ],
        ),
      ),
    );
  }
}

class _Field extends StatelessWidget {
  const _Field({
    required this.label,
    required this.initialValue,
    required this.onChanged,
  });

  final String label;
  final String initialValue;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      key: ValueKey('$label::$initialValue'),
      initialValue: initialValue,
      decoration: InputDecoration(labelText: label),
      onChanged: onChanged,
    );
  }
}

class _StatusLine extends StatelessWidget {
  const _StatusLine(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          SizedBox(
            width: 120,
            child:
                Text(label, style: const TextStyle(color: Color(0xFF5D6779))),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}
