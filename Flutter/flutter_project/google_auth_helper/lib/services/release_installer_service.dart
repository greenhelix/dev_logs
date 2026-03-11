import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../models/release_status.dart';

class ReleaseInstallerService {
  ReleaseInstallerService({http.Client? httpClient})
      : _httpClient = httpClient ?? http.Client();

  final http.Client _httpClient;

  bool get isSupported => !kIsWeb && (Platform.isWindows || Platform.isLinux);

  Future<String> downloadInstaller(ReleaseStatus status) async {
    final asset = status.installerAsset;
    if (asset == null) {
      throw StateError('설치 파일 정보를 찾지 못했습니다.');
    }

    final request = http.Request('GET', Uri.parse(asset.downloadUrl));
    final response = await _httpClient.send(request);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw StateError('설치 파일 다운로드에 실패했습니다. (${response.statusCode})');
    }

    final targetDirectory =
        await Directory.systemTemp.createTemp('gah_update_');
    final targetPath = '${targetDirectory.path}${Platform.pathSeparator}${asset.name}';
    final file = File(targetPath);
    await response.stream.pipe(file.openWrite());
    return targetPath;
  }

  Future<void> openInstaller(String installerPath) async {
    if (Platform.isWindows) {
      await Process.start(
        installerPath,
        const [],
        mode: ProcessStartMode.detached,
      );
      return;
    }
    if (Platform.isLinux) {
      await Process.start(
        'xdg-open',
        [installerPath],
        mode: ProcessStartMode.detached,
      );
      return;
    }
    throw UnsupportedError('이 플랫폼에서는 설치 프로그램 실행을 지원하지 않습니다.');
  }
}
