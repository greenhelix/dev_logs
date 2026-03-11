import 'package:flutter/material.dart';

import 'core/theme/app_theme.dart';
import 'features/shell/presentation/app_shell.dart';

class GahApp extends StatelessWidget {
  const GahApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '구글 인증 도우미',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      home: const AppShell(),
    );
  }
}
