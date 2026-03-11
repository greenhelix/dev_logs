import 'connected_adb_device.dart';

class AdbSnapshot {
  const AdbSnapshot({
    required this.available,
    required this.version,
    required this.devices,
    this.message = '',
  });

  final bool available;
  final String version;
  final List<ConnectedAdbDevice> devices;
  final String message;
}
