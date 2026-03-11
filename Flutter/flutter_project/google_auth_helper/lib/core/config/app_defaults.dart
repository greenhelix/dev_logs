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
    return AppSettings(
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
              resultsDir: '',
              logsDir: '',
              defaultCommand: _defaultCommand(toolType),
              autoUploadAfterRun: true,
            ),
          )
          .toList(growable: false),
    );
  }

  static ToolConfig defaultToolConfig(ToolType toolType) {
    return initialSettings().toolConfigFor(toolType);
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
