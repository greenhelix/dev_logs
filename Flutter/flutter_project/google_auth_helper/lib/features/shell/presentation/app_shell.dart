import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/config/app_defaults.dart';
import '../../../models/app_log_entry.dart';
import '../../../models/release_status.dart';
import '../../../providers/app_providers.dart';
import '../../dashboard/presentation/dashboard_screen.dart';
import '../../environment/presentation/environment_screen.dart';
import '../../results/presentation/results_screen.dart';
import '../../run/presentation/run_screen.dart';
import '../../settings/presentation/settings_screen.dart';
import '../../updates/presentation/updates_screen.dart';

enum AppSection { dashboard, results, updates, environment, run, settings }

class _SectionMeta {
  const _SectionMeta({
    required this.title,
    required this.description,
    required this.icon,
    required this.logArea,
  });

  final String title;
  final String description;
  final IconData icon;
  final AppLogArea logArea;
}

const _sectionMeta = <AppSection, _SectionMeta>{
  AppSection.dashboard: _SectionMeta(
    title: '대시보드',
    description: '원격 집계와 현재 미리보기를 확인합니다.',
    icon: Icons.dashboard_rounded,
    logArea: AppLogArea.dashboard,
  ),
  AppSection.results: _SectionMeta(
    title: '결과 업로드',
    description: '결과와 로그를 올리고 업로드 본문을 확인합니다.',
    icon: Icons.upload_file_rounded,
    logArea: AppLogArea.results,
  ),
  AppSection.updates: _SectionMeta(
    title: '릴리즈 감시',
    description: '릴리즈 감시 대상과 최근 변화를 확인합니다.',
    icon: Icons.update_rounded,
    logArea: AppLogArea.updates,
  ),
  AppSection.environment: _SectionMeta(
    title: '환경 점검',
    description: 'Firebase, Redmine, ADB 상태를 점검합니다.',
    icon: Icons.health_and_safety_rounded,
    logArea: AppLogArea.environment,
  ),
  AppSection.run: _SectionMeta(
    title: '자동 테스트',
    description: '콘솔 시작과 실행 명령 전송을 분리해 제어합니다.',
    icon: Icons.terminal_rounded,
    logArea: AppLogArea.run,
  ),
  AppSection.settings: _SectionMeta(
    title: '설정',
    description: '공용 인증 정보와 연결 설정을 관리합니다.',
    icon: Icons.tune_rounded,
    logArea: AppLogArea.settings,
  ),
};

class AppShell extends ConsumerStatefulWidget {
  const AppShell({super.key});

  @override
  ConsumerState<AppShell> createState() => _AppShellState();
}

class _AppShellState extends ConsumerState<AppShell> {
  AppSection _section = AppSection.dashboard;
  bool _updatePromptShown = false;
  bool _logExpanded = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.listenManual<AsyncValue<ReleaseStatus>>(
        releaseStatusProvider,
        (previous, next) {
          next.whenData((status) {
            if (!mounted || _updatePromptShown || !status.hasUpdate || kIsWeb) {
              return;
            }
            _updatePromptShown = true;
            unawaited(_showUpdateDialog(status));
          });
        },
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final releaseStatusAsync = ref.watch(releaseStatusProvider);
    final logs = ref.watch(appLogControllerProvider);
    final meta = _sectionMeta[_section]!;
    final width = MediaQuery.sizeOf(context).width;
    final isWide = width >= 1100;

    return Scaffold(
      drawer: isWide
          ? null
          : Drawer(
              child: _Sidebar(
                currentSection: _section,
                onSelected: _selectSection,
              ),
            ),
      body: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFFF4F7FB), Color(0xFFE4EBF6)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: Row(
          children: [
            if (isWide)
              SizedBox(
                width: 300,
                child: _Sidebar(
                  currentSection: _section,
                  onSelected: _selectSection,
                ),
              ),
            Expanded(
              child: SafeArea(
                child: Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 18, 20, 8),
                      child: Row(
                        children: [
                          if (!isWide)
                            Builder(
                              builder: (context) {
                                return IconButton.filledTonal(
                                  onPressed: () => Scaffold.of(context).openDrawer(),
                                  icon: const Icon(Icons.menu_rounded),
                                );
                              },
                            ),
                          if (!isWide) const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  meta.title,
                                  style: Theme.of(context).textTheme.headlineMedium,
                                ),
                                const SizedBox(height: 6),
                                Text(
                                  meta.description,
                                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                        color: const Color(0xFF5D6779),
                                      ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    releaseStatusAsync.when(
                      data: (status) => status.hasUpdate
                          ? Padding(
                              padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
                              child: _UpdateBanner(
                                status: status,
                                onInstall: () => _installRelease(status),
                                onOpenRelease: () =>
                                    _openReleaseUrl(context, status.releaseUrl),
                              ),
                            )
                          : const SizedBox.shrink(),
                      loading: () => const SizedBox.shrink(),
                      error: (_, __) => const SizedBox.shrink(),
                    ),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(20, 4, 20, 12),
                        child: AnimatedSwitcher(
                          duration: const Duration(milliseconds: 220),
                          child: KeyedSubtree(
                            key: ValueKey(_section),
                            child: _buildSection(),
                          ),
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                      child: _LogPanel(
                        section: _section,
                        entries: logs,
                        expanded: _logExpanded,
                        onClear: () {
                          ref
                              .read(appLogControllerProvider.notifier)
                              .clearForArea(meta.logArea);
                        },
                        onToggle: () {
                          setState(() {
                            _logExpanded = !_logExpanded;
                          });
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSection() {
    switch (_section) {
      case AppSection.dashboard:
        return const DashboardScreen();
      case AppSection.results:
        return const ResultsScreen();
      case AppSection.updates:
        return const UpdatesScreen();
      case AppSection.environment:
        return const EnvironmentScreen();
      case AppSection.run:
        return const RunScreen();
      case AppSection.settings:
        return const SettingsScreen();
    }
  }

  void _selectSection(AppSection section) {
    final capabilities = ref.read(runtimeCapabilitiesProvider);
    if (section == AppSection.run && !capabilities.canRunTests) {
      return;
    }

    setState(() {
      _section = section;
    });
    if (Scaffold.maybeOf(context)?.isDrawerOpen ?? false) {
      Navigator.of(context).pop();
    }
  }

  Future<void> _openReleaseUrl(BuildContext context, String rawUrl) async {
    final uri = Uri.tryParse(rawUrl);
    if (uri == null) {
      return;
    }
    final launched = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (launched || !context.mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('릴리즈 페이지를 열지 못했습니다.')),
    );
  }

  Future<void> _showUpdateDialog(ReleaseStatus status) async {
    if (!mounted) {
      return;
    }
    final install = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('새 버전이 있습니다'),
          content: Text(
            '현재 버전 ${status.currentVersion}\n최신 버전 ${status.latestVersion}\n\n지금 설치를 시작할까요?',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('나중에'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              child: const Text('설치 시작'),
            ),
          ],
        );
      },
    );
    if (install == true) {
      await _installRelease(status);
    }
  }

  Future<void> _installRelease(ReleaseStatus status) async {
    final service = ref.read(releaseInstallerServiceProvider);
    if (!service.isSupported || status.installerAsset == null) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('이 플랫폼에서는 자동 설치 파일을 찾지 못했습니다.')),
      );
      return;
    }

    ref.read(appLogControllerProvider.notifier).add(
          area: AppLogArea.common,
          message: '업데이트 설치 파일 다운로드 시작',
          detail: status.installerAsset!.name,
        );
    try {
      final installerPath = await service.downloadInstaller(status);
      await service.openInstaller(installerPath);
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('설치 프로그램을 실행했습니다: ${status.installerAsset!.name}'),
        ),
      );
      ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.common,
            message: '업데이트 설치 파일 실행',
            detail: installerPath,
          );
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('업데이트 설치를 시작하지 못했습니다. $error')),
      );
      ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.common,
            message: '업데이트 설치 실패',
            level: AppLogLevel.error,
            detail: '$error',
          );
    }
  }
}

class _Sidebar extends ConsumerWidget {
  const _Sidebar({
    required this.currentSection,
    required this.onSelected,
  });

  final AppSection currentSection;
  final ValueChanged<AppSection> onSelected;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final capabilities = ref.watch(runtimeCapabilitiesProvider);
    final resultsState = ref.watch(resultsControllerProvider);
    final releaseAsync = ref.watch(releaseWatcherSnapshotProvider);

    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(0xFF112B62), Color(0xFF0A1B42)],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
      ),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'GAH',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 28,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                '버전 ${AppDefaults.appVersion}',
                style: const TextStyle(color: Color(0xFFA7B8DE)),
              ),
              const SizedBox(height: 18),
              _SidebarCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '현재 상태',
                      style: TextStyle(
                        color: Color(0xFFBACEFF),
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 10),
                    _SidebarStatusRow(
                      label: '권한',
                      value: capabilities.badgeLabel,
                    ),
                    _SidebarStatusRow(
                      label: '미리보기',
                      value: resultsState.previewBundle == null ? '없음' : '준비됨',
                    ),
                    _SidebarStatusRow(
                      label: '자동 테스트',
                      value: capabilities.canRunTests ? '사용 가능' : '우분투 전용',
                    ),
                    _SidebarStatusRow(
                      label: '릴리즈 감시',
                      value: releaseAsync.when(
                        data: (status) =>
                            status.changes.isEmpty ? '변경 없음' : '변경 있음',
                        loading: () => '확인 중',
                        error: (_, __) => '오류',
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Expanded(
                child: ListView(
                  children: AppSection.values.map((section) {
                    final meta = _sectionMeta[section]!;
                    final selected = currentSection == section;
                    final enabled =
                        section != AppSection.run || capabilities.canRunTests;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: _SidebarMenuButton(
                        title: meta.title,
                        icon: meta.icon,
                        selected: selected,
                        enabled: enabled,
                        note: enabled ? null : '우분투 전용',
                        onPressed: enabled ? () => onSelected(section) : null,
                      ),
                    );
                  }).toList(growable: false),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SidebarMenuButton extends StatelessWidget {
  const _SidebarMenuButton({
    required this.title,
    required this.icon,
    required this.selected,
    required this.enabled,
    required this.onPressed,
    this.note,
  });

  final String title;
  final IconData icon;
  final bool selected;
  final bool enabled;
  final String? note;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return FilledButton.tonalIcon(
      style: FilledButton.styleFrom(
        alignment: Alignment.centerLeft,
        backgroundColor: selected
            ? const Color(0xFF1A6FFF)
            : enabled
                ? const Color(0x14FFFFFF)
                : const Color(0x1FFFFFFF),
        foregroundColor: enabled ? Colors.white : const Color(0xFFA7B8DE),
        disabledBackgroundColor: const Color(0x1FFFFFFF),
        disabledForegroundColor: const Color(0xFFA7B8DE),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
        ),
      ),
      onPressed: onPressed,
      icon: Icon(icon),
      label: Row(
        children: [
          Expanded(child: Text(title)),
          if (note != null)
            Text(
              note!,
              style: const TextStyle(fontSize: 11),
            ),
        ],
      ),
    );
  }
}

class _UpdateBanner extends StatelessWidget {
  const _UpdateBanner({
    required this.status,
    required this.onInstall,
    required this.onOpenRelease,
  });

  final ReleaseStatus status;
  final VoidCallback onInstall;
  final VoidCallback onOpenRelease;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFE9F3FF),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFFB8CCF3)),
      ),
      child: Row(
        children: [
          const Icon(Icons.system_update_rounded, color: Color(0xFF1459FF)),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              '새 버전 ${status.latestVersion}을 설치할 수 있습니다.',
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
          if (status.installerAsset != null)
            FilledButton.tonal(
              onPressed: onInstall,
              child: const Text('설치 시작'),
            )
          else
            FilledButton.tonal(
              onPressed: onOpenRelease,
              child: const Text('릴리즈 열기'),
            ),
        ],
      ),
    );
  }
}

class _LogPanel extends StatefulWidget {
  const _LogPanel({
    required this.section,
    required this.entries,
    required this.expanded,
    required this.onClear,
    required this.onToggle,
  });

  final AppSection section;
  final List<AppLogEntry> entries;
  final bool expanded;
  final VoidCallback onClear;
  final VoidCallback onToggle;

  @override
  State<_LogPanel> createState() => _LogPanelState();
}

class _LogPanelState extends State<_LogPanel> {
  final ScrollController _scrollController = ScrollController();

  @override
  void didUpdateWidget(covariant _LogPanel oldWidget) {
    super.didUpdateWidget(oldWidget);
    final hadVisible = _visibleEntries(oldWidget).length;
    final visible = _visibleEntries(widget).length;
    if (!widget.expanded || visible == 0 || visible == hadVisible) {
      return;
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) {
        return;
      }
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final visible = _visibleEntries(widget);
    final listHeight = math.min(
      320.0,
      math.max(140.0, visible.length * 78.0),
    );

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: Row(
                    children: [
                      Text(
                        '로그',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(width: 10),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0xFFE9EEF7),
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: Text(
                          '${visible.length}건',
                          style: const TextStyle(
                            color: Color(0xFF475467),
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                FilledButton.tonalIcon(
                  onPressed: visible.isEmpty ? null : widget.onClear,
                  icon: const Icon(Icons.delete_sweep_rounded),
                  label: const Text('현재 메뉴 비우기'),
                ),
                const SizedBox(width: 8),
                FilledButton.tonalIcon(
                  onPressed: visible.isEmpty
                      ? null
                      : () async {
                          final text = visible
                              .map(_formatLogEntry)
                              .join('\n\n');
                          await Clipboard.setData(ClipboardData(text: text));
                          if (!context.mounted) {
                            return;
                          }
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('로그를 복사했습니다.')),
                          );
                        },
                  icon: const Icon(Icons.copy_rounded),
                  label: const Text('복사'),
                ),
                const SizedBox(width: 8),
                FilledButton.tonalIcon(
                  onPressed: widget.onToggle,
                  icon: Icon(widget.expanded
                      ? Icons.unfold_less_rounded
                      : Icons.unfold_more_rounded),
                  label: Text(widget.expanded ? '접기' : '펼치기'),
                ),
              ],
            ),
            if (widget.expanded) ...[
              const SizedBox(height: 12),
              Container(
                width: double.infinity,
                height: listHeight,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFF0F1C36),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: visible.isEmpty
                    ? const Align(
                        alignment: Alignment.topLeft,
                        child: Text(
                          '표시할 로그가 없습니다.',
                          style: TextStyle(color: Color(0xFFD8E4FF)),
                        ),
                      )
                    : Scrollbar(
                        controller: _scrollController,
                        thumbVisibility: visible.length > 2,
                        child: ListView.separated(
                          controller: _scrollController,
                          itemCount: visible.length,
                          itemBuilder: (context, index) {
                            return _LogEntryTile(entry: visible[index]);
                          },
                          separatorBuilder: (context, index) =>
                              const SizedBox(height: 8),
                        ),
                      ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  List<AppLogEntry> _visibleEntries(_LogPanel widget) {
    final area = _sectionMeta[widget.section]!.logArea;
    return widget.entries
        .where((entry) => entry.area == area || entry.area == AppLogArea.common)
        .toList(growable: false);
  }

  String _formatLogEntry(AppLogEntry entry) {
    final prefix = '[${entry.timestamp.toLocal().toString().split(".").first}]';
    final detail = entry.detail == null ? '' : '\n${entry.detail}';
    return '$prefix ${entry.message}$detail';
  }
}

class _LogEntryTile extends StatelessWidget {
  const _LogEntryTile({required this.entry});

  final AppLogEntry entry;

  @override
  Widget build(BuildContext context) {
    final tone = switch (entry.level) {
      AppLogLevel.info => const Color(0xFF98A2FF),
      AppLogLevel.warning => const Color(0xFFFDB022),
      AppLogLevel.error => const Color(0xFFF97066),
    };
    final levelLabel = switch (entry.level) {
      AppLogLevel.info => 'INFO',
      AppLogLevel.warning => 'WARN',
      AppLogLevel.error => 'ERROR',
    };

    return DecoratedBox(
      decoration: BoxDecoration(
        color: const Color(0x141D2939),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0x263A4A66)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8,
                    vertical: 3,
                  ),
                  decoration: BoxDecoration(
                    color: tone.withAlpha(41),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    levelLabel,
                    style: TextStyle(
                      color: tone,
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    entry.message,
                    style: const TextStyle(
                      color: Color(0xFFF8FAFC),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                Text(
                  entry.timestamp.toLocal().toString().split('.').first,
                  style: const TextStyle(
                    color: Color(0xFF98A2B3),
                    fontSize: 11,
                  ),
                ),
              ],
            ),
            if (entry.detail != null && entry.detail!.trim().isNotEmpty) ...[
              const SizedBox(height: 8),
              SelectableText(
                entry.detail!,
                style: const TextStyle(
                  color: Color(0xFFD8E4FF),
                  fontFamily: 'Consolas',
                  fontSize: 12,
                  height: 1.4,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _SidebarCard extends StatelessWidget {
  const _SidebarCard({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: const Color(0xCC102450),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFF29498A)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: child,
      ),
    );
  }
}

class _SidebarStatusRow extends StatelessWidget {
  const _SidebarStatusRow({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Color(0xFFA7B8DE))),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
