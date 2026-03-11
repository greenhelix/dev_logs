import '../models/app_settings.dart';
import 'auth_header_provider.dart';

class WebAuthHeaderProvider implements AuthHeaderProvider {
  @override
  Future<Map<String, String>> buildHeaders(AppSettings settings) async {
    return const {};
  }
}

AuthHeaderProvider createProvider() => WebAuthHeaderProvider();
