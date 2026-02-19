import 'package:firebase_core/firebase_core.dart';

import 'core/config/firebase_config.dart';

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    // TODO(kani): Firebase Hosting 배포 전에 값을 실제 프로젝트 값으로 변경하세요.
    return const FirebaseOptions(
      apiKey: 'YOUR_API_KEY',
      appId: 'YOUR_APP_ID',
      messagingSenderId: 'YOUR_MESSAGING_SENDER_ID',
      projectId: FirebaseConfig.projectId,
      authDomain: 'YOUR_AUTH_DOMAIN',
      storageBucket: 'YOUR_STORAGE_BUCKET',
      measurementId: 'YOUR_MEASUREMENT_ID',
    );
  }
}
