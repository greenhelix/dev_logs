import 'package:flutter_test/flutter_test.dart';
import 'package:google_auth_helper/services/release_update_service.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

void main() {
  test('ReleaseUpdateService returns empty latestVersion on 404', () async {
    final service = ReleaseUpdateService(
      httpClient: MockClient((request) async {
        expect(request.url.path, contains('/releases/latest'));
        return http.Response('{"message":"Not Found"}', 404);
      }),
    );

    final status = await service.fetchLatestRelease();

    expect(status.currentVersion, '0.1.2+1');
    expect(status.latestVersion, isEmpty);
    expect(status.releaseUrl, contains('GAH-Release-Repo/releases'));
    expect(status.hasUpdate, isFalse);
  });

  test('ReleaseUpdateService parses the latest release payload', () async {
    final service = ReleaseUpdateService(
      httpClient: MockClient((_) async {
        return http.Response(
          '{"tag_name":"v0.1.2","html_url":"https://github.com/greenhelix/GAH-Release-Repo/releases/tag/v0.1.2"}',
          200,
        );
      }),
    );

    final status = await service.fetchLatestRelease();

    expect(status.latestVersion, 'v0.1.2');
    expect(status.releaseUrl, contains('/tag/v0.1.2'));
    expect(status.hasUpdate, isTrue);
  });
}
