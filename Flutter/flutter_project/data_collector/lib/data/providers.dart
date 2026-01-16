import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'local/app_database.dart';

// ─── Database Provider ───────────────────────
/// 앱 전체에서 하나의 데이터베이스 인스턴스를 공유합니다.
/// 
/// 사용 예시:
/// ```dart
/// final db = ref.read(databaseProvider);
/// await db.into(db.people).insert(...);
/// ```
final databaseProvider = Provider<AppDatabase>((ref) {
  final database = AppDatabase();
  
  // Provider가 dispose될 때 DB 연결도 닫습니다.
  ref.onDispose(() {
    database.close();
  });
  
  return database;
});

// ─── Settings Provider (Future Extension) ────
/// 사용자가 "Firebase vs 로컬 DB" 중 선택한 설정을 저장하는 Provider
/// 추후 SharedPreferences와 연결하여 구현할 예정입니다.
/// 
/// 예: final useCloud = ref.watch(settingsProvider).useCloudStorage;
final settingsProvider = StateProvider<AppSettings>((ref) {
  return AppSettings(useCloudStorage: false); // 기본값: 로컬 DB 사용
});

/// 앱 설정 데이터 모델
class AppSettings {
  final bool useCloudStorage;
  
  AppSettings({required this.useCloudStorage});
  
  AppSettings copyWith({bool? useCloudStorage}) {
    return AppSettings(
      useCloudStorage: useCloudStorage ?? this.useCloudStorage,
    );
  }
}
