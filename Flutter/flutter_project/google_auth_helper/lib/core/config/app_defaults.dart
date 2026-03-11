import 'package:flutter/foundation.dart';
import 'package:path/path.dart' as path;

import '../../models/app_settings.dart';
import '../../models/tool_config.dart';

class AppDefaults {
  static const appVersion = '0.1.1+2';
  static const firebaseProjectId = 'kani-projects';
  static const firestoreDatabaseId = 'google-auth';
  static const webProxyBaseUrl = '/';
  static const releaseRepo = 'greenhelix/GAH-Release-Repo';
  static const redmineBaseUrl = 'https://redmine.innopiatech.com:17443';
  static const redmineProjectId = '69';

  static AppSettings initialSettings() {
    final mode = kReleaseMode ? AppMode.release : AppMode.dev;
    return AppSettings(
      mode: mode,
      firebaseProjectId: firebaseProjectId,
      firestoreDatabaseId: firestoreDatabaseId,
      credentialMode: CredentialMode.serviceAccountFile,
      serviceAccountPath: '',
      webProxyBaseUrl: webProxyBaseUrl,
      redmineBaseUrl: redmineBaseUrl,
      redmineApiKey: '',
      redmineProjectId: redmineProjectId,
      toolConfigs: ToolType.values
          .map(
            (toolType) => ToolConfig(
              toolType: toolType,
              toolRoot: '',
              resultsDir: _defaultResultsDir(toolType, mode),
              logsDir: _defaultLogsDir(toolType, mode),
              defaultCommand: _defaultCommand(toolType),
              deviceSerials: const [],
              shardCount: 1,
              autoUploadAfterRun: true,
            ),
          )
          .toList(growable: false),
    );
  }

  static ToolConfig defaultToolConfig(ToolType toolType) {
    return initialSettings().toolConfigFor(toolType);
  }

  static String _defaultResultsDir(ToolType toolType, AppMode mode) {
    if (mode == AppMode.release || kIsWeb || toolType != ToolType.cts) {
      return '';
    }
    return path.join('test_sample', 'results');
  }

  static String _defaultLogsDir(ToolType toolType, AppMode mode) {
    if (mode == AppMode.release || kIsWeb || toolType != ToolType.cts) {
      return '';
    }
    return path.join('test_sample', 'logs');
  }

  static String _defaultCommand(ToolType toolType) {
    switch (toolType) {
      case ToolType.cts:
        return 'run cts';
      case ToolType.gts:
        return 'run gts';
      case ToolType.tvts:
        return 'run tvts';
      case ToolType.vts:
        return 'run vts';
      case ToolType.sts:
        return 'run sts';
      case ToolType.ctsOnGsi:
        return 'run cts-on-gsi';
    }
  }
}
