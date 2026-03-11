import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/app.dart';
import 'package:google_auth_helper/features/shell/presentation/app_shell.dart';

void main() {
  testWidgets('app shell shows the revised menu icons', (tester) async {
    tester.view.physicalSize = const Size(1600, 1200);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(const ProviderScope(child: GahApp()));
    await tester.pump();

    expect(find.byType(AppShell), findsOneWidget);
    expect(find.byIcon(Icons.dashboard_rounded), findsWidgets);
    expect(find.byIcon(Icons.update_rounded), findsWidgets);
    expect(find.byIcon(Icons.upload_file_rounded), findsWidgets);
    expect(find.byIcon(Icons.health_and_safety_rounded), findsWidgets);
    expect(find.byIcon(Icons.terminal_rounded), findsWidgets);
    expect(find.byIcon(Icons.tune_rounded), findsWidgets);
  });
}
