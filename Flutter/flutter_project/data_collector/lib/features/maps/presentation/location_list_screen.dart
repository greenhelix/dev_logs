import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/widgets/responsive_list_tile.dart';
import '../../../data/providers.dart';

class LocationListScreen extends ConsumerWidget {
  const LocationListScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final locationsAsync = ref.watch(locationsStreamProvider);
    final dateFormat = DateFormat('MM-dd HH:mm:ss');

    return Scaffold(
      appBar: AppBar(
        title: const Text('Location History'),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_forever),
            onPressed: () => _deleteAll(context, ref),
          )
        ],
      ),
      body: locationsAsync.when(
        data: (locations) => ListView.builder(
          itemCount: locations.length,
          itemBuilder: (context, index) {
            final loc = locations[index];
            return ResponsiveListTile(
              enableEdit: false,
              onEdit: () {},
              onDelete: () =>
                  ref.read(locationRepositoryProvider).deleteLocation(loc.id),
              child: ListTile(
                leading: const Icon(Icons.place, color: Colors.blue),
                title: Text(
                    '${loc.latitude.toStringAsFixed(5)}, ${loc.longitude.toStringAsFixed(5)}'),
                subtitle: Text(
                    '${dateFormat.format(loc.timestamp)} • ${loc.notes ?? ''}'),
              ),
            );
          },
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, s) => Center(child: Text('$e')),
      ),
    );
  }

  void _deleteAll(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('전체 삭제'),
        content: const Text('모든 위치 기록을 삭제하시겠습니까?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context), child: const Text('취소')),
          TextButton(
            onPressed: () {
              ref.read(locationRepositoryProvider).deleteAllLocations();
              Navigator.pop(context);
            },
            child: const Text('삭제', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
