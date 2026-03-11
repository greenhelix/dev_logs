import 'package:shared_preferences/shared_preferences.dart';

class UploadHistoryStore {
  static const _storageKey = 'gah_uploaded_file_history_v1';

  Future<List<String>> load() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getStringList(_storageKey) ?? const [];
  }

  Future<List<String>> add(String fileName) async {
    final prefs = await SharedPreferences.getInstance();
    final existing = prefs.getStringList(_storageKey) ?? const [];
    final next = [fileName, ...existing.where((item) => item != fileName)]
        .take(20)
        .toList(growable: false);
    await prefs.setStringList(_storageKey, next);
    return next;
  }
}
