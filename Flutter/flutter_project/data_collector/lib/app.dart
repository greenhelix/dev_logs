import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

// 📌 home
import 'features/home/presentation/home_screen.dart';

// 📌 person
import 'features/person/domain/person_model.dart';
import 'features/person/presentation/person_list_screen.dart';
import 'features/person/presentation/person_detail_screen.dart';

// 📌 news
import 'features/news/domain/news_model.dart';
import 'features/news/presentation/news_list_screen.dart';
import 'features/news/presentation/news_detail_screen.dart';

// 📌 maps
import 'features/maps/domain/location_model.dart';
import 'features/maps/presentation/location_list_screen.dart';
import 'features/maps/presentation/map_tracker_screen.dart';

// ─── Router Config ───────────────────────────
// 앱의 모든 화면 경로를 여기서 관리합니다.
final _router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const HomeScreen(),
      routes: [
        // 예: path: 'person', 'news', 'maps': ...
        GoRoute(
            path: 'person',
            builder: (context, state) => const PersonListScreen(),
            routes: [
              GoRoute(
                  path: 'detail',
                  builder: (context, state) {
                    final person = state.extra as PersonModel;
                    // final person = state.extra as Person;
                    return PersonDetailScreen(person: person);
                  })
            ]),
        GoRoute(
            path: 'news',
            builder: (context, state) => const NewsListScreen(),
            routes: [
              GoRoute(
                  path: 'detail',
                  builder: (context, state) {
                    final news = state.extra as NewsLog;
                    return NewsDetailScreen(news: news);
                  })
            ]),
        GoRoute(
            path: 'maps',
            builder: (context, state) => const MapTrackerScreen(),
            routes: [
              GoRoute(
                  path: 'list',
                  builder: (context, state) => const LocationListScreen()),
            ])
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
