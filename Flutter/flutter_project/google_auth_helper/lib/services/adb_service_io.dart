import 'dart:io';

import '../models/adb_snapshot.dart';
import '../models/connected_adb_device.dart';
import 'adb_service.dart';

class IoAdbService implements AdbService {
  @override
  bool get isSupported => Platform.isLinux;

  @override
  Future<AdbSnapshot> inspect() async {
    if (!isSupported) {
      return const AdbSnapshot(
        available: false,
        version: '',
        devices: [],
        message: 'ADB checks are only supported on Linux desktop.',
      );
    }

    try {
      final versionResult = await Process.run('adb', ['version']);
      if (versionResult.exitCode != 0) {
        return AdbSnapshot(
          available: false,
          version: '',
          devices: const [],
          message: (versionResult.stderr as String).trim().isEmpty
              ? 'adb version failed.'
              : (versionResult.stderr as String).trim(),
        );
      }

      final devicesResult = await Process.run('adb', ['devices', '-l']);
      if (devicesResult.exitCode != 0) {
        return AdbSnapshot(
          available: false,
          version: (versionResult.stdout as String).trim(),
          devices: const [],
          message: (devicesResult.stderr as String).trim().isEmpty
              ? 'adb devices failed.'
              : (devicesResult.stderr as String).trim(),
        );
      }

      return AdbSnapshot(
        available: true,
        version: (versionResult.stdout as String).trim(),
        devices: _parseDevices((devicesResult.stdout as String)),
        message: 'ADB is ready.',
      );
    } catch (error) {
      return AdbSnapshot(
        available: false,
        version: '',
        devices: const [],
        message: '$error',
      );
    }
  }

  List<ConnectedAdbDevice> _parseDevices(String stdout) {
    final lines = stdout
        .split('\n')
        .map((line) => line.trim())
        .where((line) => line.isNotEmpty)
        .where((line) => !line.startsWith('List of devices attached'))
        .toList(growable: false);

    return lines
        .map(_parseDeviceLine)
        .whereType<ConnectedAdbDevice>()
        .toList(growable: false);
  }

  ConnectedAdbDevice? _parseDeviceLine(String line) {
    final parts = line.split(RegExp(r'\s+'));
    if (parts.length < 2) {
      return null;
    }

    String modelName = '';
    String product = '';
    String deviceName = '';
    for (final part in parts.skip(2)) {
      if (part.startsWith('model:')) {
        modelName = part.substring('model:'.length);
      } else if (part.startsWith('product:')) {
        product = part.substring('product:'.length);
      } else if (part.startsWith('device:')) {
        deviceName = part.substring('device:'.length);
      }
    }

    return ConnectedAdbDevice(
      serial: parts[0],
      state: parts[1],
      modelName: modelName,
      product: product,
      deviceName: deviceName,
    );
  }
}

AdbService createService() => IoAdbService();
