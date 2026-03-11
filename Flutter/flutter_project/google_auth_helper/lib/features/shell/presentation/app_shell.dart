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
    title: 'Dashboard',
    description: 'Review the latest uploaded tests and summary trends.',
    icon: Icons.dashboard_rounded,
  ),
  AppSection.results: _SectionMeta(
    title: 'Results Upload',
    description: 'Upload result and log zip files, then preview uploads.',
    icon: Icons.upload_file_rounded,
  ),
  AppSection.updates: _SectionMeta(
    title: 'Release Watch',
    description: 'Track release changes from watcher sources and artifacts.',
    icon: Icons.update_rounded,
  ),
  AppSection.environment: _SectionMeta(
    title: 'Environment',
    description: 'Check Firebase, ADB, and Redmine connectivity.',
    icon: Icons.health_and_safety_rounded,
  ),
  AppSection.run: _SectionMeta(
    title: 'Auto Test',
    description: 'Linux-only tradefed execution with ADB device selection.',
    icon: Icons.terminal_rounded,
  ),
  AppSection.settings: _SectionMeta(
    title: 'Settings',
    description: 'Manage shared connections and per-tool paths.',
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
    final meta = _sectionMeta[_section]!;
    final width = MediaQuery.sizeOf(context).width;
    final isWide = width >= 1100;
    final showHeaderDescription = width >= 780;

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
                                if (showHeaderDescription) ...[
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
      const SnackBar(content: Text('Could not open the release page.')),
    );
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
                'Google Auth Helper ${AppDefaults.appVersion}',
                style: const TextStyle(color: Color(0xFFA7B8DE)),
              ),
              const SizedBox(height: 18),
              _SidebarCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Current Status',
                      style: TextStyle(
                        color: Color(0xFFBACEFF),
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 10),
                    _SidebarStatusRow(
                      label: 'Platform',
                      value: capabilities.platformLabel,
                    ),
                    _SidebarStatusRow(
                      label: 'Access',
                      value: capabilities.badgeLabel,
                    ),
                    _SidebarStatusRow(
                      label: 'Preview',
                      value: resultsState.previewBundle == null
                          ? 'Not ready'
                          : 'Ready',
                    ),
                    _SidebarStatusRow(
                      label: 'Auto Test',
                      value: capabilities.canRunTests
                          ? 'Available'
                          : 'Ubuntu only',
                    ),
                    _SidebarStatusRow(
                      label: 'Release Watch',
                      value: releaseAsync.when(
                        data: (status) =>
                            status.changes.isEmpty ? 'No changes' : 'Changes',
                        loading: () => 'Checking',
                        error: (_, __) => 'Error',
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
                        note: enabled ? null : 'Ubuntu only',
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
          tooltip: 'Web',
        ),
        const SizedBox(width: 8),
        _PlatformDot.asset(
          assetPath: 'assets/platform/windows_logo.svg',
          active: capabilities.profile == RuntimePlatformProfile.windowsDesktop,
          color: const Color(0xFF00A76F),
          tooltip: 'Windows',
        ),
        const SizedBox(width: 8),
        _PlatformDot.asset(
          assetPath: 'assets/platform/ubuntu_logo.svg',
          active: capabilities.profile == RuntimePlatformProfile.ubuntuDesktop,
          color: const Color(0xFFF79009),
          tooltip: 'Ubuntu',
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

class _PlatformDot extends StatefulWidget {
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
  State<_PlatformDot> createState() => _PlatformDotState();
}

class _PlatformDotState extends State<_PlatformDot> {
  bool _focused = false;
  bool _hovered = false;

  @override
  Widget build(BuildContext context) {
    final isHighlighted = _focused || _hovered;
    final background = widget.active
        ? widget.color
        : isHighlighted
            ? const Color(0xFFD7DEE9)
            : const Color(0xFFE5EAF2);
    final foreground =
        widget.active ? Colors.white : const Color(0xFF465467);

    return Tooltip(
      message: widget.tooltip,
      child: FocusableActionDetector(
        onShowFocusHighlight: (value) {
          setState(() {
            _focused = value;
          });
        },
        onShowHoverHighlight: (value) {
          setState(() {
            _hovered = value;
          });
        },
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            color: background,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: isHighlighted
                  ? widget.color.withValues(alpha: 0.65)
                  : Colors.transparent,
              width: 1.5,
            ),
            boxShadow: widget.active || isHighlighted
                ? [
                    BoxShadow(
                      color: widget.color.withValues(alpha: 0.20),
                      blurRadius: 14,
                      offset: const Offset(0, 6),
                    ),
                  ]
                : null,
          ),
          child: Center(
            child: widget._assetPath == null
                ? Icon(widget._icon, color: foreground)
                : SvgPicture.asset(
                    widget._assetPath!,
                    width: 24,
                    height: 24,
                    colorFilter:
                        ColorFilter.mode(foreground, BlendMode.srcIn),
                  ),
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
