import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/app.dart';
import 'package:google_auth_helper/core/runtime/runtime_capabilities.dart';
import 'package:google_auth_helper/features/shell/presentation/app_shell.dart';
import 'package:google_auth_helper/models/release_status.dart';
import 'package:google_auth_helper/models/release_watch_snapshot.dart';
import 'package:google_auth_helper/providers/app_providers.dart';

void main() {
  testWidgets('app shell shows the revised menu icons', (tester) async {
    await _pumpShell(
      tester,
      width: 1600,
      capabilities: const RuntimeCapabilities(
        profile: RuntimePlatformProfile.windowsDesktop,
        canReadRemote: true,
        canUploadResults: true,
        canRunTests: false,
        canEditSettings: true,
      ),
    );

    expect(find.byType(AppShell), findsOneWidget);
    expect(find.byIcon(Icons.dashboard_rounded), findsWidgets);
    expect(find.byIcon(Icons.update_rounded), findsWidgets);
    expect(find.byIcon(Icons.upload_file_rounded), findsWidgets);
    expect(find.byIcon(Icons.health_and_safety_rounded), findsWidgets);
    expect(find.byIcon(Icons.terminal_rounded), findsWidgets);
    expect(find.byIcon(Icons.tune_rounded), findsWidgets);
  });

  testWidgets('windows mode disables auto test menu', (tester) async {
    await _pumpShell(
      tester,
      width: 1600,
      capabilities: const RuntimeCapabilities(
        profile: RuntimePlatformProfile.windowsDesktop,
        canReadRemote: true,
        canUploadResults: true,
        canRunTests: false,
        canEditSettings: true,
      ),
    );

    expect(find.text('우분투 전용'), findsWidgets);
    final button = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, '자동 테스트'),
    );
    expect(button.onPressed, isNull);
  });

  testWidgets('header description is hidden on narrow layouts', (tester) async {
    await _pumpShell(
      tester,
      width: 700,
      capabilities: const RuntimeCapabilities(
        profile: RuntimePlatformProfile.windowsDesktop,
        canReadRemote: true,
        canUploadResults: true,
        canRunTests: false,
        canEditSettings: true,
      ),
    );

    expect(find.text('대시보드'), findsWidgets);
    expect(find.text('원격 집계와 현재 미리보기를 확인합니다.'), findsOneWidget);
  });
}

Future<void> _pumpShell(
  WidgetTester tester, {
  required double width,
  required RuntimeCapabilities capabilities,
}) async {
  tester.view.physicalSize = Size(width, 1200);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(() {
    tester.view.resetPhysicalSize();
    tester.view.resetDevicePixelRatio();
  });

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        runtimeCapabilitiesProvider.overrideWith((ref) => capabilities),
        releaseStatusProvider.overrideWith(
          (ref) async => const ReleaseStatus(
            currentVersion: '0.1.2+1',
            latestVersion: '0.1.2+1',
            releaseUrl: 'https://github.com/greenhelix/GAH-Release-Repo',
          ),
        ),
        releaseWatcherSnapshotProvider.overrideWith(
          (ref) async => ReleaseWatchSnapshot(
            sourceLabel: 'test',
            version: 'v0.1.2',
            releaseNotesHash: 'hash',
            lastCheckedAt: DateTime.parse('2026-03-11T00:00:00Z'),
            lastUploadedAt: null,
            uploadStatus: 'idle',
            changes: const [],
          ),
        ),
      ],
      child: const GahApp(),
    ),
  );
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 250));
}
