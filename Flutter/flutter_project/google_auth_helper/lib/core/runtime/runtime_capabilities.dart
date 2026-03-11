import 'package:flutter/foundation.dart';

enum RuntimePlatformProfile { webHosting, windowsDesktop, ubuntuDesktop }

class RuntimeCapabilities {
  const RuntimeCapabilities({
    required this.profile,
    required this.canReadRemote,
    required this.canUploadResults,
    required this.canRunTests,
    required this.canEditSettings,
  });

  final RuntimePlatformProfile profile;
  final bool canReadRemote;
  final bool canUploadResults;
  final bool canRunTests;
  final bool canEditSettings;

  String get badgeLabel {
    switch (profile) {
      case RuntimePlatformProfile.webHosting:
        return '조회';
      case RuntimePlatformProfile.windowsDesktop:
        return '조회 / 업로드';
      case RuntimePlatformProfile.ubuntuDesktop:
        return '조회 / 업로드 / 실행';
    }
  }

  String get platformLabel {
    switch (profile) {
      case RuntimePlatformProfile.webHosting:
        return '웹';
      case RuntimePlatformProfile.windowsDesktop:
        return '윈도우';
      case RuntimePlatformProfile.ubuntuDesktop:
        return '우분투';
    }
  }

  static RuntimeCapabilities detect() {
    if (kIsWeb) {
      return const RuntimeCapabilities(
        profile: RuntimePlatformProfile.webHosting,
        canReadRemote: true,
        canUploadResults: false,
        canRunTests: false,
        canEditSettings: true,
      );
    }

    switch (defaultTargetPlatform) {
      case TargetPlatform.windows:
        return const RuntimeCapabilities(
          profile: RuntimePlatformProfile.windowsDesktop,
          canReadRemote: true,
          canUploadResults: true,
          canRunTests: false,
          canEditSettings: true,
        );
      case TargetPlatform.linux:
        return const RuntimeCapabilities(
          profile: RuntimePlatformProfile.ubuntuDesktop,
          canReadRemote: true,
          canUploadResults: true,
          canRunTests: true,
          canEditSettings: true,
        );
      default:
        return const RuntimeCapabilities(
          profile: RuntimePlatformProfile.windowsDesktop,
          canReadRemote: true,
          canUploadResults: true,
          canRunTests: false,
          canEditSettings: true,
        );
    }
  }
}
