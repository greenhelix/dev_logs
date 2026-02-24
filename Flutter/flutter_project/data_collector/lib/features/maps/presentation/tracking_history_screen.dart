import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../data/providers.dart';
import '../domain/track_record_model.dart';

class TrackingHistoryScreen extends ConsumerWidget {
  const TrackingHistoryScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final historyAsync = ref.watch(trackRecordStreamProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('트래킹 기록')),
      body: historyAsync.when(
        data: (records) {
          if (records.isEmpty) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.route, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text('아직 트래킹 기록이 없습니다.', style: TextStyle(color: Colors.grey)),
                ],
              ),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.all(8),
            itemCount: records.length,
            itemBuilder: (context, index) {
              final record = records[index];
              return _TrackHistoryCard(record: record);
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('오류: $err')),
      ),
    );
  }
}

class _TrackHistoryCard extends ConsumerWidget {
  final TrackRecordModel record;

  const _TrackHistoryCard({required this.record});

  String _formatDate(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} '
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  String _formatDuration(DateTime start, DateTime end) {
    final diff = end.difference(start);
    final h = diff.inHours;
    final m = diff.inMinutes % 60;
    final s = diff.inSeconds % 60;
    if (h > 0) return '${h}시간 ${m}분';
    if (m > 0) return '${m}분 ${s}초';
    return '${s}초';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6, horizontal: 4),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        leading: CircleAvatar(
          backgroundColor: Colors.blue[100],
          child: Text(
            '${record.totalDistance.toStringAsFixed(1)}k',
            style: TextStyle(color: Colors.blue[800], fontSize: 11, fontWeight: FontWeight.bold),
          ),
        ),
        title: Text(record.title, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 4),
            Row(children: [
              const Icon(Icons.play_arrow, size: 14, color: Colors.green),
              const SizedBox(width: 4),
              Text(_formatDate(record.startTime), style: const TextStyle(fontSize: 12)),
            ]),
            Row(children: [
              const Icon(Icons.stop, size: 14, color: Colors.red),
              const SizedBox(width: 4),
              Text(_formatDate(record.endTime), style: const TextStyle(fontSize: 12)),
            ]),
            const SizedBox(height: 4),
            Row(
              children: [
                Icon(Icons.timer_outlined, size: 14, color: Colors.grey[600]),
                const SizedBox(width: 4),
                Text(_formatDuration(record.startTime, record.endTime), style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                const SizedBox(width: 12),
                Icon(Icons.route, size: 14, color: Colors.grey[600]),
                const SizedBox(width: 4),
                Text('${record.totalDistance.toStringAsFixed(2)}km', style: TextStyle(fontSize: 12, color: Colors.grey[600])),
              ],
            ),
            if (record.memo != null && record.memo!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text('📝 ${record.memo}', style: const TextStyle(fontSize: 12, color: Colors.blueGrey)),
              ),
          ],
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              icon: const Icon(Icons.delete_outline, color: Colors.red),
              onPressed: () => _confirmDelete(context, ref),
            ),
            const Icon(Icons.arrow_forward_ios, size: 16),
          ],
        ),
        onTap: () => context.push('/maps/history/detail', extra: record),
      ),
    );
  }

  void _confirmDelete(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('삭제 확인'),
        content: const Text('이 트래킹 기록을 삭제하시겠습니까?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('취소')),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () async {
              await ref.read(trackRecordRepositoryProvider).deleteTrackRecord(record.id);
              if (ctx.mounted) Navigator.pop(ctx);
            },
            child: const Text('삭제', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}
