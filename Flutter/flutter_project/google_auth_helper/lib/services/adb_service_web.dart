import '../models/adb_snapshot.dart';
import 'adb_service.dart';

class UnsupportedAdbService implements AdbService {
  @override
  bool get isSupported => false;

  @override
  Future<AdbSnapshot> inspect() async {
    return const AdbSnapshot(
      available: false,
      version: '',
      devices: [],
      message: 'ADB is not supported on this platform.',
    );
  }
}

AdbService createService() => UnsupportedAdbService();
