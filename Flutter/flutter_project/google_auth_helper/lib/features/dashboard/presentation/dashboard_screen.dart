import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../models/failed_test_record.dart';
import '../../../models/import_bundle.dart';
import '../../../models/run_session_state.dart';
import '../../../models/test_metric_record.dart';
import '../../../models/tool_config.dart';
import '../../../providers/app_providers.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final metricsAsync = ref.watch(testMetricsProvider);
    final failuresAsync = ref.watch(failedTestsProvider);
    final previewBundle = ref.watch(resultsControllerProvider).previewBundle;
    final runState = ref.watch(runControllerProvider);

    return metricsAsync.when(
      data: (metrics) => failuresAsync.when(
        data: (failures) {
          final mergedMetrics = _mergedMetrics(metrics, previewBundle);
          final mergedFailures = _mergedFailures(failures, previewBundle);
          return ListView(
            children: [
              _CurrentRunCard(
                runState: runState,
                previewBundle: previewBundle,
              ),
              const SizedBox(height: 16),
              const _SectionHeader(
                title: '업로드된 결과 그래프',
                subtitle: '최근 업로드 기준으로 fail/pass 추이와 FW 정보를 함께 봅니다.',
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _Panel(
                    title: '최근 결과 fail/pass',
                    child: _MetricTrendChart(
                        metrics: mergedMetrics.take(6).toList()),
                  ),
                  _Panel(
                    title: '최근 업로드 요약',
                    child: _MetricSummaryList(
                        metrics: mergedMetrics.take(6).toList()),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              const _SectionHeader(
                title: '테스트케이스 통계 그래프',
                subtitle: '실패가 많이 쌓인 테스트케이스와 모듈을 빠르게 확인합니다.',
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _Panel(
                    title: '상위 실패 테스트',
                    child: _FailureFrequencyChart(
                        items: _rankFailures(mergedFailures)),
                  ),
                  _Panel(
                    title: '실패 Top 목록',
                    child: _FailureSummaryList(
                        items: _rankFailures(mergedFailures)),
                  ),
                ],
              ),
            ],
          );
        },
        loading: () => const _LoadingCard('실패 통계를 불러오는 중입니다...'),
        error: (error, _) => _ErrorCard(message: '$error'),
      ),
      loading: () => const _LoadingCard('대시보드 데이터를 불러오는 중입니다...'),
      error: (error, _) => _ErrorCard(message: '$error'),
    );
  }

  List<TestMetricRecord> _mergedMetrics(
      List<TestMetricRecord> metrics, ImportBundle? bundle) {
    final items = [...metrics];
    if (bundle != null && items.every((item) => item.id != bundle.metric.id)) {
      items.insert(0, bundle.metric);
    }
    items.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    return items;
  }

  List<FailedTestRecord> _mergedFailures(
    List<FailedTestRecord> failures,
    ImportBundle? bundle,
  ) {
    final items = [...failures];
    if (bundle != null) {
      for (final failure in bundle.activeFailedTests) {
        if (items.every((item) => item.id != failure.id)) {
          items.insert(0, failure);
        }
      }
    }
    items.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    return items;
  }

  List<_RankedFailure> _rankFailures(List<FailedTestRecord> items) {
    final ranked = <String, _RankedFailure>{};
    for (final item in items) {
      final key = '${item.displayModuleName}#${item.testName}';
      final current = ranked[key];
      ranked[key] = _RankedFailure(
        key: key,
        count: (current?.count ?? 0) + 1,
        summary: item.failureMessage,
      );
    }
    final values = ranked.values.toList()
      ..sort((a, b) => b.count.compareTo(a.count));
    return values.take(8).toList(growable: false);
  }
}

class _CurrentRunCard extends StatelessWidget {
  const _CurrentRunCard({
    required this.runState,
    required this.previewBundle,
  });

  final RunSessionState runState;
  final ImportBundle? previewBundle;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('현재 진행 테스트', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 10),
            if (!runState.isRunning)
              const Text(
                '현재 진행 중인 테스트가 없습니다.',
                style: TextStyle(color: Color(0xFF5D6779)),
              )
            else
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _StatusChip(label: '도구', value: runState.selectedTool.label),
                  _StatusChip(label: '단계', value: runState.stage.name),
                  _StatusChip(label: '샤드', value: '${runState.shardCount}'),
                  _StatusChip(
                    label: '자동 업로드',
                    value: runState.autoUploadAfterRun ? '사용' : '미사용',
                  ),
                ],
              ),
            const SizedBox(height: 14),
            _InfoLine(
              label: '대표 빌드',
              value: previewBundle == null
                  ? '(없음)'
                  : previewBundle!.metric.primaryBuildLabel,
            ),
            _InfoLine(
              label: '빌드 요약',
              value: previewBundle == null
                  ? '(없음)'
                  : previewBundle!.metric.compactBuildLabel,
            ),
            _InfoLine(
              label: '집계 기준',
              value: previewBundle == null
                  ? '(없음)'
                  : previewBundle!.metric.countSource,
            ),
            _InfoLine(
              label: '결과 경로',
              value: runState.detectedResultsDir ?? '(미확인)',
            ),
            _InfoLine(
              label: '로그 경로',
              value: runState.detectedLogsDir ?? '(미확인)',
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({
    required this.title,
    required this.subtitle,
  });

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 4),
        Text(subtitle, style: const TextStyle(color: Color(0xFF5D6779))),
      ],
    );
  }
}

class _Panel extends StatelessWidget {
  const _Panel({
    required this.title,
    required this.child,
  });

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 480,
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 14),
              SizedBox(height: 280, child: child),
            ],
          ),
        ),
      ),
    );
  }
}

class _MetricTrendChart extends StatelessWidget {
  const _MetricTrendChart({required this.metrics});

  final List<TestMetricRecord> metrics;

  @override
  Widget build(BuildContext context) {
    if (metrics.isEmpty) {
      return const _EmptyState(message: '업로드된 결과가 없습니다.');
    }
    final chartMetrics = [...metrics]
      ..sort((a, b) => a.timestamp.compareTo(b.timestamp));
    return BarChart(
      BarChartData(
        borderData: FlBorderData(show: false),
        gridData: const FlGridData(show: true),
        barGroups: [
          for (var i = 0; i < chartMetrics.length; i += 1)
            BarChartGroupData(
              x: i,
              barsSpace: 6,
              barRods: [
                BarChartRodData(
                  toY: chartMetrics[i].failCount.toDouble(),
                  width: 16,
                  color: const Color(0xFFF04438),
                  borderRadius: BorderRadius.circular(6),
                ),
                BarChartRodData(
                  toY: chartMetrics[i].passCount.toDouble(),
                  width: 16,
                  color: const Color(0xFF1459FF),
                  borderRadius: BorderRadius.circular(6),
                ),
              ],
            ),
        ],
        titlesData: FlTitlesData(
          topTitles:
              const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles:
              const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          leftTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              reservedSize: 36,
              getTitlesWidget: (value, meta) => Text(value.toInt().toString()),
            ),
          ),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (value, meta) {
                final index = value.toInt();
                if (index < 0 || index >= chartMetrics.length) {
                  return const SizedBox.shrink();
                }
                final metric = chartMetrics[index];
                return Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    '${metric.timestamp.month}/${metric.timestamp.day}\n${metric.fwVersion}',
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 10),
                  ),
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}

class _MetricSummaryList extends StatelessWidget {
  const _MetricSummaryList({required this.metrics});

  final List<TestMetricRecord> metrics;

  @override
  Widget build(BuildContext context) {
    if (metrics.isEmpty) {
      return const _EmptyState(message: '요약할 결과가 없습니다.');
    }
    return ListView.separated(
      itemCount: metrics.length,
      separatorBuilder: (_, __) => const Divider(height: 18),
      itemBuilder: (context, index) {
        final metric = metrics[index];
        return ListTile(
          contentPadding: EdgeInsets.zero,
          title: Text('${metric.toolType} ${metric.fwVersion}'),
          subtitle: Text(
            '${metric.buildDevice} / Android ${metric.androidVersion} / fail ${metric.failCount}',
          ),
          trailing:
              Text(metric.timestamp.toLocal().toString().split('.').first),
        );
      },
    );
  }
}

class _FailureFrequencyChart extends StatelessWidget {
  const _FailureFrequencyChart({required this.items});

  final List<_RankedFailure> items;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const _EmptyState(message: '실패 통계가 없습니다.');
    }
    return BarChart(
      BarChartData(
        borderData: FlBorderData(show: false),
        gridData: const FlGridData(show: true),
        barGroups: [
          for (var i = 0; i < items.length; i += 1)
            BarChartGroupData(
              x: i,
              barRods: [
                BarChartRodData(
                  toY: items[i].count.toDouble(),
                  width: 20,
                  color: const Color(0xFFF79009),
                  borderRadius: BorderRadius.circular(6),
                ),
              ],
            ),
        ],
        titlesData: FlTitlesData(
          topTitles:
              const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles:
              const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          leftTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              reservedSize: 36,
              getTitlesWidget: (value, meta) => Text(value.toInt().toString()),
            ),
          ),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (value, meta) {
                final index = value.toInt();
                if (index < 0 || index >= items.length) {
                  return const SizedBox.shrink();
                }
                final label = items[index].key.split('#').first;
                return Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    label,
                    style: const TextStyle(fontSize: 10),
                    overflow: TextOverflow.ellipsis,
                  ),
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}

class _FailureSummaryList extends StatelessWidget {
  const _FailureSummaryList({required this.items});

  final List<_RankedFailure> items;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const _EmptyState(message: '실패 항목이 없습니다.');
    }
    return ListView.separated(
      itemCount: items.length,
      separatorBuilder: (_, __) => const Divider(height: 18),
      itemBuilder: (context, index) {
        final item = items[index];
        return ListTile(
          contentPadding: EdgeInsets.zero,
          leading: CircleAvatar(
            backgroundColor: const Color(0xFFFDEAD7),
            child: Text('${index + 1}'),
          ),
          title: Text(item.key),
          subtitle: Text(item.summary),
          trailing: Text('${item.count}회'),
        );
      },
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFFF4F8FF),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: const Color(0xFFD8E3F4)),
      ),
      child: Text('$label: $value'),
    );
  }
}

class _InfoLine extends StatelessWidget {
  const _InfoLine({
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
        children: [
          SizedBox(
            width: 110,
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

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(
        message,
        style: const TextStyle(color: Color(0xFF5D6779)),
      ),
    );
  }
}

class _LoadingCard extends StatelessWidget {
  const _LoadingCard(this.message);

  final String message;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Row(
          children: [
            const CircularProgressIndicator(),
            const SizedBox(width: 16),
            Text(message),
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

class _RankedFailure {
  const _RankedFailure({
    required this.key,
    required this.count,
    required this.summary,
  });

  final String key;
  final int count;
  final String summary;
}
