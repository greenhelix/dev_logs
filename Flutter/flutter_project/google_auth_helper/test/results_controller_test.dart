import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/core/config/app_defaults.dart';
import 'package:google_auth_helper/models/app_settings.dart';
import 'package:google_auth_helper/models/tool_config.dart';
import 'package:google_auth_helper/providers/app_providers.dart';
import 'package:google_auth_helper/services/app_settings_store.dart';
import 'package:google_auth_helper/services/upload_history_store.dart';
class _FakeAppSettingsStore extends AppSettingsStore {
  _FakeAppSettingsStore(this.settings);

  final AppSettings settings;

  @override
  Future<AppSettings> load() async => settings;
}

class _FakeUploadHistoryStore extends UploadHistoryStore {
  @override
  Future<List<String>> load() async => const [];
}

class _FakeAppSettingsController extends AppSettingsController {
  _FakeAppSettingsController(AppSettings settings)
      : super(_FakeAppSettingsStore(settings)) {
    state = AppSettingsState(settings: settings, isLoading: false);
  }

  @override
  Future<void> load() async {}
}

void main() {
  test('ResultsController does not fall back to synthetic demo data',
      () async {
    final emptySettings = AppDefaults.initialSettings().copyWith(
      toolConfigs: ToolType.values
          .map(
            (toolType) => ToolConfig(
              toolType: toolType,
              toolRoot: '',
              resultsDir: '',
              logsDir: '',
              defaultCommand:
                  AppDefaults.defaultToolConfig(toolType).defaultCommand,
              autoUploadAfterRun: true,
            ),
          )
          .toList(growable: false),
    );
    final container = ProviderContainer(
      overrides: [
        appSettingsStoreProvider.overrideWith((ref) {
          return _FakeAppSettingsStore(emptySettings);
        }),
        uploadHistoryStoreProvider.overrideWith((ref) {
          return _FakeUploadHistoryStore();
        }),
        appSettingsControllerProvider.overrideWith((ref) {
          return _FakeAppSettingsController(emptySettings);
        }),
      ],
    );
    addTearDown(container.dispose);

    final controller = container.read(resultsControllerProvider.notifier);
    await controller.initialize();
    final state = container.read(resultsControllerProvider);

    expect(state.usingDemoData, isFalse);
    expect(state.previewBundle, isNull);
    expect(state.loadError, isNull);
    expect(state.loadStage, ResultsLoadStage.idle);
    expect(
      state.message,
      'Upload result and log zip files to build a preview.',
    );
  });
}
