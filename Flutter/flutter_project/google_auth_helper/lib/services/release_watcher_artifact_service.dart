import 'dart:convert';

import '../models/release_watch_snapshot.dart';
import 'local_file_gateway.dart';

class ReleaseWatcherArtifactService {
  ReleaseWatcherArtifactService({
    required LocalFileGateway localFileGateway,
  }) : _localFileGateway = localFileGateway;

  final LocalFileGateway _localFileGateway;

  static const artifactPath =
      'tools/release_watcher/output/latest_snapshot.json';

  Future<ReleaseWatchSnapshot> loadLatestSnapshot() async {
    if (_localFileGateway.supportsLocalFiles &&
        await _localFileGateway.fileExists(artifactPath)) {
      final raw = await _localFileGateway.readAsString(artifactPath);
      return ReleaseWatchSnapshot.fromMap(
          jsonDecode(raw) as Map<String, dynamic>);
    }
    return ReleaseWatchSnapshot(
      sourceLabel: 'Google Portal Watcher',
      version: '14_r10',
      releaseNotesHash: 'demo-release-hash',
      lastCheckedAt: DateTime.parse('2026-03-11T00:00:00Z'),
      lastUploadedAt: DateTime.parse('2026-03-11T00:05:00Z'),
      uploadStatus: 'demo',
      changes: const [
        ReleaseWatchChange(
          kind: 'version',
          summary: 'Detected a new tool version in the portal output.',
        ),
        ReleaseWatchChange(
          kind: 'notes',
          summary: 'Release notes text hash changed since the previous run.',
        ),
      ],
    );
  }
}
