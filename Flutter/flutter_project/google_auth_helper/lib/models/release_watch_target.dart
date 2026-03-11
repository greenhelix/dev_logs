enum ReleaseWatchSourceType { excel, gsheet, web }

extension ReleaseWatchSourceTypeX on ReleaseWatchSourceType {
  String get storageKey {
    switch (this) {
      case ReleaseWatchSourceType.excel:
        return 'excel';
      case ReleaseWatchSourceType.gsheet:
        return 'gsheet';
      case ReleaseWatchSourceType.web:
        return 'web';
    }
  }

  String get label {
    switch (this) {
      case ReleaseWatchSourceType.excel:
        return 'Excel';
      case ReleaseWatchSourceType.gsheet:
        return 'Google Sheet';
      case ReleaseWatchSourceType.web:
        return 'Web';
    }
  }

  static ReleaseWatchSourceType fromStorageKey(String value) {
    return ReleaseWatchSourceType.values.firstWhere(
      (item) => item.storageKey == value,
      orElse: () => ReleaseWatchSourceType.web,
    );
  }
}

class ReleaseWatchTarget {
  const ReleaseWatchTarget({
    required this.id,
    required this.name,
    required this.sourceType,
    required this.sourceRef,
    required this.parserRule,
    required this.status,
    this.lastSnapshot = '',
    this.lastDiff = '',
    this.lastCheckedAt,
  });

  final String id;
  final String name;
  final ReleaseWatchSourceType sourceType;
  final String sourceRef;
  final String parserRule;
  final String status;
  final String lastSnapshot;
  final String lastDiff;
  final DateTime? lastCheckedAt;

  ReleaseWatchTarget copyWith({
    String? id,
    String? name,
    ReleaseWatchSourceType? sourceType,
    String? sourceRef,
    String? parserRule,
    String? status,
    String? lastSnapshot,
    String? lastDiff,
    DateTime? lastCheckedAt,
  }) {
    return ReleaseWatchTarget(
      id: id ?? this.id,
      name: name ?? this.name,
      sourceType: sourceType ?? this.sourceType,
      sourceRef: sourceRef ?? this.sourceRef,
      parserRule: parserRule ?? this.parserRule,
      status: status ?? this.status,
      lastSnapshot: lastSnapshot ?? this.lastSnapshot,
      lastDiff: lastDiff ?? this.lastDiff,
      lastCheckedAt: lastCheckedAt ?? this.lastCheckedAt,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'sourceType': sourceType.storageKey,
      'sourceRef': sourceRef,
      'parserRule': parserRule,
      'status': status,
      'lastSnapshot': lastSnapshot,
      'lastDiff': lastDiff,
      'lastCheckedAt': lastCheckedAt?.toUtc().toIso8601String(),
    };
  }

  factory ReleaseWatchTarget.fromMap(Map<String, dynamic> map) {
    return ReleaseWatchTarget(
      id: map['id'] as String? ?? '',
      name: map['name'] as String? ?? '',
      sourceType: ReleaseWatchSourceTypeX.fromStorageKey(
        map['sourceType'] as String? ?? 'web',
      ),
      sourceRef: map['sourceRef'] as String? ?? '',
      parserRule: map['parserRule'] as String? ?? '',
      status: map['status'] as String? ?? 'draft',
      lastSnapshot: map['lastSnapshot'] as String? ?? '',
      lastDiff: map['lastDiff'] as String? ?? '',
      lastCheckedAt: DateTime.tryParse(map['lastCheckedAt'] as String? ?? ''),
    );
  }
}
