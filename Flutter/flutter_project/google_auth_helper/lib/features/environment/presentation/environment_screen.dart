import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/runtime/runtime_capabilities.dart';
import '../../../models/environment_check_status.dart';
import '../../../providers/app_providers.dart';

class EnvironmentScreen extends ConsumerWidget {
  const EnvironmentScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final envAsync = ref.watch(environmentStatusProvider);
    final capabilities = ref.watch(runtimeCapabilitiesProvider);

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('환경 점검', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                const Text('현재 실행 환경에서 Firebase, Redmine, ADB 연결 상태를 확인합니다.'),
                const SizedBox(height: 12),
                FilledButton.tonalIcon(
                  onPressed: () => ref.invalidate(environmentStatusProvider),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('다시 점검'),
                ),
                if (capabilities.profile == RuntimePlatformProfile.webHosting) ...[
                  const SizedBox(height: 10),
                  const Text(
                    '웹에서는 ADB 점검 항목을 표시하지 않습니다.',
                    style: TextStyle(
                      color: Color(0xFF5D6779),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        envAsync.when(
          data: (status) => _EnvironmentResult(
            status: status,
            showAdb: capabilities.profile != RuntimePlatformProfile.webHosting,
          ),
          loading: () => const _LoadingCard(),
          error: (error, _) => _ErrorCard(message: error.toString()),
        ),
      ],
    );
  }
}

class _EnvironmentResult extends StatelessWidget {
  const _EnvironmentResult({
    required this.status,
    required this.showAdb,
  });

  final EnvironmentCheckStatus status;
  final bool showAdb;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _ProbeSection(
          title: 'Firebase',
          subtitle: '호스팅과 Firestore 연결 상태',
          probes: status.firebaseResults,
        ),
        const SizedBox(height: 16),
        if (showAdb) ...[
          _ProbeSection(
            title: '로컬 도구',
            subtitle: 'ADB와 연결된 장치 상태',
            probes: status.localResults,
          ),
          const SizedBox(height: 16),
        ],
        _ProbeSection(
          title: 'Redmine',
          subtitle: '연결, 현재 사용자, 프로젝트 접근 상태',
          probes: status.redmineResults,
        ),
      ],
    );
  }
}

class _ProbeSection extends StatelessWidget {
  const _ProbeSection({
    required this.title,
    required this.subtitle,
    required this.probes,
  });

  final String title;
  final String subtitle;
  final List<EnvironmentProbeResult> probes;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text(subtitle, style: const TextStyle(color: Color(0xFF5D6779))),
        const SizedBox(height: 12),
        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: probes
              .map((result) => _ProbeCard(result: result))
              .toList(growable: false),
        ),
      ],
    );
  }
}

class _ProbeCard extends StatelessWidget {
  const _ProbeCard({required this.result});

  final EnvironmentProbeResult result;

  @override
  Widget build(BuildContext context) {
    final okColor = const Color(0xFF067647);
    final errorColor = const Color(0xFFB42318);
    return SizedBox(
      width: 320,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(
                    result.isOk
                        ? Icons.check_circle_rounded
                        : Icons.error_outline_rounded,
                    color: result.isOk ? okColor : errorColor,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      result.label,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                result.message,
                style: TextStyle(
                  color: result.isOk ? okColor : errorColor,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _LoadingCard extends StatelessWidget {
  const _LoadingCard();

  @override
  Widget build(BuildContext context) {
    return const Card(
      child: Padding(
        padding: EdgeInsets.all(24),
        child: Row(
          children: [
            CircularProgressIndicator(),
            SizedBox(width: 16),
            Text('환경 점검 결과를 불러오는 중입니다...'),
          ],
        ),
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Text(
          message,
          style: const TextStyle(color: Color(0xFFB42318)),
        ),
      ),
    );
  }
}
