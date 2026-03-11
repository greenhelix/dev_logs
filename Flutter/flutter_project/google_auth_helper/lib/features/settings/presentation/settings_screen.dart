import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/config/app_defaults.dart';
import '../../../models/app_settings.dart';
import '../../../models/redmine_current_user.dart';
import '../../../models/redmine_project_summary.dart';
import '../../../providers/app_providers.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  bool _initialized = false;
  bool _isDirty = false;
  String _lastLoadedJson = '';
  bool _isLoadingRedmineProjects = false;
  String? _redmineLookupMessage;
  RedmineCurrentUser? _redmineCurrentUser;
  RedmineProjectSummary? _selectedProject;
  List<RedmineProjectSummary> _redmineProjects = const [];
  late AppSettings _draft;

  @override
  Widget build(BuildContext context) {
    final capabilities = ref.watch(runtimeCapabilitiesProvider);
    final state = ref.watch(appSettingsControllerProvider);
    final serialized = jsonEncode(state.settings.toJson());
    if (!_initialized || (!_isDirty && _lastLoadedJson != serialized)) {
      _draft = state.settings;
      _lastLoadedJson = serialized;
      _initialized = true;
    }

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('설정', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                const Text(
                  '이 화면은 공통 자격증명과 연결값만 관리합니다. 도구 경로, 명령, 샤드는 자동 테스트 메뉴에서 관리합니다.',
                ),
                const SizedBox(height: 14),
                _InfoLine('App Version', AppDefaults.appVersion),
                const SizedBox(height: 8),
                _InfoLine('플랫폼', capabilities.platformLabel),
                _InfoLine('권한', capabilities.badgeLabel),
                const SizedBox(height: 12),
                DropdownButtonFormField<AppMode>(
                  initialValue: _draft.mode,
                  decoration: const InputDecoration(labelText: '모드'),
                  items: AppMode.values
                      .map(
                        (mode) => DropdownMenuItem(
                          value: mode,
                          child: Text(mode.label),
                        ),
                      )
                      .toList(growable: false),
                  onChanged: (value) {
                    if (value == null) {
                      return;
                    }
                    setState(() {
                      _isDirty = true;
                      _draft = _draft.copyWith(mode: value);
                    });
                  },
                ),
                const SizedBox(height: 12),
                _Field(
                  label: 'Firebase Project ID',
                  initialValue: _draft.firebaseProjectId,
                  onChanged: (value) {
                    _isDirty = true;
                    _draft = _draft.copyWith(firebaseProjectId: value);
                  },
                ),
                const SizedBox(height: 12),
                _Field(
                  label: 'Firestore Database ID',
                  initialValue: _draft.firestoreDatabaseId,
                  onChanged: (value) {
                    _isDirty = true;
                    _draft = _draft.copyWith(firestoreDatabaseId: value);
                  },
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<CredentialMode>(
                  initialValue: _draft.credentialMode,
                  decoration:
                      const InputDecoration(labelText: 'Firestore 자격증명 방식'),
                  items: CredentialMode.values
                      .map(
                        (mode) => DropdownMenuItem(
                          value: mode,
                          child: Text(mode.label),
                        ),
                      )
                      .toList(growable: false),
                  onChanged: (value) {
                    if (value == null) {
                      return;
                    }
                    setState(() {
                      _isDirty = true;
                      _draft = _draft.copyWith(credentialMode: value);
                    });
                  },
                ),
                const SizedBox(height: 12),
                _Field(
                  label: 'Service Account Path',
                  initialValue: _draft.serviceAccountPath,
                  onChanged: (value) {
                    _isDirty = true;
                    _draft = _draft.copyWith(serviceAccountPath: value);
                  },
                ),
                const SizedBox(height: 12),
                _Field(
                  label: 'Web Proxy Base URL',
                  initialValue: _draft.webProxyBaseUrl,
                  onChanged: (value) {
                    _isDirty = true;
                    _draft = _draft.copyWith(webProxyBaseUrl: value);
                  },
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
                Text('Redmine 자격증명',
                    style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                const Text(
                  '웹과 PC 모두 이 값을 사용합니다. 사용자별 API 키를 직접 입력해 저장할 수 있습니다.',
                ),
                const SizedBox(height: 12),
                _Field(
                  label: 'Redmine Base URL',
                  initialValue: _draft.redmineBaseUrl,
                  onChanged: (value) {
                    _isDirty = true;
                    _draft = _draft.copyWith(redmineBaseUrl: value);
                  },
                ),
                const SizedBox(height: 12),
                _Field(
                  label: 'Redmine API Key',
                  initialValue: _draft.redmineApiKey,
                  obscureText: true,
                  onChanged: (value) {
                    _isDirty = true;
                    _draft = _draft.copyWith(redmineApiKey: value);
                  },
                ),
                const SizedBox(height: 12),
                _Field(
                  label: 'Redmine Project ID',
                  initialValue: _draft.redmineProjectId,
                  onChanged: (value) {
                    _isDirty = true;
                    _draft = _draft.copyWith(redmineProjectId: value);
                  },
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    FilledButton.tonalIcon(
                      onPressed: _isLoadingRedmineProjects
                          ? null
                          : () => _loadRedmineProjects(),
                      icon: const Icon(Icons.cloud_sync_rounded),
                      label: Text(
                        _isLoadingRedmineProjects
                            ? 'Loading...'
                            : 'Load Projects',
                      ),
                    ),
                    if (_selectedProject != null)
                      Chip(
                        label: Text(
                          'Selected: ${_selectedProject!.displayLabel}',
                        ),
                      ),
                  ],
                ),
                if (_isLoadingRedmineProjects) ...[
                  const SizedBox(height: 12),
                  const LinearProgressIndicator(),
                ],
                if (_redmineLookupMessage != null) ...[
                  const SizedBox(height: 12),
                  Text(
                    _redmineLookupMessage!,
                    style: TextStyle(
                      color: _selectedProject != null || _redmineCurrentUser != null
                          ? const Color(0xFF1459FF)
                          : const Color(0xFFB42318),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
                if (_redmineCurrentUser != null) ...[
                  const SizedBox(height: 12),
                  _InfoLine('Current User', _redmineCurrentUser!.displayName),
                ],
                if (_redmineProjects.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Text(
                    'Accessible Projects',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                    height: 220,
                    child: ListView.separated(
                      itemCount: _redmineProjects.length,
                      separatorBuilder: (_, __) => const Divider(height: 1),
                      itemBuilder: (context, index) {
                        final project = _redmineProjects[index];
                        final selected = _draft.redmineProjectId.trim() ==
                            project.id.toString();
                        return ListTile(
                          dense: true,
                          contentPadding: EdgeInsets.zero,
                          leading: Icon(
                            selected
                                ? Icons.radio_button_checked_rounded
                                : Icons.radio_button_unchecked_rounded,
                            color: selected
                                ? const Color(0xFF1459FF)
                                : const Color(0xFF98A2B3),
                          ),
                          title: Text(project.displayLabel),
                          subtitle: project.description.isEmpty
                              ? null
                              : Text(
                                  project.description,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                          onTap: () {
                            setState(() {
                              _isDirty = true;
                              _draft = _draft.copyWith(
                                redmineProjectId: project.id.toString(),
                              );
                              _selectedProject = project;
                            });
                          },
                        );
                      },
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 18),
        FilledButton.icon(
          onPressed: state.isLoading
              ? null
              : () async {
                  await ref
                      .read(appSettingsControllerProvider.notifier)
                      .updateSettings(_draft);
                  ref.read(runControllerProvider.notifier).syncToolConfig();
                  _isDirty = false;
                  _lastLoadedJson = jsonEncode(_draft.toJson());
                  if (!context.mounted) {
                    return;
                  }
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('설정을 저장했습니다.')),
                  );
                },
          icon: const Icon(Icons.save_rounded),
          label: Text(state.isLoading ? '저장 중...' : '설정 저장'),
        ),
      ],
    );
  }

  Future<void> _loadRedmineProjects() async {
    setState(() {
      _isLoadingRedmineProjects = true;
      _redmineLookupMessage = null;
    });

    try {
      final service = ref.read(redmineServiceProvider);
      final user = await service.fetchCurrentUser(settings: _draft);
      final projects = await service.fetchProjects(settings: _draft, limit: 200);
      RedmineProjectSummary? selectedProject;
      final selectedProjectId = _draft.redmineProjectId.trim();
      if (selectedProjectId.isNotEmpty) {
        try {
          selectedProject = await service.fetchProject(
            settings: _draft,
            projectId: selectedProjectId,
          );
        } catch (_) {
          selectedProject = projects.cast<RedmineProjectSummary?>().firstWhere(
                (item) => item?.id.toString() == selectedProjectId,
                orElse: () => null,
              );
        }
      }

      if (!mounted) {
        return;
      }
      setState(() {
        _isLoadingRedmineProjects = false;
        _redmineCurrentUser = user;
        _redmineProjects = projects;
        _selectedProject = selectedProject;
        _redmineLookupMessage =
            'Loaded ${projects.length} project(s) from Redmine.';
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _isLoadingRedmineProjects = false;
        _redmineLookupMessage = '$error';
        _redmineCurrentUser = null;
        _redmineProjects = const [];
        _selectedProject = null;
      });
    }
  }
}

class _Field extends StatelessWidget {
  const _Field({
    required this.label,
    required this.initialValue,
    required this.onChanged,
    this.obscureText = false,
  });

  final String label;
  final String initialValue;
  final ValueChanged<String> onChanged;
  final bool obscureText;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      key: ValueKey('$label::$initialValue'),
      initialValue: initialValue,
      decoration: InputDecoration(labelText: label),
      obscureText: obscureText,
      onChanged: onChanged,
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
        children: [
          SizedBox(
            width: 140,
            child:
                Text(label, style: const TextStyle(color: Color(0xFF5D6779))),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}
