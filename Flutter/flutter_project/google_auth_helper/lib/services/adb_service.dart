import '../models/adb_snapshot.dart';
import 'adb_service_web.dart' if (dart.library.io) 'adb_service_io.dart';

abstract class AdbService {
  bool get isSupported;

  Future<AdbSnapshot> inspect();
}

AdbService createAdbService() => createService();
