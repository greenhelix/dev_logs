import 'dart:math' as math;

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../models/failed_test_record.dart';
import '../../../models/import_bundle.dart';
import '../../../models/live_status.dart';
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

    return ListView(
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('대시보드', style: Theme.of(context).textTheme.titleLarge),
                      const SizedBox(height: 8),
                      const Text('품질 추이, 최신 업로드 상태, 실패 집중 구간을 한 번에 확인합니다.'),
                    ],
                  ),
                ),
                FilledButton.tonalIcon(
                  onPressed: () {
                    ref.invalidate(testMetricsProvider);
                    ref.invalidate(failedTestsProvider);
                    ref.invalidate(testCasesProvider);
                  },
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('새로고침'),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        _CurrentRunCard(
          runState: runState,
          previewBundle: previewBundle,
        ),
        const SizedBox(height: 16),
        metricsAsync.when(
          data: (metrics) => failuresAsync.when(
            data: (failures) {
              final mergedMetrics = _mergedMetrics(metrics, previewBundle);
              final mergedFailures = _mergedFailures(failures, previewBundle);
              final rankedFailures = _rankFailures(mergedFailures);
              final latestMetric =
                  mergedMetrics.isEmpty ? previewBundle?.metric : mergedMetrics.first;

              return Column(
                children: [
                  _OverviewStrip(latestMetric: latestMetric),
                  const SizedBox(height: 16),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: [
                      _Panel(
                        title: '품질 추이',
                        subtitle: '최근 업로드 기준 통과율과 실패율 흐름',
                        width: 720,
                        height: 320,
                        child: _QualityTrendChart(
                          metrics: mergedMetrics.take(8).toList(growable: false),
                        ),
                      ),
                      _Panel(
                        title: '최신 실행 구성',
                        subtitle: '가장 최근 업로드의 결과 분포',
                        width: 360,
                        height: 320,
                        child: _LatestCompositionChart(metric: latestMetric),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: [
                      _Panel(
                        title: '실패 집중도',
                        subtitle: '자주 재발하는 상위 실패 항목',
                        width: 720,
                        height: 320,
                        child: _FailureIntensityChart(items: rankedFailures),
                      ),
                      _Panel(
                        title: '최근 업로드 타임라인',
                        subtitle: '최근 6건의 빌드/장치/실패 요약',
                        width: 360,
                        height: 320,
                        child: _MetricSummaryList(
                          metrics: mergedMetrics.take(6).toList(growable: false),
                        ),
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
        ),
      ],
    );
  }
}

List<TestMetricRecord> _mergedMetrics(
  List<TestMetricRecord> metrics,
  ImportBundle? bundle,
) {
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
      module: item.displayModuleName,
      testName: item.testName,
      count: (current?.count ?? 0) + 1,
      summary: item.failureMessage,
    );
  }
  final values = ranked.values.toList()
    ..sort((a, b) => b.count.compareTo(a.count));
  return values.take(7).toList(growable: false);
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
    final ready = runState.isRunning || runState.isConsoleReady;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    '현재 실행 상태',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                AnimatedContainer(
                  duration: const Duration(milliseconds: 280),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: ready
                        ? const Color(0xFFE7F6EC)
                        : const Color(0xFFF2F4F7),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    ready ? _runStageLabel(runState) : '대기',
                    style: TextStyle(
                      color: ready
                          ? const Color(0xFF067647)
                          : const Color(0xFF475467),
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            if (!ready)
              const Text(
                '현재 진행 중인 자동 테스트가 없습니다.',
                style: TextStyle(color: Color(0xFF5D6779)),
              )
            else
              Wrap(
                spacing: 10,
                runSpacing: 10,
                children: [
                  _StatusChip(label: '도구', value: runState.selectedTool.label),
                  _StatusChip(label: '샤드', value: '${runState.shardCount}'),
                  _StatusChip(
                    label: '자동 업로드',
                    value: runState.autoUploadAfterRun ? '사용' : '미사용',
                  ),
                ],
              ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 16,
              runSpacing: 10,
              children: [
                _InfoLine(
                  label: '최근 빌드',
                  value: previewBundle?.metric.primaryBuildLabel ?? '(없음)',
                ),
                _InfoLine(
                  label: '빌드 요약',
                  value: previewBundle?.metric.compactBuildLabel ?? '(없음)',
                ),
                _InfoLine(
                  label: '결과 경로',
                  value: runState.detectedResultsDir ?? '(미설정)',
                ),
                _InfoLine(
                  label: '로그 경로',
                  value: runState.detectedLogsDir ?? '(미설정)',
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _runStageLabel(RunSessionState state) {
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
}

class _OverviewStrip extends StatelessWidget {
  const _OverviewStrip({required this.latestMetric});

  final TestMetricRecord? latestMetric;

  @override
  Widget build(BuildContext context) {
    if (latestMetric == null) {
      return const SizedBox.shrink();
    }

    final metric = latestMetric!;
    final passRate = metric.totalTests == 0
        ? 0
        : (metric.passCount / metric.totalTests) * 100;

    return Wrap(
      spacing: 12,
      runSpacing: 12,
      children: [
        _KpiCard(
          label: '통과율',
          value: '${passRate.toStringAsFixed(1)}%',
          accent: const Color(0xFF1570EF),
          note: '${metric.passCount}/${metric.totalTests} 통과',
        ),
        _KpiCard(
          label: '실패',
          value: '${metric.reportedFailCount}',
          accent: const Color(0xFFF04438),
          note: '제외 ${metric.excludedFailureCount}건 반영',
        ),
        _KpiCard(
          label: '소요 시간',
          value: _formatDuration(metric.durationSeconds),
          accent: const Color(0xFF12B76A),
          note: '모듈 ${metric.moduleCount}개',
        ),
        _KpiCard(
          label: '장치 수',
          value: '${metric.devices.length}',
          accent: const Color(0xFFF79009),
          note: metric.buildDevice.isEmpty ? '장치 정보 없음' : metric.buildDevice,
        ),
      ],
    );
  }
}

class _KpiCard extends StatelessWidget {
  const _KpiCard({
    required this.label,
    required this.value,
    required this.accent,
    required this.note,
  });

  final String label;
  final String value;
  final Color accent;
  final String note;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 420),
      curve: Curves.easeOutCubic,
      builder: (context, t, child) {
        return Transform.translate(
          offset: Offset(0, 14 * (1 - t)),
          child: Opacity(opacity: t, child: child),
        );
      },
      child: SizedBox(
        width: 264,
        child: DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [
                accent.withAlpha(18),
                Colors.white,
              ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: accent.withAlpha(36)),
          ),
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    color: Color(0xFF5D6779),
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  value,
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                        fontWeight: FontWeight.w900,
                        color: const Color(0xFF101828),
                      ),
                ),
                const SizedBox(height: 8),
                Text(
                  note,
                  style: const TextStyle(
                    color: Color(0xFF475467),
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _Panel extends StatelessWidget {
  const _Panel({
    required this.title,
    required this.subtitle,
    required this.width,
    required this.height,
    required this.child,
  });

  final String title;
  final String subtitle;
  final double width;
  final double height;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 460),
      curve: Curves.easeOutCubic,
      builder: (context, t, child) {
        return Transform.translate(
          offset: Offset(0, 18 * (1 - t)),
          child: Opacity(opacity: t, child: child),
        );
      },
      child: SizedBox(
        width: width,
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 6),
                Text(
                  subtitle,
                  style: const TextStyle(color: Color(0xFF5D6779)),
                ),
                const SizedBox(height: 16),
                SizedBox(height: height, child: child),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _QualityTrendChart extends StatelessWidget {
  const _QualityTrendChart({required this.metrics});

  final List<TestMetricRecord> metrics;

  @override
  Widget build(BuildContext context) {
    if (metrics.isEmpty) {
      return const _EmptyState(message: '업로드된 결과가 없습니다.');
    }

    final chartMetrics = [...metrics]
      ..sort((a, b) => a.timestamp.compareTo(b.timestamp));

    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 700),
      curve: Curves.easeOutCubic,
      builder: (context, progress, _) {
        return LineChart(
          LineChartData(
            minY: 0,
            maxY: 100,
            gridData: FlGridData(
              show: true,
              drawVerticalLine: false,
              horizontalInterval: 20,
              getDrawingHorizontalLine: (_) => const FlLine(
                color: Color(0xFFE4E7EC),
                strokeWidth: 1,
              ),
            ),
            borderData: FlBorderData(show: false),
            lineTouchData: LineTouchData(
              handleBuiltInTouches: true,
              touchTooltipData: LineTouchTooltipData(
                getTooltipColor: (_) => const Color(0xFF101828),
                getTooltipItems: (spots) {
                  return spots.map((spot) {
                    final metric = chartMetrics[spot.x.toInt()];
                    final label = spot.barIndex == 0 ? '통과율' : '실패율';
                    return LineTooltipItem(
                      '$label ${spot.y.toStringAsFixed(1)}%\n'
                      '${metric.timestamp.month}/${metric.timestamp.day} ${metric.fwVersion}\n'
                      '통과 ${metric.passCount} / 실패 ${metric.reportedFailCount}',
                      const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                        fontSize: 12,
                      ),
                    );
                  }).toList(growable: false);
                },
              ),
            ),
            titlesData: FlTitlesData(
              topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
              rightTitles:
                  const AxisTitles(sideTitles: SideTitles(showTitles: false)),
              leftTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  reservedSize: 42,
                  interval: 20,
                  getTitlesWidget: (value, meta) => Text(
                    '${value.toInt()}%',
                    style: const TextStyle(
                      color: Color(0xFF667085),
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
              bottomTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  reservedSize: 44,
                  getTitlesWidget: (value, meta) {
                    final index = value.toInt();
                    if (index < 0 || index >= chartMetrics.length) {
                      return const SizedBox.shrink();
                    }
                    final metric = chartMetrics[index];
                    return Padding(
                      padding: const EdgeInsets.only(top: 10),
                      child: Text(
                        '${metric.timestamp.month}/${metric.timestamp.day}',
                        style: const TextStyle(
                          color: Color(0xFF667085),
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    );
                  },
                ),
              ),
            ),
            lineBarsData: [
              LineChartBarData(
                spots: [
                  for (var i = 0; i < chartMetrics.length; i += 1)
                    FlSpot(i.toDouble(), _passRate(chartMetrics[i]) * progress),
                ],
                isCurved: true,
                curveSmoothness: 0.32,
                barWidth: 4,
                color: const Color(0xFF1570EF),
                dotData: FlDotData(
                  show: true,
                  getDotPainter: (spot, percent, bar, index) {
                    return FlDotCirclePainter(
                      radius: 4.2,
                      color: const Color(0xFFFFFFFF),
                      strokeWidth: 3,
                      strokeColor: const Color(0xFF1570EF),
                    );
                  },
                ),
                belowBarData: BarAreaData(
                  show: true,
                  gradient: LinearGradient(
                    colors: [
                      const Color(0xFF1570EF).withAlpha(76),
                      const Color(0xFF1570EF).withAlpha(10),
                    ],
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                  ),
                ),
              ),
              LineChartBarData(
                spots: [
                  for (var i = 0; i < chartMetrics.length; i += 1)
                    FlSpot(i.toDouble(), _failureRate(chartMetrics[i]) * progress),
                ],
                isCurved: true,
                curveSmoothness: 0.28,
                barWidth: 3,
                color: const Color(0xFFF04438),
                dashArray: const [8, 5],
                dotData: const FlDotData(show: false),
                belowBarData: BarAreaData(show: false),
              ),
            ],
            minX: 0,
            maxX: math.max(0, chartMetrics.length - 1).toDouble(),
          ),
        );
      },
    );
  }

  double _passRate(TestMetricRecord metric) {
    if (metric.totalTests == 0) {
      return 0;
    }
    return (metric.passCount / metric.totalTests) * 100;
  }

  double _failureRate(TestMetricRecord metric) {
    if (metric.totalTests == 0) {
      return 0;
    }
    return (metric.reportedFailCount / metric.totalTests) * 100;
  }
}

class _LatestCompositionChart extends StatelessWidget {
  const _LatestCompositionChart({required this.metric});

  final TestMetricRecord? metric;

  @override
  Widget build(BuildContext context) {
    if (metric == null) {
      return const _EmptyState(message: '최신 업로드 데이터가 없습니다.');
    }

    final data = <_SliceData>[
      _SliceData(
        label: '통과',
        value: metric!.passCount.toDouble(),
        color: const Color(0xFF12B76A),
      ),
      _SliceData(
        label: '실패',
        value: metric!.reportedFailCount.toDouble(),
        color: const Color(0xFFF04438),
      ),
      _SliceData(
        label: '제외',
        value: metric!.excludedFailureCount.toDouble(),
        color: const Color(0xFFF79009),
      ),
      _SliceData(
        label: '기타',
        value: (metric!.ignoredCount + metric!.assumptionFailureCount).toDouble(),
        color: const Color(0xFF98A2B3),
      ),
    ].where((item) => item.value > 0).toList(growable: false);

    if (data.isEmpty) {
      return const _EmptyState(message: '표시할 분포 데이터가 없습니다.');
    }

    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 650),
      curve: Curves.easeOutCubic,
      builder: (context, progress, _) {
        return Row(
          children: [
            Expanded(
              child: PieChart(
                PieChartData(
                  centerSpaceRadius: 58,
                  sectionsSpace: 4,
                  pieTouchData: PieTouchData(
                    touchCallback: (_, __) {},
                  ),
                  sections: [
                    for (final item in data)
                      PieChartSectionData(
                        value: item.value * progress,
                        color: item.color,
                        radius: 56,
                        title: '${item.percentOf(data).toStringAsFixed(0)}%',
                        titleStyle: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w800,
                          fontSize: 12,
                        ),
                      ),
                  ],
                ),
              ),
            ),
            const SizedBox(width: 18),
            SizedBox(
              width: 120,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    metric!.fwVersion.isEmpty ? '최신 업로드' : metric!.fwVersion,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w900,
                        ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    metric!.buildDevice.isEmpty ? '장치 정보 없음' : metric!.buildDevice,
                    style: const TextStyle(
                      color: Color(0xFF5D6779),
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 16),
                  ...data.map(
                    (item) => Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _LegendRow(item: item),
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}

class _LegendRow extends StatelessWidget {
  const _LegendRow({required this.item});

  final _SliceData item;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 10,
          height: 10,
          decoration: BoxDecoration(
            color: item.color,
            borderRadius: BorderRadius.circular(999),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            item.label,
            style: const TextStyle(
              color: Color(0xFF344054),
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        Text(
          item.value.toInt().toString(),
          style: const TextStyle(
            color: Color(0xFF101828),
            fontWeight: FontWeight.w800,
          ),
        ),
      ],
    );
  }
}

class _FailureIntensityChart extends StatelessWidget {
  const _FailureIntensityChart({required this.items});

  final List<_RankedFailure> items;

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return const _EmptyState(message: '실패 통계가 없습니다.');
    }

    final maxCount = items
        .map((item) => item.count)
        .fold<int>(0, (prev, next) => math.max(prev, next));

    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: const Duration(milliseconds: 700),
      curve: Curves.easeOutCubic,
      builder: (context, progress, _) {
        return BarChart(
          BarChartData(
            alignment: BarChartAlignment.spaceAround,
            maxY: (maxCount + 1).toDouble(),
            gridData: FlGridData(
              show: true,
              drawVerticalLine: false,
              horizontalInterval: 1,
              getDrawingHorizontalLine: (_) => const FlLine(
                color: Color(0xFFE4E7EC),
                strokeWidth: 1,
              ),
            ),
            borderData: FlBorderData(show: false),
            barTouchData: BarTouchData(
              enabled: true,
              touchTooltipData: BarTouchTooltipData(
                getTooltipColor: (_) => const Color(0xFF101828),
                getTooltipItem: (group, groupIndex, rod, rodIndex) {
                  final item = items[group.x.toInt()];
                  return BarTooltipItem(
                    '${item.module}\n${item.testName}\n${item.count}회',
                    const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                    ),
                  );
                },
              ),
            ),
            titlesData: FlTitlesData(
              topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
              rightTitles:
                  const AxisTitles(sideTitles: SideTitles(showTitles: false)),
              leftTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  reservedSize: 28,
                  interval: 1,
                  getTitlesWidget: (value, meta) => Text(
                    value.toInt().toString(),
                    style: const TextStyle(
                      color: Color(0xFF667085),
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
              bottomTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  reservedSize: 44,
                  getTitlesWidget: (value, meta) {
                    final index = value.toInt();
                    if (index < 0 || index >= items.length) {
                      return const SizedBox.shrink();
                    }
                    return Padding(
                      padding: const EdgeInsets.only(top: 10),
                      child: Text(
                        _shortLabel(items[index].module),
                        style: const TextStyle(
                          color: Color(0xFF667085),
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    );
                  },
                ),
              ),
            ),
            barGroups: [
              for (var i = 0; i < items.length; i += 1)
                BarChartGroupData(
                  x: i,
                  barRods: [
                    BarChartRodData(
                      toY: items[i].count * progress,
                      width: 28,
                      gradient: const LinearGradient(
                        colors: [
                          Color(0xFFFFB547),
                          Color(0xFFF04438),
                        ],
                        begin: Alignment.bottomCenter,
                        end: Alignment.topCenter,
                      ),
                      borderRadius: const BorderRadius.only(
                        topLeft: Radius.circular(12),
                        topRight: Radius.circular(12),
                      ),
                      backDrawRodData: BackgroundBarChartRodData(
                        show: true,
                        toY: maxCount.toDouble(),
                        color: const Color(0xFFF2F4F7),
                      ),
                    ),
                  ],
                ),
            ],
          ),
        );
      },
    );
  }

  String _shortLabel(String text) {
    if (text.length <= 10) {
      return text;
    }
    return '${text.substring(0, 9)}…';
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
      separatorBuilder: (_, __) => const SizedBox(height: 10),
      itemBuilder: (context, index) {
        final metric = metrics[index];
        final passRate = metric.totalTests == 0
            ? 0
            : (metric.passCount / metric.totalTests) * 100;
        return AnimatedContainer(
          duration: Duration(milliseconds: 220 + (index * 40)),
          curve: Curves.easeOutCubic,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: const Color(0xFFF8FAFC),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFFD8E3F4)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      metric.fwVersion.isEmpty ? metric.toolType : metric.fwVersion,
                      style: const TextStyle(
                        color: Color(0xFF101828),
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  Text(
                    '${metric.timestamp.month}/${metric.timestamp.day}',
                    style: const TextStyle(
                      color: Color(0xFF667085),
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                metric.compactBuildLabel,
                style: const TextStyle(
                  color: Color(0xFF475467),
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  _TinyBadge(
                    label: '통과율',
                    value: '${passRate.toStringAsFixed(0)}%',
                    color: const Color(0xFF1570EF),
                  ),
                  const SizedBox(width: 8),
                  _TinyBadge(
                    label: '실패',
                    value: '${metric.reportedFailCount}',
                    color: const Color(0xFFF04438),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }
}

class _TinyBadge extends StatelessWidget {
  const _TinyBadge({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: color.withAlpha(20),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        '$label $value',
        style: TextStyle(
          color: color,
          fontWeight: FontWeight.w800,
          fontSize: 12,
        ),
      ),
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
    return SizedBox(
      width: 520,
      child: Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 110,
              child: Text(
                label,
                style: const TextStyle(color: Color(0xFF5D6779)),
              ),
            ),
            Expanded(
              child: Text(
                value,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ),
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

class _SliceData {
  const _SliceData({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final double value;
  final Color color;

  double percentOf(List<_SliceData> all) {
    final total = all.fold<double>(0, (sum, item) => sum + item.value);
    if (total == 0) {
      return 0;
    }
    return (value / total) * 100;
  }
}

class _RankedFailure {
  const _RankedFailure({
    required this.key,
    required this.module,
    required this.testName,
    required this.count,
    required this.summary,
  });

  final String key;
  final String module;
  final String testName;
  final int count;
  final String summary;
}

String _formatDuration(int seconds) {
  if (seconds <= 0) {
    return '0m';
  }
  final hours = seconds ~/ 3600;
  final minutes = (seconds % 3600) ~/ 60;
  if (hours > 0) {
    return '${hours}h ${minutes}m';
  }
  return '${minutes}m';
}
