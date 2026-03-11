import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../models/environment_check_status.dart';
import '../../../providers/app_providers.dart';

class EnvironmentScreen extends ConsumerWidget {
  const EnvironmentScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final envAsync = ref.watch(environmentStatusProvider);

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Environment',
                    style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                const Text(
                  'Check Firebase, Firestore, ADB, and Redmine connectivity from the current runtime.',
                ),
                const SizedBox(height: 12),
                FilledButton.tonalIcon(
                  onPressed: () => ref.invalidate(environmentStatusProvider),
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Refresh Status'),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        envAsync.when(
          data: (status) => _EnvironmentResult(status: status),
          loading: () => const _LoadingCard(),
          error: (error, _) => _ErrorCard(message: error.toString()),
        ),
      ],
    );
  }
}

class _EnvironmentResult extends StatelessWidget {
  const _EnvironmentResult({required this.status});

  final EnvironmentCheckStatus status;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _ProbeSection(
          title: 'Firebase',
          subtitle: 'Hosting and Firestore connectivity.',
          probes: status.firebaseResults,
        ),
        const SizedBox(height: 16),
        _ProbeSection(
          title: 'Local Tools',
          subtitle: 'ADB availability and detected devices.',
          probes: status.localResults,
        ),
        const SizedBox(height: 16),
        _ProbeSection(
          title: 'Redmine',
          subtitle: 'Connection, current user, and project access.',
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
                    color: result.isOk
                        ? const Color(0xFF067647)
                        : const Color(0xFFB42318),
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
                  color: result.isOk
                      ? const Color(0xFF067647)
                      : const Color(0xFFB42318),
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
            Text('Checking environment status...'),
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
