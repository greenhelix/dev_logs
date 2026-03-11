import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/legacy.dart';

import '../core/config/app_defaults.dart';
import '../core/runtime/runtime_capabilities.dart';
import '../data/firestore_repository.dart';
import '../data/firestore_rest_client.dart';
import '../models/app_log_entry.dart';
import '../models/app_settings.dart';
import '../models/console_health.dart';
import '../models/environment_check_status.dart';
import '../models/failed_test_record.dart';
import '../models/import_bundle.dart';
import '../models/import_source.dart';
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
import '../services/release_installer_service.dart';
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
    this.uploadPreviewText = '',
    this.isLoading = false,
    this.isUploading = false,
    this.usingDemoData = false,
    this.initialized = false,
    this.resultArchiveName,
    this.logArchiveName,
    this.resultSourceKind,
    this.logSourceKind,
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
  final String uploadPreviewText;
  final bool isLoading;
  final bool isUploading;
  final bool usingDemoData;
  final bool initialized;
  final String? resultArchiveName;
  final String? logArchiveName;
  final ImportSourceKind? resultSourceKind;
  final ImportSourceKind? logSourceKind;
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
    Object? uploadPreviewText = _unset,
    bool? isLoading,
    bool? isUploading,
    bool? usingDemoData,
    bool? initialized,
    Object? resultArchiveName = _unset,
    Object? logArchiveName = _unset,
    Object? resultSourceKind = _unset,
    Object? logSourceKind = _unset,
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
      uploadPreviewText: identical(uploadPreviewText, _unset)
          ? this.uploadPreviewText
          : uploadPreviewText as String,
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
      resultSourceKind: identical(resultSourceKind, _unset)
          ? this.resultSourceKind
          : resultSourceKind as ImportSourceKind?,
      logSourceKind: identical(logSourceKind, _unset)
          ? this.logSourceKind
          : logSourceKind as ImportSourceKind?,
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

class AppLogController extends StateNotifier<List<AppLogEntry>> {
  AppLogController() : super(const []);

  void add({
    required AppLogArea area,
    required String message,
    AppLogLevel level = AppLogLevel.info,
    String? detail,
  }) {
    final next = [
      ...state,
      AppLogEntry(
        timestamp: DateTime.now(),
        area: area,
        level: level,
        message: message,
        detail: detail,
      ),
    ];
    final overflow = next.length - 300;
    state = overflow > 0
        ? next.sublist(overflow).toList(growable: false)
        : next;
  }

  void clearForArea(AppLogArea area) {
    state = state
        .where((entry) => entry.area != area)
        .toList(growable: false);
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
  ImportSource? _pendingResultSource;
  ImportSource? _pendingLogSource;

  Future<void> initialize() async {
    if (state.initialized) {
      return;
    }
    final history = await _ref.read(uploadHistoryStoreProvider).load();
    final capabilities = _ref.read(runtimeCapabilitiesProvider);
    state = state.copyWith(
      history: history,
      initialized: true,
      loadStage: ResultsLoadStage.idle,
      message: capabilities.profile == RuntimePlatformProfile.webHosting
          ? '웹에서는 결과 업로드를 지원하지 않습니다.'
          : '결과와 로그 원본을 올리면 미리보기를 생성합니다.',
    );
  }

  Future<void> selectTool(ToolType toolType) async {
    _pendingResultSource = null;
    _pendingLogSource = null;
    state = state.copyWith(
      selectedTool: toolType,
      previewBundle: null,
      baselineBundle: null,
      uploadPreviewText: '',
      resultArchiveName: null,
      logArchiveName: null,
      resultSourceKind: null,
      logSourceKind: null,
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
      uploadPreviewText: _buildUploadPreview(target, state.previewBundle),
      message: '${target.label} 업로드 대상으로 전환했습니다.',
    );
  }

  void registerSelectedArchive(UploadArchiveSlot slot, String fileName) {
    state = state.copyWith(
      resultArchiveName:
          slot == UploadArchiveSlot.result ? fileName : state.resultArchiveName,
      logArchiveName:
          slot == UploadArchiveSlot.log ? fileName : state.logArchiveName,
      resultSourceKind:
          slot == UploadArchiveSlot.result ? ImportSourceKind.archive : state.resultSourceKind,
      logSourceKind:
          slot == UploadArchiveSlot.log ? ImportSourceKind.archive : state.logSourceKind,
      selectedAt: DateTime.now(),
      loadStage: ResultsLoadStage.selectingFile,
      loadError: null,
      previewWarnings: const [],
      message: '$fileName 파일을 선택했습니다.',
    );
  }

  void registerSelectedDirectory(
    UploadArchiveSlot slot,
    String label,
  ) {
    state = state.copyWith(
      resultArchiveName:
          slot == UploadArchiveSlot.result ? label : state.resultArchiveName,
      logArchiveName:
          slot == UploadArchiveSlot.log ? label : state.logArchiveName,
      resultSourceKind: slot == UploadArchiveSlot.result
          ? ImportSourceKind.directory
          : state.resultSourceKind,
      logSourceKind: slot == UploadArchiveSlot.log
          ? ImportSourceKind.directory
          : state.logSourceKind,
      selectedAt: DateTime.now(),
      loadStage: ResultsLoadStage.selectingFile,
      loadError: null,
      previewWarnings: const [],
      message: '$label 폴더를 선택했습니다.',
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
      loadError: '선택한 압축파일을 읽지 못했습니다: $error',
      previewWarnings: const [],
      message: '압축파일 읽기에 실패했습니다.',
    );
    _ref.read(appLogControllerProvider.notifier).add(
          area: AppLogArea.results,
          message: '압축파일 읽기 실패',
          level: AppLogLevel.error,
          detail: '$error',
        );
  }

  Future<void> importArchivePart({
    required UploadArchiveSlot slot,
    required String fileName,
    required Uint8List bytes,
  }) async {
    final source = ArchiveImportSource(fileName: fileName, bytes: bytes);
    await _registerSource(slot: slot, source: source, label: fileName);
  }

  Future<void> importDirectoryPart({
    required UploadArchiveSlot slot,
    required String directoryPath,
    required String label,
  }) async {
    final source = DirectoryImportSource(
      directoryPath: directoryPath,
      label: label,
    );
    await _registerSource(slot: slot, source: source, label: label);
  }

  Future<void> _registerSource({
    required UploadArchiveSlot slot,
    required ImportSource source,
    required String label,
  }) async {
    if (slot == UploadArchiveSlot.result) {
      _pendingResultSource = source;
    } else {
      _pendingLogSource = source;
    }
    state = state.copyWith(
      isLoading: false,
      message: null,
      resultArchiveName:
          slot == UploadArchiveSlot.result ? label : state.resultArchiveName,
      logArchiveName:
          slot == UploadArchiveSlot.log ? label : state.logArchiveName,
      resultSourceKind:
          slot == UploadArchiveSlot.result ? source.kind : state.resultSourceKind,
      logSourceKind:
          slot == UploadArchiveSlot.log ? source.kind : state.logSourceKind,
      selectedAt: DateTime.now(),
      loadStage: ResultsLoadStage.fileLoaded,
      loadError: null,
      resultArchiveLoaded:
          slot == UploadArchiveSlot.result ? true : state.resultArchiveLoaded,
      logArchiveLoaded:
          slot == UploadArchiveSlot.log ? true : state.logArchiveLoaded,
      previewWarnings: const [],
    );
    _ref.read(appLogControllerProvider.notifier).add(
          area: AppLogArea.results,
          message: '${slot == UploadArchiveSlot.result ? "결과" : "로그"} 원본 등록',
          detail: '$label (${source.kind.name})',
        );

    if (_pendingResultSource == null || _pendingLogSource == null) {
      state = state.copyWith(
        message: _pendingResultSource == null
            ? '결과 원본을 추가로 선택해 주세요.'
            : '로그 원본을 추가로 선택해 주세요.',
      );
      return;
    }

    try {
      state = state.copyWith(
        isLoading: true,
        loadStage: ResultsLoadStage.parsing,
      );
      final bundle = await _ref.read(archiveImportServiceProvider).importFromSources(
            resultSource: _pendingResultSource!,
            logSource: _pendingLogSource!,
          );
      final historyLabel =
          '${state.resultArchiveName ?? "결과"} + ${state.logArchiveName ?? "로그"}';
      final history = await _ref.read(uploadHistoryStoreProvider).add(historyLabel);
      state = state.copyWith(
        previewBundle: bundle,
        baselineBundle: bundle,
        uploadPreviewText: _buildUploadPreview(state.uploadTarget, bundle),
        isLoading: false,
        usingDemoData: false,
        initialized: true,
        loadStage: ResultsLoadStage.ready,
        loadError: null,
        previewWarnings: bundle.previewWarnings,
        history: history,
        message: '미리보기를 준비했습니다.',
      );
      _ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.results,
            message: '미리보기 생성 완료',
            detail: historyLabel,
          );
    } catch (error) {
      state = state.copyWith(
        isLoading: false,
        loadStage: ResultsLoadStage.error,
        loadError: '업로드 원본을 파싱하지 못했습니다: $error',
        previewWarnings: const [],
        message: '파싱에 실패했습니다.',
      );
      _ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.results,
            message: '미리보기 생성 실패',
            level: AppLogLevel.error,
            detail: '$error',
          );
    }
  }

  void resetUploadState() {
    _pendingResultSource = null;
    _pendingLogSource = null;
    state = state.copyWith(
      previewBundle: null,
      baselineBundle: null,
      uploadPreviewText: '',
      resultArchiveName: null,
      logArchiveName: null,
      resultSourceKind: null,
      logSourceKind: null,
      resultArchiveLoaded: false,
      logArchiveLoaded: false,
      loadStage: ResultsLoadStage.idle,
      loadError: null,
      previewWarnings: const [],
      message: '업로드 상태를 초기화했습니다.',
    );
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
            message: '파이어스토어 업로드를 완료했습니다.',
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
            message: '레드마인 업로드를 완료했습니다.',
          );
          break;
      }
      _ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.results,
            message: '${state.uploadTarget.label} 업로드 완료',
          );
    } catch (error) {
      state = state.copyWith(
        isUploading: false,
        message: '${state.uploadTarget.label} 업로드에 실패했습니다.',
      );
      _ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.results,
            message: '${state.uploadTarget.label} 업로드 실패',
            level: AppLogLevel.error,
            detail: '$error',
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
      uploadPreviewText: _buildUploadPreview(state.uploadTarget, bundle),
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
      uploadPreviewText: _buildUploadPreview(state.uploadTarget, updatedBundle),
    );
  }

  String _buildUploadPreview(UploadTarget target, ImportBundle? bundle) {
    if (bundle == null) {
      return '';
    }
    switch (target) {
      case UploadTarget.redmine:
        return _ref.read(redmineServiceProvider).buildMarkdown(bundle);
      case UploadTarget.firestore:
        return const JsonEncoder.withIndent('  ').convert(_normalizeForJson({
          'metric': bundle.metric
              .copyWith(
                excludedFailureCount: bundle.excludedFailedTests.length,
              )
              .toMap(),
          'testCases':
              bundle.testCases.map((item) => item.toMap()).toList(growable: false),
          'failedTests': bundle.activeFailedTests
              .map((item) => item.toMap())
              .toList(growable: false),
          'warnings': bundle.previewWarnings,
        }));
    }
  }

  Object? _normalizeForJson(Object? value) {
    if (value is DateTime) {
      return value.toUtc().toIso8601String();
    }
    if (value is Map<String, dynamic>) {
      return value.map((key, item) => MapEntry(key, _normalizeForJson(item)));
    }
    if (value is List) {
      return value.map(_normalizeForJson).toList(growable: false);
    }
    return value;
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
      isConsoleReady: state.isConsoleReady,
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
    final settings = _ref.read(appSettingsControllerProvider).settings;
    state = state.copyWith(
      isRefreshingDevices: true,
      message: null,
    );

    final snapshot = await adbService.inspect(
      configuredPath: settings.adbExecutablePath,
    );
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
    _ref.read(appLogControllerProvider.notifier).add(
          area: AppLogArea.run,
          message: 'ADB 장치 새로고침',
          detail: snapshot.message,
        );
  }

  void updateAutoUpload(bool value) {
    state = state.copyWith(autoUploadAfterRun: value, message: null);
  }

  Future<void> startConsole() async {
    final capabilities = _ref.read(runtimeCapabilitiesProvider);
    if (!capabilities.canRunTests) {
      state = state.copyWith(
        message: '이 플랫폼에서는 자동 테스트를 실행할 수 없습니다.',
      );
      return;
    }

    final executionService = _ref.read(xtsExecutionServiceProvider);
    if (!executionService.isSupported) {
      state = state.copyWith(message: '실행 서비스를 사용할 수 없습니다.');
      return;
    }
    if (state.selectedDeviceSerials.isEmpty) {
      state = state.copyWith(
        message: '실행 가능한 ADB 장치를 하나 이상 선택해 주세요.',
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
      isRunning: false,
      isConsoleReady: false,
      isUploading: false,
      stage: RunStage.starting,
      latestLogs: const [],
      consoleHealth: const ConsoleHealth(
        status: ConsoleHealthStatus.checking,
        message: 'tradefed 콘솔 프롬프트를 기다리는 중입니다.',
      ),
      startedAt: DateTime.now(),
      finishedAt: null,
      detectedResultsDir: runtimeConfig.resultsDir,
      detectedLogsDir: runtimeConfig.logsDir,
      message: '콘솔을 시작하는 중입니다.',
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
      state = state.copyWith(
        consoleHealth: health,
        isConsoleReady: health.status == ConsoleHealthStatus.ok,
      );
    });

    try {
      await executionService.startConsole(
        config: runtimeConfig,
        request: RunRequest(
          toolType: state.selectedTool,
          command: state.generatedCommand,
          deviceSerials: state.selectedDeviceSerials,
          shardCount: state.shardCount,
        ),
      );
      state = state.copyWith(
        isConsoleReady: true,
        stage: RunStage.starting,
        message: '콘솔이 준비되었습니다. 이제 실행 버튼을 눌러 주세요.',
      );
      _ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.run,
            message: '콘솔 시작 완료',
          );
    } catch (error) {
      state = state.copyWith(
        isRunning: false,
        isConsoleReady: false,
        stage: RunStage.error,
        consoleHealth: const ConsoleHealth(
          status: ConsoleHealthStatus.failed,
          message: '콘솔 시작에 실패했습니다.',
        ),
        message: '콘솔 시작에 실패했습니다.',
      );
      _ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.run,
            message: '콘솔 시작 실패',
            level: AppLogLevel.error,
            detail: '$error',
          );
    }
  }

  Future<void> startRun() async {
    final executionService = _ref.read(xtsExecutionServiceProvider);
    if (!state.isConsoleReady || !executionService.isConsoleRunning) {
      state = state.copyWith(
        message: '먼저 콘솔 시작 버튼으로 콘솔을 준비해 주세요.',
      );
      return;
    }
    if (state.generatedCommand.isEmpty) {
      state = state.copyWith(message: '실행 명령이 비어 있습니다.');
      return;
    }

    state = state.copyWith(
      isRunning: true,
      stage: RunStage.queued,
      message: '실행 명령을 전송하는 중입니다.',
    );
    try {
      final request = RunRequest(
        toolType: state.selectedTool,
        command: state.generatedCommand,
        deviceSerials: state.selectedDeviceSerials,
        shardCount: state.shardCount,
      );
      await executionService.sendRunCommand(request);
      state = state.copyWith(
        stage: RunStage.running,
        message: '테스트 실행을 시작했습니다.',
      );
      _ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.run,
            message: '실행 명령 전송 완료',
            detail: request.command,
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
        message: '실행 명령 전송에 실패했습니다.',
      );
      _ref.read(appLogControllerProvider.notifier).add(
            area: AppLogArea.run,
            message: '실행 명령 전송 실패',
            level: AppLogLevel.error,
            detail: '$error',
          );
    }
  }

  Future<void> stopRun() async {
    final exitCode = await _ref.read(xtsExecutionServiceProvider).stopRun();
    state = state.copyWith(
      isRunning: false,
      isConsoleReady: false,
      stage: RunStage.finished,
      finishedAt: DateTime.now(),
      exitCode: exitCode,
      message: '실행을 중지했습니다.',
    );
    await _afterRunFinished();
  }

  Future<void> finalizeRun() async {
    if (!state.isRunning && state.finishedAt != null) {
      return;
    }
    state = state.copyWith(
      isRunning: false,
      isConsoleReady: false,
      finishedAt: DateTime.now(),
      stage: state.stage == RunStage.error ? RunStage.error : RunStage.finished,
    );
    await _afterRunFinished();
  }

  Future<void> _afterRunFinished() async {
    if (!state.autoUploadAfterRun) {
      state = state.copyWith(
        message: '실행이 끝났습니다. 필요하면 수동으로 업로드해 주세요.',
      );
      return;
    }

    final config = _ref
        .read(appSettingsControllerProvider)
        .settings
        .toolConfigFor(state.selectedTool);
    state = state.copyWith(
      isUploading: true,
      message: '설정된 경로에서 실행 결과를 가져오는 중입니다.',
    );
    try {
      final bundle = await _ref.read(importServiceProvider).importFromPaths(
            resultsDir: config.resultsDir,
            logsDir: config.logsDir,
          );
      await _ref.read(firestoreRepositoryProvider).syncImportBundle(bundle);
      _ref.read(resultsControllerProvider.notifier).attachBundle(
          bundle,
            message: '실행 결과를 자동으로 불러왔습니다.',
          );
      state = state.copyWith(
        isUploading: false,
        message: '실행 결과 업로드를 완료했습니다.',
      );
    } catch (error) {
      state = state.copyWith(
        isUploading: false,
        message: '자동 업로드에 실패했습니다.',
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
final releaseInstallerServiceProvider =
    Provider((ref) => ReleaseInstallerService());
final redmineServiceProvider = Provider((ref) => RedmineService());
final releaseWatcherArtifactServiceProvider = Provider(
  (ref) => ReleaseWatcherArtifactService(
    localFileGateway: ref.read(localFileGatewayProvider),
  ),
);
final archiveImportServiceProvider = Provider(
  (ref) => ArchiveImportService(
    localFileGateway: ref.read(localFileGatewayProvider),
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

final appLogControllerProvider =
    StateNotifierProvider<AppLogController, List<AppLogEntry>>((ref) {
  return AppLogController();
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
  final logger = ref.read(appLogControllerProvider.notifier);
  logger.add(
    area: AppLogArea.updates,
    message: '릴리즈 감시 산출물 조회 시작',
  );
  try {
    final snapshot =
        await ref.watch(releaseWatcherArtifactServiceProvider).loadLatestSnapshot();
    logger.add(
      area: AppLogArea.updates,
      message: '릴리즈 감시 산출물 조회 완료',
      detail:
          '${snapshot.version} | 변경 ${snapshot.changes.length}건 | 상태 ${snapshot.uploadStatus}',
    );
    return snapshot;
  } catch (error) {
    logger.add(
      area: AppLogArea.updates,
      message: '릴리즈 감시 산출물 조회 실패',
      level: AppLogLevel.error,
      detail: '$error',
    );
    rethrow;
  }
});

final environmentStatusProvider =
    FutureProvider<EnvironmentCheckStatus>((ref) async {
  final settings = ref.watch(appSettingsControllerProvider).settings;
  final logger = ref.read(appLogControllerProvider.notifier);
  logger.add(
    area: AppLogArea.environment,
    message: '환경 점검 시작',
    detail: 'Firebase, Redmine, ADB 연결 상태를 확인합니다.',
  );
  try {
    final status = await ref.watch(environmentCheckServiceProvider).check(
      settings,
      onProgress: (progress) {
        if (progress.phase == EnvironmentCheckProgressPhase.started) {
          logger.add(
            area: AppLogArea.environment,
            message: '${progress.label} 점검 시작',
            detail: progress.message,
          );
          return;
        }
        final result = progress.result;
        logger.add(
          area: AppLogArea.environment,
          message: '${progress.label} ${result?.isOk == true ? "확인 완료" : "점검 필요"}',
          level: result == null
              ? AppLogLevel.warning
              : result.isOk
                  ? AppLogLevel.info
                  : AppLogLevel.warning,
          detail: progress.message,
        );
      },
    );
    final allResults = [
      ...status.firebaseResults,
      ...status.localResults,
      ...status.redmineResults,
    ];
    final okCount = allResults.where((item) => item.isOk).length;
    logger.add(
      area: AppLogArea.environment,
      message: '환경 점검 완료',
      level: okCount == allResults.length ? AppLogLevel.info : AppLogLevel.warning,
      detail: '정상 $okCount / 전체 ${allResults.length}',
    );
    return status;
  } catch (error) {
    logger.add(
      area: AppLogArea.environment,
      message: '환경 점검 실패',
      level: AppLogLevel.error,
      detail: '$error',
    );
    rethrow;
  }
});
