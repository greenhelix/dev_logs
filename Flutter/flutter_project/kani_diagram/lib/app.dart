import 'package:flutter/material.dart';

import 'core/theme/app_theme.dart';
import 'features/diagram/presentation/diagram_workspace_page.dart';

class KaniDiagramApp extends StatelessWidget {
  const KaniDiagramApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Kani Diagram',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: const DiagramWorkspacePage(),
    );
  }
}
