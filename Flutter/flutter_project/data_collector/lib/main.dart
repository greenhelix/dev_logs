import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:firebase_core/firebase_core.dart';
import 'firebase_options.dart'; // 터미널에서 flutterfire configure 실행 후 생성됨

import 'app.dart'; // 다음 단계(Step 3)에서 생성할 파일

void main() async {
  // 1. Flutter 엔진과 위젯 바인딩 초기화 (비동기 처리를 위해 필수)
  WidgetsFlutterBinding.ensureInitialized();

  // 2. Firebase 초기화
  // 실제 앱 실행을 위해서는 터미널에서 'flutterfire configure'를 실행하여
  // firebase_options.dart를 생성해야 합니다.
  // 생성 전이라면 아래 try-catch 블록을 주석 처리하고 진행해도 UI 개발은 가능합니다.
  try {
    await Firebase.initializeApp(
      options:
          DefaultFirebaseOptions.currentPlatform, // firebase_options.dart 필요
    );
  } catch (e) {
    debugPrint("Firebase initialization failed (Ignore if not setup yet): $e");
  }

  // 3. 앱 실행
  // ProviderScope: Riverpod 상태 관리가 앱 전체에서 작동하도록 감싸줌
  runApp(
    const ProviderScope(
      child: DataCollectorApp(),
    ),
  );
}
