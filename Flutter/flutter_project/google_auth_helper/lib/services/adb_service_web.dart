import '../models/adb_snapshot.dart';
import 'adb_service.dart';

class UnsupportedAdbService implements AdbService {
  @override
  bool get isSupported => false;

  @override
  Future<AdbSnapshot> inspect({String? configuredPath}) async {
    return const AdbSnapshot(
      available: false,
      version: '',
      devices: [],
      message: '웹에서는 ADB 점검을 지원하지 않습니다.',
    );
  }
}

AdbService createService() => UnsupportedAdbService();
