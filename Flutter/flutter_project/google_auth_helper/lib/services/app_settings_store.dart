import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../core/config/app_defaults.dart';
import '../models/app_settings.dart';

class AppSettingsStore {
  static const _storageKey = 'gah_app_settings_v1';

  Future<AppSettings> load() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_storageKey);
    if (raw == null || raw.isEmpty) {
      return AppDefaults.initialSettings();
    }

    try {
      return AppSettings.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (_) {
      return AppDefaults.initialSettings();
    }
  }

  Future<void> save(AppSettings settings) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_storageKey, jsonEncode(settings.toJson()));
  }
}
