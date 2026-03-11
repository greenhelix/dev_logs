import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../models/connected_adb_device.dart';
import '../../../models/console_health.dart';
import '../../../models/live_status.dart';
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

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Auto Test',
                    style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                Text(
                  capabilities.canRunTests
                      ? 'Start tradefed, wait for the console prompt, then send the generated run command.'
                      : 'This platform does not support test execution.',
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
                        decoration: const InputDecoration(labelText: 'Tool'),
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
                        runState.isRefreshingDevices
                            ? 'Refreshing...'
                            : 'Refresh Devices',
                      ),
                    ),
                    FilledButton.icon(
                      onPressed: runState.isRunning ||
                              !capabilities.canRunTests ||
                              runState.selectedDeviceSerials.isEmpty
                          ? null
                          : controller.startRun,
                      icon: const Icon(Icons.play_arrow_rounded),
                      label: const Text('Run'),
                    ),
                    FilledButton.tonalIcon(
                      onPressed: runState.isRunning ? controller.stopRun : null,
                      icon: const Icon(Icons.stop_circle_outlined),
                      label: const Text('Stop'),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  value: runState.autoUploadAfterRun,
                  onChanged:
                      runState.isRunning ? null : controller.updateAutoUpload,
                  title: const Text('Auto upload after run'),
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
                Text('Device Selection',
                    style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                if (runState.availableDevices.isEmpty)
                  const Text('No ADB devices detected.')
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
                  label: 'Generated Command',
                  value: runState.generatedCommand.isEmpty
                      ? '(select ready devices first)'
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
                Text('Execution Status',
                    style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                _StatusLine('Stage', runState.stage.name),
                _StatusLine(
                  'Console',
                  _consoleStatusLabel(runState.consoleHealth.status),
                ),
                _StatusLine('Console Detail', runState.consoleHealth.message),
                _StatusLine(
                  'Selected Devices',
                  runState.selectedDeviceSerials.isEmpty
                      ? '(none)'
                      : runState.selectedDeviceSerials.join(', '),
                ),
                _StatusLine('Shard Count', '${runState.shardCount}'),
                _StatusLine(
                  'Auto Upload',
                  runState.autoUploadAfterRun ? 'enabled' : 'disabled',
                ),
                _StatusLine(
                  'Result Path',
                  runState.detectedResultsDir ?? '(not set)',
                ),
                _StatusLine(
                  'Log Path',
                  runState.detectedLogsDir ?? '(not set)',
                ),
                _StatusLine(
                  'Exit Code',
                  runState.exitCode == null ? '(none)' : '${runState.exitCode}',
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
                Text('Live Logs', style: Theme.of(context).textTheme.titleLarge),
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
                        ? 'No logs yet.'
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

  String _consoleStatusLabel(ConsoleHealthStatus status) {
    switch (status) {
      case ConsoleHealthStatus.idle:
        return 'idle';
      case ConsoleHealthStatus.checking:
        return 'checking';
      case ConsoleHealthStatus.ok:
        return 'ok';
      case ConsoleHealthStatus.failed:
        return 'failed';
      case ConsoleHealthStatus.needsAttention:
        return 'needs attention';
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
      SnackBar(content: Text('${draft.toolType.label} profile saved.')),
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
      subtitle: Text('State: ${device.state}'),
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
                    'Tool Profile',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                FilledButton.tonalIcon(
                  onPressed: onSave,
                  icon: const Icon(Icons.save_rounded),
                  label: const Text('Save Profile'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            _Field(
              label: 'Tool Root',
              initialValue: config.toolRoot,
              onChanged: (value) => onChanged(config.copyWith(toolRoot: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: 'Result Path',
              initialValue: config.resultsDir,
              onChanged: (value) =>
                  onChanged(config.copyWith(resultsDir: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: 'Log Path',
              initialValue: config.logsDir,
              onChanged: (value) => onChanged(config.copyWith(logsDir: value)),
            ),
            const SizedBox(height: 12),
            _Field(
              label: 'Base Command',
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
