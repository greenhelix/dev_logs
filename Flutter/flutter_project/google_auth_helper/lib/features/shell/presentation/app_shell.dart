import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/config/app_defaults.dart';
import '../../../core/runtime/runtime_capabilities.dart';
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
  });

  final String title;
  final String description;
  final IconData icon;
}

const _sectionMeta = <AppSection, _SectionMeta>{
  AppSection.dashboard: _SectionMeta(
    title: '대시보드',
    description: '현재 진행 테스트, 업로드된 결과 추이, 테스트케이스 통계를 확인합니다.',
    icon: Icons.dashboard_rounded,
  ),
  AppSection.results: _SectionMeta(
    title: '결과 업로드',
    description: 'zip 업로드, 파싱 미리보기, Firestore/Redmine 업로드를 처리합니다.',
    icon: Icons.upload_file_rounded,
  ),
  AppSection.updates: _SectionMeta(
    title: '릴리즈 감시',
    description: 'Excel, Google Sheet, watcher artifact 기반 변경 감시 초안을 관리합니다.',
    icon: Icons.update_rounded,
  ),
  AppSection.environment: _SectionMeta(
    title: '환경점검',
    description: 'Firebase와 Redmine 연결 상태를 항목별로 점검합니다.',
    icon: Icons.health_and_safety_rounded,
  ),
  AppSection.run: _SectionMeta(
    title: '자동 테스트',
    description: '명령, 샤드, 경로를 관리하고 데스크톱에서 자동 테스트를 실행합니다.',
    icon: Icons.terminal_rounded,
  ),
  AppSection.settings: _SectionMeta(
    title: '설정',
    description: 'Redmine API 키와 공통 연결값 같은 자격증명만 관리합니다.',
    icon: Icons.tune_rounded,
  ),
};

class AppShell extends ConsumerStatefulWidget {
  const AppShell({super.key});

  @override
  ConsumerState<AppShell> createState() => _AppShellState();
}

class _AppShellState extends ConsumerState<AppShell> {
  AppSection _section = AppSection.dashboard;

  @override
  Widget build(BuildContext context) {
    final capabilities = ref.watch(runtimeCapabilitiesProvider);
    final releaseStatusAsync = ref.watch(releaseStatusProvider);
    final visibleSections = AppSection.values;
    final meta = _sectionMeta[_section]!;
    final width = MediaQuery.sizeOf(context).width;
    final isWide = width >= 1100;

    return Scaffold(
      drawer: isWide
          ? null
          : Drawer(
              child: _Sidebar(
                currentSection: _section,
                visibleSections: visibleSections,
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
                  visibleSections: visibleSections,
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
                                  onPressed: () =>
                                      Scaffold.of(context).openDrawer(),
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
                                  style: Theme.of(context)
                                      .textTheme
                                      .headlineMedium,
                                ),
                                const SizedBox(height: 6),
                                Text(
                                  meta.description,
                                  style: Theme.of(context)
                                      .textTheme
                                      .bodyMedium
                                      ?.copyWith(
                                        color: const Color(0xFF5D6779),
                                      ),
                                ),
                              ],
                            ),
                          ),
                          _PlatformStatusIcons(capabilities: capabilities),
                        ],
                      ),
                    ),
                    releaseStatusAsync.when(
                      data: (status) => status.hasUpdate
                          ? Padding(
                              padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
                              child: _UpdateBanner(
                                status: status,
                                onOpenRelease: () => _openReleaseUrl(
                                  context,
                                  status.releaseUrl,
                                ),
                              ),
                            )
                          : const SizedBox.shrink(),
                      loading: () => const SizedBox.shrink(),
                      error: (_, __) => const SizedBox.shrink(),
                    ),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
                        child: AnimatedSwitcher(
                          duration: const Duration(milliseconds: 220),
                          child: KeyedSubtree(
                            key: ValueKey(_section),
                            child: _buildSection(),
                          ),
                        ),
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
      const SnackBar(content: Text('Could not open the release page.')),
    );
  }
}

class _Sidebar extends ConsumerWidget {
  const _Sidebar({
    required this.currentSection,
    required this.visibleSections,
    required this.onSelected,
  });

  final AppSection currentSection;
  final List<AppSection> visibleSections;
  final ValueChanged<AppSection> onSelected;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final capabilities = ref.watch(runtimeCapabilitiesProvider);
    final resultsState = ref.watch(resultsControllerProvider);
    final runState = ref.watch(runControllerProvider);
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
                'Google Auth Helper ${AppDefaults.appVersion}',
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
                        label: '플랫폼', value: capabilities.platformLabel),
                    _SidebarStatusRow(
                        label: '권한', value: capabilities.badgeLabel),
                    _SidebarStatusRow(
                      label: '결과 미리보기',
                      value: resultsState.previewBundle == null ? '없음' : '준비됨',
                    ),
                    _SidebarStatusRow(
                      label: '자동 테스트',
                      value: runState.isRunning ? '진행 중' : '대기',
                    ),
                    _SidebarStatusRow(
                      label: '릴리즈 감시',
                      value: releaseAsync.when(
                        data: (status) =>
                            status.changes.isEmpty ? '변경 없음' : '변경 감지',
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
                  children: visibleSections.map((section) {
                    final meta = _sectionMeta[section]!;
                    final selected = currentSection == section;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: FilledButton.tonalIcon(
                        style: FilledButton.styleFrom(
                          alignment: Alignment.centerLeft,
                          backgroundColor: selected
                              ? const Color(0xFF1A6FFF)
                              : const Color(0x14FFFFFF),
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(
                            horizontal: 14,
                            vertical: 14,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                        onPressed: () => onSelected(section),
                        icon: Icon(meta.icon),
                        label: Text(meta.title),
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

class _PlatformStatusIcons extends StatelessWidget {
  const _PlatformStatusIcons({required this.capabilities});

  final RuntimeCapabilities capabilities;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        _PlatformDot.icon(
          icon: Icons.language_rounded,
          active: capabilities.profile == RuntimePlatformProfile.webHosting,
          color: const Color(0xFF1459FF),
          tooltip: '웹',
        ),
        const SizedBox(width: 8),
        _PlatformDot.asset(
          assetPath: 'assets/platform/windows_logo.svg',
          active: capabilities.profile == RuntimePlatformProfile.windowsDesktop,
          color: const Color(0xFF00A76F),
          tooltip: '윈도우',
        ),
        const SizedBox(width: 8),
        _PlatformDot.asset(
          assetPath: 'assets/platform/ubuntu_logo.svg',
          active: capabilities.profile == RuntimePlatformProfile.ubuntuDesktop,
          color: const Color(0xFFF79009),
          tooltip: '우분투',
        ),
      ],
    );
  }
}

class _UpdateBanner extends StatelessWidget {
  const _UpdateBanner({
    required this.status,
    required this.onOpenRelease,
  });

  final ReleaseStatus status;
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
              'Update available: ${status.currentVersion} -> ${status.latestVersion}',
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
          FilledButton.tonal(
            onPressed: onOpenRelease,
            child: const Text('Open Release'),
          ),
        ],
      ),
    );
  }
}

class _PlatformDot extends StatelessWidget {
  const _PlatformDot.icon({
    required this.active,
    required this.color,
    required this.tooltip,
    required IconData icon,
  })  : _icon = icon,
        _assetPath = null;

  const _PlatformDot.asset({
    required this.active,
    required this.color,
    required this.tooltip,
    required String assetPath,
  })  : _assetPath = assetPath,
        _icon = null;

  final bool active;
  final Color color;
  final String tooltip;
  final IconData? _icon;
  final String? _assetPath;

  @override
  Widget build(BuildContext context) {
    final background = active ? color : const Color(0xFFE5EAF2);
    final foreground = active ? Colors.white : const Color(0xFF7A869A);

    return Tooltip(
      message: tooltip,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        width: 42,
        height: 42,
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(14),
          boxShadow: active
              ? [
                  BoxShadow(
                    color: color.withValues(alpha: 0.22),
                    blurRadius: 16,
                    offset: const Offset(0, 8),
                  ),
                ]
              : null,
        ),
        child: Center(
          child: _assetPath == null
              ? Icon(_icon, color: foreground)
              : SvgPicture.asset(
                  _assetPath,
                  width: 24,
                  height: 24,
                  colorFilter: active
                      ? const ColorFilter.mode(Colors.white, BlendMode.srcIn)
                      : null,
                ),
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
