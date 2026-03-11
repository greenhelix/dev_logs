class ReleaseWatchChange {
  const ReleaseWatchChange({
    required this.kind,
    required this.summary,
  });

  final String kind;
  final String summary;

  factory ReleaseWatchChange.fromMap(Map<String, dynamic> map) {
    return ReleaseWatchChange(
      kind: map['kind'] as String? ?? '',
      summary: map['summary'] as String? ?? '',
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'kind': kind,
      'summary': summary,
    };
  }
}

class ReleaseWatchSnapshot {
  const ReleaseWatchSnapshot({
    required this.sourceLabel,
    required this.version,
    required this.releaseNotesHash,
    required this.lastCheckedAt,
    required this.lastUploadedAt,
    required this.uploadStatus,
    required this.changes,
  });

  final String sourceLabel;
  final String version;
  final String releaseNotesHash;
  final DateTime lastCheckedAt;
  final DateTime? lastUploadedAt;
  final String uploadStatus;
  final List<ReleaseWatchChange> changes;

  factory ReleaseWatchSnapshot.fromMap(Map<String, dynamic> map) {
    return ReleaseWatchSnapshot(
      sourceLabel: map['sourceLabel'] as String? ?? '',
      version: map['version'] as String? ?? '',
      releaseNotesHash: map['releaseNotesHash'] as String? ?? '',
      lastCheckedAt: DateTime.tryParse(map['lastCheckedAt'] as String? ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
      lastUploadedAt: DateTime.tryParse(map['lastUploadedAt'] as String? ?? ''),
      uploadStatus: map['uploadStatus'] as String? ?? '',
      changes: (map['changes'] as List<dynamic>? ?? const [])
          .map((item) =>
              ReleaseWatchChange.fromMap(item as Map<String, dynamic>))
          .toList(growable: false),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'sourceLabel': sourceLabel,
      'version': version,
      'releaseNotesHash': releaseNotesHash,
      'lastCheckedAt': lastCheckedAt.toUtc().toIso8601String(),
      'lastUploadedAt': lastUploadedAt?.toUtc().toIso8601String(),
      'uploadStatus': uploadStatus,
      'changes': changes.map((item) => item.toMap()).toList(growable: false),
    };
  }
}
