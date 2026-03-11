import '../models/app_settings.dart';
import 'auth_header_provider_web.dart'
    if (dart.library.io) 'auth_header_provider_io.dart';

abstract class AuthHeaderProvider {
  Future<Map<String, String>> buildHeaders(AppSettings settings);
}

AuthHeaderProvider createAuthHeaderProvider() => createProvider();
