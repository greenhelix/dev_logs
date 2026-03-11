import 'tool_config.dart';

enum CredentialMode { serviceAccountFile, localToken }

extension CredentialModeX on CredentialMode {
  String get label {
    switch (this) {
      case CredentialMode.serviceAccountFile:
        return 'Service Account File';
      case CredentialMode.localToken:
        return 'Local Firebase Token';
    }
  }

  String get storageKey {
    switch (this) {
      case CredentialMode.serviceAccountFile:
        return 'serviceAccountFile';
      case CredentialMode.localToken:
        return 'localToken';
    }
  }

  static CredentialMode fromStorageKey(String value) {
    return value == 'localToken'
        ? CredentialMode.localToken
        : CredentialMode.serviceAccountFile;
  }
}

class AppSettings {
  const AppSettings({
    required this.firebaseProjectId,
    required this.firestoreDatabaseId,
    required this.credentialMode,
    required this.serviceAccountPath,
    required this.webProxyBaseUrl,
    required this.redmineBaseUrl,
    required this.redmineApiKey,
    required this.redmineProjectId,
    required this.toolConfigs,
  });

  final String firebaseProjectId;
  final String firestoreDatabaseId;
  final CredentialMode credentialMode;
  final String serviceAccountPath;
  final String webProxyBaseUrl;
  final String redmineBaseUrl;
  final String redmineApiKey;
  final String redmineProjectId;
  final List<ToolConfig> toolConfigs;

  ToolConfig toolConfigFor(ToolType toolType) {
    return toolConfigs.firstWhere((config) => config.toolType == toolType);
  }

  AppSettings copyWith({
    String? firebaseProjectId,
    String? firestoreDatabaseId,
    CredentialMode? credentialMode,
    String? serviceAccountPath,
    String? webProxyBaseUrl,
    String? redmineBaseUrl,
    String? redmineApiKey,
    String? redmineProjectId,
    List<ToolConfig>? toolConfigs,
  }) {
    return AppSettings(
      firebaseProjectId: firebaseProjectId ?? this.firebaseProjectId,
      firestoreDatabaseId: firestoreDatabaseId ?? this.firestoreDatabaseId,
      credentialMode: credentialMode ?? this.credentialMode,
      serviceAccountPath: serviceAccountPath ?? this.serviceAccountPath,
      webProxyBaseUrl: webProxyBaseUrl ?? this.webProxyBaseUrl,
      redmineBaseUrl: redmineBaseUrl ?? this.redmineBaseUrl,
      redmineApiKey: redmineApiKey ?? this.redmineApiKey,
      redmineProjectId: redmineProjectId ?? this.redmineProjectId,
      toolConfigs: toolConfigs ?? this.toolConfigs,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'firebaseProjectId': firebaseProjectId,
      'firestoreDatabaseId': firestoreDatabaseId,
      'credentialMode': credentialMode.storageKey,
      'serviceAccountPath': serviceAccountPath,
      'webProxyBaseUrl': webProxyBaseUrl,
      'redmineBaseUrl': redmineBaseUrl,
      'redmineApiKey': redmineApiKey,
      'redmineProjectId': redmineProjectId,
      'toolConfigs': toolConfigs.map((config) => config.toJson()).toList(),
    };
  }

  factory AppSettings.fromJson(Map<String, dynamic> json) {
    return AppSettings(
      firebaseProjectId: json['firebaseProjectId'] as String? ?? '',
      firestoreDatabaseId: json['firestoreDatabaseId'] as String? ?? '',
      credentialMode: CredentialModeX.fromStorageKey(
        json['credentialMode'] as String? ?? 'serviceAccountFile',
      ),
      serviceAccountPath: json['serviceAccountPath'] as String? ?? '',
      webProxyBaseUrl: json['webProxyBaseUrl'] as String? ?? '/',
      redmineBaseUrl: json['redmineBaseUrl'] as String? ?? '',
      redmineApiKey: json['redmineApiKey'] as String? ?? '',
      redmineProjectId: json['redmineProjectId'] as String? ?? '',
      toolConfigs: (json['toolConfigs'] as List<dynamic>? ?? const [])
          .map((item) => ToolConfig.fromJson(item as Map<String, dynamic>))
          .toList(growable: false),
    );
  }
}
