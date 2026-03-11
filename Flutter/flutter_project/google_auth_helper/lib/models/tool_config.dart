enum ToolType { cts, gts, tvts, vts, sts, ctsOnGsi }

extension ToolTypeX on ToolType {
  String get label {
    switch (this) {
      case ToolType.cts:
        return 'CTS';
      case ToolType.gts:
        return 'GTS';
      case ToolType.tvts:
        return 'TVTS';
      case ToolType.vts:
        return 'VTS';
      case ToolType.sts:
        return 'STS';
      case ToolType.ctsOnGsi:
        return 'CTS on GSI';
    }
  }

  String get runKeyword {
    switch (this) {
      case ToolType.cts:
        return 'cts';
      case ToolType.gts:
        return 'gts';
      case ToolType.tvts:
        return 'tvts';
      case ToolType.vts:
        return 'vts';
      case ToolType.sts:
        return 'sts';
      case ToolType.ctsOnGsi:
        return 'cts';
    }
  }

  String get tradefedExecutable {
    switch (this) {
      case ToolType.cts:
        return 'cts-tradefed';
      case ToolType.gts:
        return 'gts-tradefed';
      case ToolType.tvts:
        return 'tvts-tradefed';
      case ToolType.vts:
        return 'vts-tradefed';
      case ToolType.sts:
        return 'sts-tradefed';
      case ToolType.ctsOnGsi:
        return 'cts-tradefed';
    }
  }

  String get storageKey {
    switch (this) {
      case ToolType.cts:
        return 'cts';
      case ToolType.gts:
        return 'gts';
      case ToolType.tvts:
        return 'tvts';
      case ToolType.vts:
        return 'vts';
      case ToolType.sts:
        return 'sts';
      case ToolType.ctsOnGsi:
        return 'cts_on_gsi';
    }
  }

  static ToolType fromStorageKey(String value) {
    return ToolType.values.firstWhere(
      (toolType) => toolType.storageKey == value,
      orElse: () => ToolType.cts,
    );
  }
}

class ToolConfig {
  const ToolConfig({
    required this.toolType,
    required this.toolRoot,
    required this.resultsDir,
    required this.logsDir,
    required this.defaultCommand,
    required this.deviceSerials,
    required this.shardCount,
    required this.autoUploadAfterRun,
  });

  final ToolType toolType;
  final String toolRoot;
  final String resultsDir;
  final String logsDir;
  final String defaultCommand;
  final List<String> deviceSerials;
  final int shardCount;
  final bool autoUploadAfterRun;

  ToolConfig copyWith({
    String? toolRoot,
    String? resultsDir,
    String? logsDir,
    String? defaultCommand,
    List<String>? deviceSerials,
    int? shardCount,
    bool? autoUploadAfterRun,
  }) {
    return ToolConfig(
      toolType: toolType,
      toolRoot: toolRoot ?? this.toolRoot,
      resultsDir: resultsDir ?? this.resultsDir,
      logsDir: logsDir ?? this.logsDir,
      defaultCommand: defaultCommand ?? this.defaultCommand,
      deviceSerials: deviceSerials ?? this.deviceSerials,
      shardCount: shardCount ?? this.shardCount,
      autoUploadAfterRun: autoUploadAfterRun ?? this.autoUploadAfterRun,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'toolType': toolType.storageKey,
      'toolRoot': toolRoot,
      'resultsDir': resultsDir,
      'logsDir': logsDir,
      'defaultCommand': defaultCommand,
      'deviceSerials': deviceSerials,
      'shardCount': shardCount,
      'autoUploadAfterRun': autoUploadAfterRun,
    };
  }

  factory ToolConfig.fromJson(Map<String, dynamic> json) {
    return ToolConfig(
      toolType: ToolTypeX.fromStorageKey(json['toolType'] as String? ?? 'cts'),
      toolRoot: json['toolRoot'] as String? ?? '',
      resultsDir: json['resultsDir'] as String? ?? '',
      logsDir: json['logsDir'] as String? ?? '',
      defaultCommand: json['defaultCommand'] as String? ?? 'run cts',
      deviceSerials: (json['deviceSerials'] as List<dynamic>? ?? const [])
          .map((item) => item.toString())
          .toList(growable: false),
      shardCount: (json['shardCount'] as num?)?.toInt() ?? 1,
      autoUploadAfterRun: json['autoUploadAfterRun'] as bool? ?? true,
    );
  }
}
