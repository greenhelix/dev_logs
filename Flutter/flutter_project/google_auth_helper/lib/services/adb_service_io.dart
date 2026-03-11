import 'dart:io';

import 'package:path/path.dart' as path;

import '../models/adb_snapshot.dart';
import '../models/connected_adb_device.dart';
import 'adb_service.dart';

class IoAdbService implements AdbService {
  @override
  bool get isSupported => Platform.isLinux || Platform.isWindows;

  @override
  Future<AdbSnapshot> inspect({String? configuredPath}) async {
    if (!isSupported) {
      return const AdbSnapshot(
        available: false,
        version: '',
        devices: [],
        message: '이 플랫폼에서는 ADB 점검을 지원하지 않습니다.',
      );
    }

    try {
      final executable = await _resolveExecutable(configuredPath);
      final versionResult = await Process.run(executable, ['version']);
      if (versionResult.exitCode != 0) {
        return AdbSnapshot(
          available: false,
          version: '',
          devices: const [],
          message: (versionResult.stderr as String).trim().isEmpty
              ? 'adb 버전 확인에 실패했습니다.'
              : (versionResult.stderr as String).trim(),
        );
      }

      final devicesResult = await Process.run(executable, ['devices', '-l']);
      if (devicesResult.exitCode != 0) {
        return AdbSnapshot(
          available: false,
          version: (versionResult.stdout as String).trim(),
          devices: const [],
          message: (devicesResult.stderr as String).trim().isEmpty
              ? 'adb 장치 확인에 실패했습니다.'
              : (devicesResult.stderr as String).trim(),
        );
      }

      return AdbSnapshot(
        available: true,
        version: (versionResult.stdout as String).trim(),
        devices: _parseDevices((devicesResult.stdout as String)),
        message: 'ADB를 사용할 수 있습니다.',
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

  Future<String> _resolveExecutable(String? configuredPath) async {
    final candidates = <String>[
      if (configuredPath != null && configuredPath.trim().isNotEmpty)
        configuredPath.trim(),
      ..._defaultCandidates(),
      Platform.isWindows ? 'adb.exe' : 'adb',
    ];

    for (final candidate in candidates) {
      final normalized = candidate.trim();
      if (normalized.isEmpty) {
        continue;
      }
      if (!path.isAbsolute(normalized)) {
        return normalized;
      }
      if (await File(normalized).exists()) {
        return normalized;
      }
    }

    return Platform.isWindows ? 'adb.exe' : 'adb';
  }

  List<String> _defaultCandidates() {
    final names = Platform.isWindows ? ['adb.exe'] : ['adb'];
    final paths = <String>[];
    final sdkRoots = <String?>[
      Platform.environment['ANDROID_SDK_ROOT'],
      Platform.environment['ANDROID_HOME'],
    ];
    for (final root in sdkRoots) {
      if (root == null || root.trim().isEmpty) {
        continue;
      }
      for (final name in names) {
        paths.add(path.join(root, 'platform-tools', name));
      }
    }

    if (Platform.isWindows) {
      final localAppData = Platform.environment['LOCALAPPDATA'];
      if (localAppData != null && localAppData.isNotEmpty) {
        paths.add(path.join(
          localAppData,
          'Android',
          'Sdk',
          'platform-tools',
          'adb.exe',
        ));
      }
    }
    return paths;
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
