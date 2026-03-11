import 'dart:async';
import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/legacy.dart';

import '../core/config/app_defaults.dart';
import '../core/runtime/runtime_capabilities.dart';
import '../data/firestore_repository.dart';
import '../data/firestore_rest_client.dart';
import '../models/app_settings.dart';
import '../models/console_health.dart';
import '../models/environment_check_status.dart';
import '../models/failed_test_record.dart';
import '../models/import_bundle.dart';
import '../models/live_status.dart';
import '../models/release_status.dart';
import '../models/release_watch_snapshot.dart';
import '../models/release_watch_target.dart';
import '../models/run_request.dart';
import '../models/run_session_state.dart';
import '../models/tool_config.dart';
import '../models/upload_target.dart';
import '../services/adb_service.dart';
import '../services/app_settings_store.dart';
import '../services/archive_import_service.dart';
import '../services/auth_header_provider.dart';
import '../services/environment_check_service.dart';
import '../services/import_service.dart';
import '../services/local_file_gateway.dart';
import '../services/redmine_service.dart';
import '../services/release_update_service.dart';
import '../services/release_watcher_artifact_service.dart';
import '../services/release_watch_targets_store.dart';
import '../services/upload_history_store.dart';
import '../services/xts_execution_service.dart';
import '../services/xts_live_log_parser.dart';
import '../services/xts_result_parser.dart';
import '../services/xts_tf_output_parser.dart';

enum ResultsLoadStage { idle, selectingFile, fileLoaded, parsing, ready, error }

enum UploadArchiveSlot { result, log }

const _unset = Object();

class _PendingArchive {
  const _PendingArchive({
    required this.fileName,
    required this.bytes,
  });

  final String fileName;
  final Uint8List bytes;
}

class AppSettingsState {
  const AppSettingsState({
    required this.settings,
    required this.isLoading,
    this.errorMessage,
  });

  final AppSettings settings;
  final bool isLoading;
  final String? errorMessage;

  AppSettingsState copyWith({
    AppSettings? settings,
    bool? isLoading,
    Object? errorMessage = _unset,
  }) {
    return AppSettingsState(
      settings: settings ?? this.settings,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: identical(errorMessage, _unset)
          ? this.errorMessage
          : errorMessage as String?,
    );
  }
}

class ReleaseWatchTargetsState {
  const ReleaseWatchTargetsState({
    this.targets = const [],
    this.isLoading = false,
  });

  final List<ReleaseWatchTarget> targets;
  final bool isLoading;

  ReleaseWatchTargetsState copyWith({
    List<ReleaseWatchTarget>? targets,
    bool? isLoading,
  }) {
    return ReleaseWatchTargetsState(
      targets: targets ?? this.targets,
      isLoading: isLoading ?? this.isLoading,
    );
  }
}

class ResultsState {
  const ResultsState({
    required this.selectedTool,
    required this.uploadTarget,
    this.previewBundle,
    this.baselineBundle,
    this.redmineMarkdown = '',
    this.isLoading = false,
    this.isUploading = false,
    this.usingDemoData = false,
    this.initialized = false,
    this.resultArchiveName,
    this.logArchiveName,
    this.selectedAt,
    this.loadStage = ResultsLoadStage.idle,
    this.loadError,
    this.resultArchiveLoaded = false,
    this.logArchiveLoaded = false,
    this.previewWarnings = const [],
    this.history = const [],
    this.message,
  });

  final ToolType selectedTool;
  final UploadTarget uploadTarget;
  final ImportBundle? previewBundle;
  final ImportBundle? baselineBundle;
  final String redmineMarkdown;
  final bool isLoading;
  final bool isUploading;
  final bool usingDemoData;
  final bool initialized;
  final String? resultArchiveName;
  final String? logArchiveName;
  final DateTime? selectedAt;
  final ResultsLoadStage loadStage;
  final String? loadError;
  final bool resultArchiveLoaded;
  final bool logArchiveLoaded;
  final List<String> previewWarnings;
  final List<String> history;
  final String? message;

  bool get hasUploadablePreview {
    return previewBundle != null &&
        loadError == null &&
        loadStage != ResultsLoadStage.parsing;
  }

  ResultsState copyWith({
    ToolType? selectedTool,
    UploadTarget? uploadTarget,
    Object? previewBundle = _unset,
    Object? baselineBundle = _unset,
    Object? redmineMarkdown = _unset,
    bool? isLoading,
    bool? isUploading,
    bool? usingDemoData,
    bool? initialized,
    Object? resultArchiveName = _unset,
    Object? logArchiveName = _unset,
    Object? selectedAt = _unset,
    ResultsLoadStage? loadStage,
    Object? loadError = _unset,
    bool? resultArchiveLoaded,
    bool? logArchiveLoaded,
    List<String>? previewWarnings,
    List<String>? history,
    Object? message = _unset,
  }) {
    return ResultsState(
      selectedTool: selectedTool ?? this.selectedTool,
      uploadTarget: uploadTarget ?? this.uploadTarget,
      previewBundle: identical(previewBundle, _unset)
          ? this.previewBundle
          : previewBundle as ImportBundle?,
      baselineBundle: identical(baselineBundle, _unset)
          ? this.baselineBundle
          : baselineBundle as ImportBundle?,
      redmineMarkdown: identical(redmineMarkdown, _unset)
          ? this.redmineMarkdown
          : redmineMarkdown as String,
      isLoading: isLoading ?? this.isLoading,
      isUploading: isUploading ?? this.isUploading,
      usingDemoData: usingDemoData ?? this.usingDemoData,
      initialized: initialized ?? this.initialized,
      resultArchiveName: identical(resultArchiveName, _unset)
          ? this.resultArchiveName
          : resultArchiveName as String?,
      logArchiveName: identical(logArchiveName, _unset)
          ? this.logArchiveName
          : logArchiveName as String?,
      selectedAt: identical(selectedAt, _unset)
          ? this.selectedAt
          : selectedAt as DateTime?,
      loadStage: loadStage ?? this.loadStage,
      loadError:
          identical(loadError, _unset) ? this.loadError : loadError as String?,
      resultArchiveLoaded: resultArchiveLoaded ?? this.resultArchiveLoaded,
      logArchiveLoaded: logArchiveLoaded ?? this.logArchiveLoaded,
      previewWarnings: previewWarnings ?? this.previewWarnings,
      history: history ?? this.history,
      message: identical(message, _unset) ? this.message : message as String?,
    );
  }
}

class AppSettingsController extends StateNotifier<AppSettingsState> {
  AppSettingsController(this._store)
      : super(
          AppSettingsState(
            settings: AppDefaults.initialSettings(),
            isLoading: true,
          ),
        );

  final AppSettingsStore _store;

  Future<void> load() async {
    try {
      final settings = await _store.load();
      state = AppSettingsState(settings: settings, isLoading: false);
    } catch (error) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: error.toString(),
      );
    }
  }

  Future<void> updateSettings(AppSettings settings) async {
    state = state.copyWith(
      settings: settings,
      isLoading: true,
      errorMessage: null,
    );
    await _store.save(settings);
    state = state.copyWith(isLoading: false);
  }

  Future<void> updateToolConfig(ToolConfig config) async {
    final updated = state.settings.toolConfigs
        .map((item) => item.toolType == config.toolType ? config : item)
        .toList(growable: false);
    await updateSettings(state.settings.copyWith(toolConfigs: updated));
  }
}

class ReleaseWatchTargetsController
    extends StateNotifier<ReleaseWatchTargetsState> {
  ReleaseWatchTargetsController(this._store)
      : super(const ReleaseWatchTargetsState(isLoading: true));

  final ReleaseWatchTargetsStore _store;

  Future<void> load() async {
    final targets = await _store.load();
    state = state.copyWith(targets: targets, isLoading: false);
  }

  Future<void> addTarget(ReleaseWatchTarget target) async {
    final updated = [target, ...state.targets];
    state = state.copyWith(targets: updated, isLoading: false);
    await _store.save(updated);
  }

  Future<void> removeTarget(String id) async {
    final updated =
        state.targets.where((item) => item.id != id).toList(growable: false);
    state = state.copyWith(targets: updated, isLoading: false);
    await _store.save(updated);
  }
}

class ResultsController extends StateNotifier<ResultsState> {
  ResultsController(this._ref)
      : super(
          const ResultsState(
            selectedTool: ToolType.cts,
            uploadTarget: UploadTarget.firestore,
          ),
        );

  final Ref _ref;
  _PendingArchive? _pendingResultArchive;
  _PendingArchive? _pendingLogArchive;

  Future<void> initialize() async {
    if (state.initialized) {
      return;
    }
    final history = await _ref.read(uploadHistoryStoreProvider).load();
    state = state.copyWith(
      history: history,
      initialized: true,
      loadStage: ResultsLoadStage.idle,
      message: 'Upload result and log zip files to build a preview.',
    );
  }

  Future<void> selectTool(ToolType toolType) async {
    _pendingResultArchive = null;
    _pendingLogArchive = null;
    state = state.copyWith(
      selectedTool: toolType,
      previewBundle: null,
      baselineBundle: null,
      redmineMarkdown: '',
      resultArchiveName: null,
      logArchiveName: null,
      resultArchiveLoaded: false,
      logArchiveLoaded: false,
      loadStage: ResultsLoadStage.idle,
      message: null,
      loadError: null,
      previewWarnings: const [],
    );
  }

  void selectUploadTarget(UploadTarget target) {
    state = state.copyWith(
      uploadTarget: target,
      message: '${target.label} target selected.',
    );
  }

  void registerSelectedArchive(UploadArchiveSlot slot, String fileName) {
    state = state.copyWith(
      resultArchiveName:
          slot == UploadArchiveSlot.result ? fileName : state.resultArchiveName,
      logArchiveName:
          slot == UploadArchiveSlot.log ? fileName : state.logArchiveName,
      selectedAt: DateTime.now(),
      loadStage: ResultsLoadStage.selectingFile,
      loadError: null,
      previewWarnings: const [],
      message: '$fileName selected.',
    );
  }

  void registerArchiveReadFailure(
    UploadArchiveSlot slot,
    String fileName,
    Object error,
  ) {
    state = state.copyWith(
      resultArchiveName:
          slot == UploadArchiveSlot.result ? fileName : state.resultArchiveName,
      logArchiveName:
          slot == UploadArchiveSlot.log ? fileName : state.logArchiveName,
      selectedAt: DateTime.now(),
      loadStage: ResultsLoadStage.error,
      loadError: 'Could not read the selected zip file: $error',
      previewWarnings: const [],
      message: 'zip read failed: $error',
    );
  }

  Future<void> importArchivePart({
    required UploadArchiveSlot slot,
    required String fileName,
    required Uint8List bytes,
  }) async {
    if (slot == UploadArchiveSlot.result) {
      _pendingResultArchive = _PendingArchive(fileName: fileName, bytes: bytes);
    } else {
      _pendingLogArchive = _PendingArchive(fileName: fileName, bytes: bytes);
    }

    state = state.copyWith(
      isLoading: false,
      message: null,
      resultArchiveName:
          slot == UploadArchiveSlot.result ? fileName : state.resultArchiveName,
      logArchiveName:
          slot == UploadArchiveSlot.log ? fileName : state.logArchiveName,
      selectedAt: DateTime.now(),
      loadStage: ResultsLoadStage.fileLoaded,
      loadError: null,
      resultArchiveLoaded:
          slot == UploadArchiveSlot.result ? true : state.resultArchiveLoaded,
      logArchiveLoaded:
          slot == UploadArchiveSlot.log ? true : state.logArchiveLoaded,
      previewWarnings: const [],
    );

    if (_pendingResultArchive == null || _pendingLogArchive == null) {
      state = state.copyWith(
        message: _pendingResultArchive == null
            ? 'Upload the result zip to continue.'
            : 'Upload the log zip to continue.',
      );
      return;
    }

    try {
      state = state.copyWith(
        isLoading: true,
        loadStage: ResultsLoadStage.parsing,
      );
      final bundle =
          await _ref.read(archiveImportServiceProvider).importSplitZipBytes(
                resultFileName: _pendingResultArchive!.fileName,
                resultBytes: _pendingResultArchive!.bytes,
                logFileName: _pendingLogArchive!.fileName,
                logBytes: _pendingLogArchive!.bytes,
              );
      final historyLabel =
          '${_pendingResultArchive!.fileName} + ${_pendingLogArchive!.fileName}';
      final history =
          await _ref.read(uploadHistoryStoreProvider).add(historyLabel);
      state = state.copyWith(
        previewBundle: bundle,
        baselineBundle: bundle,
        redmineMarkdown:
            _ref.read(redmineServiceProvider).buildMarkdown(bundle),
        isLoading: false,
        usingDemoData: false,
        initialized: true,
        loadStage: ResultsLoadStage.ready,
        loadError: null,
        previewWarnings: bundle.previewWarnings,
        history: history,
        message: 'Preview is ready from the uploaded zip files.',
      );
    } catch (error) {
      state = state.copyWith(
        isLoading: false,
        loadStage: ResultsLoadStage.error,
        loadError: 'The uploaded zip files could not be parsed: $error',
        previewWarnings: const [],
        message: 'Zip parsing failed: $error',
      );
    }
  }

  Future<void> uploadPreview() async {
    final bundle = state.previewBundle;
    if (!state.hasUploadablePreview) {
      state = state.copyWith(
        message: state.loadError == null
            ? 'No parsed preview is ready for upload.'
            : 'The current preview has a parsing error.',
      );
      return;
    }
    if (bundle == null) {
      state = state.copyWith(message: 'There is no preview to upload.');
      return;
    }

    state = state.copyWith(isUploading: true, message: null);
    try {
      switch (state.uploadTarget) {
        case UploadTarget.firestore:
          await _ref.read(firestoreRepositoryProvider).syncImportBundle(bundle);
          state = state.copyWith(
            isUploading: false,
            message: 'Firestore upload completed.',
          );
          break;
        case UploadTarget.redmine:
          final settings = _ref.read(appSettingsControllerProvider).settings;
          await _ref.read(redmineServiceProvider).createIssue(
                settings: settings,
                bundle: bundle,
              );
          state = state.copyWith(
            isUploading: false,
            message: 'Redmine upload completed.',
          );
          break;
      }
    } catch (error) {
      state = state.copyWith(
        isUploading: false,
        message: '${state.uploadTarget.label} upload failed: $error',
      );
    }
  }

  void attachBundle(
    ImportBundle bundle, {
    String? message,
    bool usingDemoData = false,
  }) {
    state = state.copyWith(
      previewBundle: bundle,
      baselineBundle: bundle,
      redmineMarkdown: _ref.read(redmineServiceProvider).buildMarkdown(bundle),
      usingDemoData: usingDemoData,
      message: message,
      initialized: true,
      loadStage: ResultsLoadStage.ready,
      loadError: null,
      previewWarnings: bundle.previewWarnings,
    );
  }

  void toggleFailedTestExcluded(String id, bool excluded) {
    final bundle = state.previewBundle;
    if (bundle == null) {
      return;
    }
    final updatedFailures = bundle.failedTests
        .map(
          (item) => item.id == id ? item.copyWith(excluded: excluded) : item,
        )
        .toList(growable: false);
    _applyUpdatedFailures(updatedFailures);
  }

  void updateFailedTestMemo(String id, String memo) {
    final bundle = state.previewBundle;
    if (bundle == null) {
      return;
    }
    final updatedFailures = bundle.failedTests
        .map(
          (item) => item.id == id ? item.copyWith(manualMemo: memo) : item,
        )
        .toList(growable: false);
    _applyUpdatedFailures(updatedFailures);
  }

  void resetFailureOverrides() {
    final baseline = state.baselineBundle;
    if (baseline == null) {
      return;
    }
    final resetFailures = baseline.failedTests
        .map((item) => item.copyWith(excluded: false, manualMemo: ''))
        .toList(growable: false);
    _applyUpdatedFailures(resetFailures);
  }

  void _applyUpdatedFailures(List<FailedTestRecord> failedTests) {
    final bundle = state.previewBundle;
    if (bundle == null) {
      return;
    }
    final excludedCount = failedTests.where((item) => item.excluded).length;
    final updatedBundle = bundle.copyWith(
      failedTests: failedTests,
      metric: bundle.metric.copyWith(excludedFailureCount: excludedCount),
    );
    state = state.copyWith(
      previewBundle: updatedBundle,
      redmineMarkdown:
          _ref.read(redmineServiceProvider).buildMarkdown(updatedBundle),
    );
  }
}

class RunController extends StateNotifier<RunSessionState> {
  RunController(this._ref)
      : super(
          RunSessionState.initial(
            AppDefaults.initialSettings().toolConfigFor(ToolType.cts),
          ),
        );

  final Ref _ref;
  StreamSubscription<String>? _logSubscription;
  StreamSubscription<ConsoleHealth>? _consoleHealthSubscription;
  final StringBuffer _logBuffer = StringBuffer();

  void syncToolConfig() {
    final config = _ref
        .read(appSettingsControllerProvider)
        .settings
        .toolConfigFor(state.selectedTool);
    state = RunSessionState.initial(config).copyWith(
      latestLogs: state.latestLogs,
      availableDevices: state.availableDevices,
      selectedDeviceSerials: state.selectedDeviceSerials
          .where(
            (serial) => state.availableDevices.any(
              (device) => device.serial == serial && device.isReady,
            ),
          )
          .toList(growable: false),
      consoleHealth: state.consoleHealth,
      message: state.message,
    );
  }

  Future<void> selectTool(ToolType toolType) async {
    final config = _ref
        .read(appSettingsControllerProvider)
        .settings
        .toolConfigFor(toolType);
    state = RunSessionState.initial(config);
    await refreshDevices();
  }

  void toggleDeviceSelection(String serial, bool selected) {
    final current = [...state.selectedDeviceSerials];
    if (selected) {
      if (!current.contains(serial)) {
        current.add(serial);
      }
    } else {
      current.remove(serial);
    }
    state = state.copyWith(
      selectedDeviceSerials: current,
      message: null,
    );
  }

  Future<void> refreshDevices() async {
    final adbService = _ref.read(adbServiceProvider);
    state = state.copyWith(
      isRefreshingDevices: true,
      message: null,
    );

    final snapshot = await adbService.inspect();
    final selected = state.selectedDeviceSerials
        .where(
          (serial) => snapshot.devices.any(
            (device) => device.serial == serial && device.isReady,
          ),
        )
        .toList(growable: false);
    state = state.copyWith(
      availableDevices: snapshot.devices,
      selectedDeviceSerials: selected,
      isRefreshingDevices: false,
      message: snapshot.message,
    );
  }

  void updateAutoUpload(bool value) {
    state = state.copyWith(autoUploadAfterRun: value, message: null);
  }

  Future<void> startRun() async {
    final capabilities = _ref.read(runtimeCapabilitiesProvider);
    if (!capabilities.canRunTests) {
      state = state.copyWith(
        message: 'Test execution is not supported on this platform.',
      );
      return;
    }

    final executionService = _ref.read(xtsExecutionServiceProvider);
    if (!executionService.isSupported) {
      state = state.copyWith(message: 'Execution service is not available.');
      return;
    }
    if (state.selectedDeviceSerials.isEmpty) {
      state = state.copyWith(
        message: 'Select at least one ready ADB device before running.',
      );
      return;
    }
    if (state.generatedCommand.isEmpty) {
      state = state.copyWith(
        message: 'The generated run command is empty.',
      );
      return;
    }

    final savedConfig = _ref
        .read(appSettingsControllerProvider)
        .settings
        .toolConfigFor(state.selectedTool);
    final runtimeConfig = savedConfig.copyWith(
      defaultCommand: state.command,
      autoUploadAfterRun: state.autoUploadAfterRun,
    );

    await _logSubscription?.cancel();
    await _consoleHealthSubscription?.cancel();
    _logBuffer.clear();
    state = state.copyWith(
      isRunning: true,
      isUploading: false,
      stage: RunStage.starting,
      latestLogs: const [],
      consoleHealth: const ConsoleHealth(
        status: ConsoleHealthStatus.checking,
        message: 'Waiting for tradefed console prompt.',
      ),
      startedAt: DateTime.now(),
      finishedAt: null,
      detectedResultsDir: runtimeConfig.resultsDir,
      detectedLogsDir: runtimeConfig.logsDir,
      message: 'Starting test execution.',
      exitCode: null,
    );

    _logSubscription = executionService.logLines.listen((line) {
      _logBuffer.writeln(line);
      final nextLogs = [...state.latestLogs, line];
      if (nextLogs.length > 160) {
        nextLogs.removeAt(0);
      }
      state = state.copyWith(
        latestLogs: nextLogs,
        stage: executionService.deriveStage(_logBuffer.toString()),
      );
    });
    _consoleHealthSubscription =
        executionService.consoleHealthUpdates.listen((health) {
      state = state.copyWith(consoleHealth: health);
    });

    try {
      await executionService.startRun(
        config: runtimeConfig,
        request: RunRequest(
          toolType: state.selectedTool,
          command: state.generatedCommand,
          deviceSerials: state.selectedDeviceSerials,
          shardCount: state.shardCount,
        ),
      );
      unawaited(
        executionService.waitForExit().then((exitCode) async {
          state = state.copyWith(exitCode: exitCode);
          await finalizeRun();
        }),
      );
    } catch (error) {
      state = state.copyWith(
        isRunning: false,
        stage: RunStage.error,
        consoleHealth: const ConsoleHealth(
          status: ConsoleHealthStatus.failed,
          message: 'Console start failed.',
        ),
        message: 'Run failed: $error',
      );
    }
  }

  Future<void> stopRun() async {
    final exitCode = await _ref.read(xtsExecutionServiceProvider).stopRun();
    state = state.copyWith(
      isRunning: false,
      stage: RunStage.finished,
      finishedAt: DateTime.now(),
      exitCode: exitCode,
      message: 'Test run was stopped.',
    );
    await _afterRunFinished();
  }

  Future<void> finalizeRun() async {
    if (!state.isRunning && state.finishedAt != null) {
      return;
    }
    state = state.copyWith(
      isRunning: false,
      finishedAt: DateTime.now(),
      stage: state.stage == RunStage.error ? RunStage.error : RunStage.finished,
    );
    await _afterRunFinished();
  }

  Future<void> _afterRunFinished() async {
    if (!state.autoUploadAfterRun) {
      state = state.copyWith(
        message: 'Run finished. Upload the parsed result manually if needed.',
      );
      return;
    }

    final config = _ref
        .read(appSettingsControllerProvider)
        .settings
        .toolConfigFor(state.selectedTool);
    state = state.copyWith(
      isUploading: true,
      message: 'Importing run result from configured paths.',
    );
    try {
      final bundle = await _ref.read(importServiceProvider).importFromPaths(
            resultsDir: config.resultsDir,
            logsDir: config.logsDir,
          );
      await _ref.read(firestoreRepositoryProvider).syncImportBundle(bundle);
      _ref.read(resultsControllerProvider.notifier).attachBundle(
            bundle,
            message: 'Run result imported automatically.',
          );
      state = state.copyWith(
        isUploading: false,
        message: 'Run result upload completed.',
      );
    } catch (error) {
      state = state.copyWith(
        isUploading: false,
        message: 'Automatic upload failed: $error',
      );
    }
  }

  @override
  void dispose() {
    _logSubscription?.cancel();
    _consoleHealthSubscription?.cancel();
    super.dispose();
  }
}

final runtimeCapabilitiesProvider = Provider((ref) {
  return RuntimeCapabilities.detect();
});

final appSettingsStoreProvider = Provider((ref) => AppSettingsStore());
final uploadHistoryStoreProvider = Provider((ref) => UploadHistoryStore());
final releaseWatchTargetsStoreProvider =
    Provider((ref) => ReleaseWatchTargetsStore());
final adbServiceProvider = Provider((ref) => createAdbService());
final localFileGatewayProvider = Provider((ref) => createLocalFileGateway());
final authHeaderProviderProvider =
    Provider((ref) => createAuthHeaderProvider());
final xtsResultParserProvider = Provider((ref) => XtsResultParser());
final xtsLiveLogParserProvider = Provider((ref) => XtsLiveLogParser());
final xtsTfOutputParserProvider = Provider((ref) => XtsTfOutputParser());
final xtsExecutionServiceProvider =
    Provider((ref) => createXtsExecutionService());
final releaseUpdateServiceProvider = Provider((ref) => ReleaseUpdateService());
final redmineServiceProvider = Provider((ref) => RedmineService());
final releaseWatcherArtifactServiceProvider = Provider(
  (ref) => ReleaseWatcherArtifactService(
    localFileGateway: ref.read(localFileGatewayProvider),
  ),
);
final archiveImportServiceProvider = Provider(
  (ref) => ArchiveImportService(
    resultParser: ref.read(xtsResultParserProvider),
    liveLogParser: ref.read(xtsLiveLogParserProvider),
    tfOutputParser: ref.read(xtsTfOutputParserProvider),
  ),
);
final environmentCheckServiceProvider = Provider(
  (ref) => EnvironmentCheckService(
    authHeaderProvider: ref.read(authHeaderProviderProvider),
    adbService: ref.read(adbServiceProvider),
  ),
);
final importServiceProvider = Provider(
  (ref) => ImportService(
    localFileGateway: ref.read(localFileGatewayProvider),
    resultParser: ref.read(xtsResultParserProvider),
    liveLogParser: ref.read(xtsLiveLogParserProvider),
    tfOutputParser: ref.read(xtsTfOutputParserProvider),
  ),
);

final appSettingsControllerProvider =
    StateNotifierProvider<AppSettingsController, AppSettingsState>((ref) {
  final controller = AppSettingsController(ref.read(appSettingsStoreProvider));
  controller.load();
  return controller;
});

final firestoreRepositoryProvider = Provider((ref) {
  final settings = ref.watch(appSettingsControllerProvider).settings;
  return FirestoreRepository(
    client: FirestoreRestClient(
      settings: settings,
      authHeaderProvider: ref.read(authHeaderProviderProvider),
    ),
  );
});

final resultsControllerProvider =
    StateNotifierProvider<ResultsController, ResultsState>((ref) {
  final controller = ResultsController(ref);
  unawaited(controller.initialize());
  return controller;
});

final releaseWatchTargetsControllerProvider = StateNotifierProvider<
    ReleaseWatchTargetsController, ReleaseWatchTargetsState>((ref) {
  final controller = ReleaseWatchTargetsController(
    ref.read(releaseWatchTargetsStoreProvider),
  );
  unawaited(controller.load());
  return controller;
});

final runControllerProvider =
    StateNotifierProvider<RunController, RunSessionState>((ref) {
  final controller = RunController(ref);
  return controller;
});

final testMetricsProvider = FutureProvider((ref) async {
  return ref.watch(firestoreRepositoryProvider).fetchMetrics();
});

final failedTestsProvider = FutureProvider((ref) async {
  return ref.watch(firestoreRepositoryProvider).fetchFailedTests();
});

final testCasesProvider = FutureProvider((ref) async {
  return ref.watch(firestoreRepositoryProvider).fetchTestCases();
});

final releaseStatusProvider = FutureProvider<ReleaseStatus>((ref) async {
  return ref.watch(releaseUpdateServiceProvider).fetchLatestRelease();
});

final releaseWatcherSnapshotProvider =
    FutureProvider<ReleaseWatchSnapshot>((ref) async {
  return ref.watch(releaseWatcherArtifactServiceProvider).loadLatestSnapshot();
});

final environmentStatusProvider =
    FutureProvider<EnvironmentCheckStatus>((ref) async {
  final settings = ref.watch(appSettingsControllerProvider).settings;
  return ref.watch(environmentCheckServiceProvider).check(settings);
});
