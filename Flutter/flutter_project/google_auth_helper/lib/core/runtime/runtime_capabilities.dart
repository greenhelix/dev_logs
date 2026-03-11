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
        return 'Read / Upload';
      case RuntimePlatformProfile.windowsDesktop:
        return 'Read / Upload';
      case RuntimePlatformProfile.ubuntuDesktop:
        return 'Read / Upload / Run';
    }
  }

  String get platformLabel {
    switch (profile) {
      case RuntimePlatformProfile.webHosting:
        return 'Web';
      case RuntimePlatformProfile.windowsDesktop:
        return 'Windows';
      case RuntimePlatformProfile.ubuntuDesktop:
        return 'Ubuntu';
    }
  }

  static RuntimeCapabilities detect() {
    if (kIsWeb) {
      return const RuntimeCapabilities(
        profile: RuntimePlatformProfile.webHosting,
        canReadRemote: true,
        canUploadResults: true,
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
          profile: RuntimePlatformProfile.webHosting,
          canReadRemote: true,
          canUploadResults: true,
          canRunTests: false,
          canEditSettings: true,
        );
    }
  }
}
