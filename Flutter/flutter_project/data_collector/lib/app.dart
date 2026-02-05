import 'package:data_accumulator_app/features/person/presentation/person_list_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'data/local/app_database.dart';

import 'features/person/presentation/person_detail_screen.dart';
import 'features/home/presentation/home_screen.dart';

// ─── Router Config ───────────────────────────
// 앱의 모든 화면 경로를 여기서 관리합니다.
final _router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const HomeScreen(),
      routes: [
        // 추후 여기에 각 기능별 라우트를 추가할 예정입니다.
        // 예: path: 'person', builder: ...
        GoRoute(
            path: 'person',
            builder: (context, state) => const PersonListScreen(),
            routes: [
              GoRoute(
                  path: 'detail',
                  builder: (context, state) {
                    final person = state.extra as Person;
                    return PersonDetailScreen(person: person);
                  })
            ]),
      ],
    ),
  ],
);

// ─── App Widget ──────────────────────────────
class DataCollectorApp extends ConsumerWidget {
  const DataCollectorApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      title: 'Data Collector',

      // 디자인 테마 설정 (Material 3 적용)
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        appBarTheme: const AppBarTheme(
          centerTitle: true,
          elevation: 0,
        ),
      ),

      // GoRouter 연결
      routerConfig: _router,

      // 디버그 배너 숨기기
      debugShowCheckedModeBanner: false,
    );
  }
}
