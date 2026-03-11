class FirestoreFieldCodec {
  static Map<String, dynamic> encodeDocument(Map<String, dynamic> data) {
    return {
      'fields': data.map(
        (key, value) => MapEntry(key, encodeValue(value)),
      ),
    };
  }

  static Map<String, dynamic> encodeValue(dynamic value) {
    if (value == null) {
      return {'nullValue': null};
    }
    if (value is String) {
      return {'stringValue': value};
    }
    if (value is bool) {
      return {'booleanValue': value};
    }
    if (value is int) {
      return {'integerValue': value.toString()};
    }
    if (value is double) {
      return {'doubleValue': value};
    }
    if (value is DateTime) {
      return {'timestampValue': value.toUtc().toIso8601String()};
    }
    if (value is List) {
      return {
        'arrayValue': {
          'values': value.map(encodeValue).toList(),
        },
      };
    }
    if (value is Map<String, dynamic>) {
      return {
        'mapValue': {
          'fields': value.map(
            (key, nestedValue) => MapEntry(key, encodeValue(nestedValue)),
          ),
        },
      };
    }
    return {'stringValue': value.toString()};
  }

  static Map<String, dynamic> decodeDocument(Map<String, dynamic> json) {
    final fields = json['fields'] as Map<String, dynamic>? ?? const {};
    return fields.map(
      (key, value) => MapEntry(key, decodeValue(value as Map<String, dynamic>)),
    );
  }

  static dynamic decodeValue(Map<String, dynamic> value) {
    if (value.containsKey('stringValue')) {
      return value['stringValue'];
    }
    if (value.containsKey('booleanValue')) {
      return value['booleanValue'];
    }
    if (value.containsKey('integerValue')) {
      return int.tryParse(value['integerValue'].toString()) ?? 0;
    }
    if (value.containsKey('doubleValue')) {
      return (value['doubleValue'] as num?)?.toDouble() ?? 0;
    }
    if (value.containsKey('timestampValue')) {
      return value['timestampValue'];
    }
    if (value.containsKey('nullValue')) {
      return null;
    }
    if (value.containsKey('arrayValue')) {
      final values =
          value['arrayValue']?['values'] as List<dynamic>? ?? const [];
      return values
          .map((item) => decodeValue(item as Map<String, dynamic>))
          .toList();
    }
    if (value.containsKey('mapValue')) {
      final fields =
          value['mapValue']?['fields'] as Map<String, dynamic>? ?? const {};
      return fields.map(
        (key, nestedValue) => MapEntry(
          key,
          decodeValue(nestedValue as Map<String, dynamic>),
        ),
      );
    }
    return null;
  }
}
