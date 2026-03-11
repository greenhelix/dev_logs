import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_auth_helper/models/app_log_entry.dart';
import 'package:google_auth_helper/providers/app_providers.dart';

void main() {
  test('AppLogController appends logs in chronological order', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);

    final controller = container.read(appLogControllerProvider.notifier);
    controller.add(
      area: AppLogArea.environment,
      message: '첫 번째 로그',
    );
    controller.add(
      area: AppLogArea.environment,
      message: '두 번째 로그',
    );

    final logs = container.read(appLogControllerProvider);
    expect(logs, hasLength(2));
    expect(logs.first.message, '첫 번째 로그');
    expect(logs.last.message, '두 번째 로그');
  });

  test('AppLogController clears only the requested area', () {
    final container = ProviderContainer();
    addTearDown(container.dispose);

    final controller = container.read(appLogControllerProvider.notifier);
    controller.add(
      area: AppLogArea.environment,
      message: '환경 로그',
    );
    controller.add(
      area: AppLogArea.updates,
      message: '업데이트 로그',
    );

    controller.clearForArea(AppLogArea.environment);

    final logs = container.read(appLogControllerProvider);
    expect(logs, hasLength(1));
    expect(logs.single.area, AppLogArea.updates);
  });
}
