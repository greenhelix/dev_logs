import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/release_watch_target.dart';

class ReleaseWatchTargetsStore {
  static const _storageKey = 'gah_release_watch_targets_v1';

  Future<List<ReleaseWatchTarget>> load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_storageKey);
    if (raw == null || raw.isEmpty) {
      return const [];
    }
    final payload = jsonDecode(raw) as List<dynamic>;
    return payload
        .map((item) => ReleaseWatchTarget.fromMap(item as Map<String, dynamic>))
        .toList(growable: false);
  }

  Future<void> save(List<ReleaseWatchTarget> targets) async {
    final prefs = await SharedPreferences.getInstance();
    final raw = jsonEncode(
      targets.map((item) => item.toMap()).toList(growable: false),
    );
    await prefs.setString(_storageKey, raw);
  }
}
