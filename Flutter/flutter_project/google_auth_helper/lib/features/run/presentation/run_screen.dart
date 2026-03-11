import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../models/connected_adb_device.dart';
import '../../../models/console_health.dart';
import '../../../models/live_status.dart';
import '../../../models/run_session_state.dart';
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
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      final capabilities = ref.read(runtimeCapabilitiesProvider);
      if (!capabilities.canRunTests) {
        return;
      }
      ref.read(runControllerProvider.notifier).refreshDevices();
    });
  }

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

    if (!capabilities.canRunTests) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text('이 플랫폼에서는 자동 테스트 실행을 지원하지 않습니다.'),
        ),
      );
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
                const Text(
                  '콘솔 시작으로 tradefed 프롬프트만 먼저 확인한 뒤, 실행 버튼으로 실제 명령을 보냅니다.',
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
                                if (value != null) {
                                  controller.selectTool(value);
                                }
                              },
                      ),
                    ),
                    FilledButton.tonalIcon(
                      onPressed: runState.isRefreshingDevices || runState.isRunning
                          ? null
                          : controller.refreshDevices,
                      icon: const Icon(Icons.usb_rounded),
                      label: Text(
                        runState.isRefreshingDevices ? '새로고침 중...' : '장치 새로고침',
                      ),
                    ),
                    FilledButton.tonalIcon(
                      onPressed: runState.isConsoleReady || runState.isRunning
                          ? null
                          : controller.startConsole,
                      icon: const Icon(Icons.play_circle_outline_rounded),
                      label: const Text('콘솔 시작'),
                    ),
                    FilledButton.icon(
                      onPressed: runState.isRunning || !runState.isConsoleReady
                          ? null
                          : controller.startRun,
                      icon: const Icon(Icons.send_rounded),
                      label: const Text('실행'),
                    ),
                    FilledButton.tonalIcon(
                      onPressed:
                          runState.isConsoleReady || runState.isRunning ? controller.stopRun : null,
                      icon: const Icon(Icons.stop_circle_outlined),
                      label: const Text('중지'),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
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
                        color: runState.stage == RunStage.error
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
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('장치 선택', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                if (runState.availableDevices.isEmpty)
                  const Text('ADB 장치를 찾지 못했습니다.')
                else
                  ...runState.availableDevices.map(
                    (device) => _DeviceTile(
                      device: device,
                      selected:
                          runState.selectedDeviceSerials.contains(device.serial),
                      enabled: device.isReady && !runState.isRunning,
                      onChanged: (value) {
                        controller.toggleDeviceSelection(
                          device.serial,
                          value ?? false,
                        );
                      },
                    ),
                  ),
                const SizedBox(height: 16),
                _ReadOnlyField(
                  label: '생성된 실행 명령',
                  value: runState.generatedCommand.isEmpty
                      ? '(실행 가능한 장치를 먼저 선택해 주세요)'
                      : runState.generatedCommand,
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
                _StatusLine('단계', _stageLabel(runState)),
                _StatusLine(
                  '콘솔 상태',
                  _consoleStatusLabel(runState.consoleHealth.status),
                ),
                _StatusLine('콘솔 상세', runState.consoleHealth.message),
                _StatusLine(
                  '선택 장치',
                  runState.selectedDeviceSerials.isEmpty
                      ? '(없음)'
                      : runState.selectedDeviceSerials.join(', '),
                ),
                _StatusLine('샤드 수', '${runState.shardCount}'),
                _StatusLine(
                  '자동 업로드',
                  runState.autoUploadAfterRun ? '사용' : '미사용',
                ),
                _StatusLine('결과 경로', runState.detectedResultsDir ?? '(미설정)'),
                _StatusLine('로그 경로', runState.detectedLogsDir ?? '(미설정)'),
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
                        ? '아직 로그가 없습니다.'
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

  String _stageLabel(RunSessionState state) {
    if (state.isConsoleReady && !state.isRunning) {
      return '콘솔 준비 완료';
    }
    switch (state.stage) {
      case RunStage.idle:
        return '대기';
      case RunStage.queued:
        return '명령 전송';
      case RunStage.starting:
        return '시작 중';
      case RunStage.sharding:
        return '샤딩';
      case RunStage.running:
        return '실행 중';
      case RunStage.finished:
        return '완료';
      case RunStage.error:
        return '오류';
    }
  }

  String _consoleStatusLabel(ConsoleHealthStatus status) {
    switch (status) {
      case ConsoleHealthStatus.idle:
        return '대기';
      case ConsoleHealthStatus.checking:
        return '확인 중';
      case ConsoleHealthStatus.ok:
        return '정상';
      case ConsoleHealthStatus.failed:
        return '실패';
      case ConsoleHealthStatus.needsAttention:
        return '확인 필요';
    }
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
      SnackBar(content: Text('${draft.toolType.label} 설정을 저장했습니다.')),
    );
  }
}

class _DeviceTile extends StatelessWidget {
  const _DeviceTile({
    required this.device,
    required this.selected,
    required this.enabled,
    required this.onChanged,
  });

  final ConnectedAdbDevice device;
  final bool selected;
  final bool enabled;
  final ValueChanged<bool?> onChanged;

  @override
  Widget build(BuildContext context) {
    final statusColor = switch (device.state) {
      'device' => const Color(0xFF067647),
      'offline' => const Color(0xFFF79009),
      _ => const Color(0xFFB42318),
    };

    return CheckboxListTile(
      value: selected,
      onChanged: enabled ? onChanged : null,
      contentPadding: EdgeInsets.zero,
      title: Text(device.summaryLabel),
      subtitle: Text('상태: ${device.state}'),
      secondary: Icon(Icons.circle, color: statusColor, size: 14),
    );
  }
}

class _ReadOnlyField extends StatelessWidget {
  const _ReadOnlyField({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      initialValue: value,
      readOnly: true,
      maxLines: 3,
      decoration: InputDecoration(labelText: label),
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
                  child: Text(
                    '도구 설정',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                FilledButton.tonalIcon(
                  onPressed: onSave,
                  icon: const Icon(Icons.save_rounded),
                  label: const Text('설정 저장'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            _Field(
              label: '도구 루트 경로',
              initialValue: config.toolRoot,
              onChanged: (value) => onChanged(config.copyWith(toolRoot: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: '결과 경로',
              initialValue: config.resultsDir,
              onChanged: (value) => onChanged(config.copyWith(resultsDir: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: '로그 경로',
              initialValue: config.logsDir,
              onChanged: (value) => onChanged(config.copyWith(logsDir: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: '기본 실행 명령',
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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(label, style: const TextStyle(color: Color(0xFF5D6779))),
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
