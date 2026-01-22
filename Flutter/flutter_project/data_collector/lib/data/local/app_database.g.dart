// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'app_database.dart';

// ignore_for_file: type=lint
class $PeopleTable extends People with TableInfo<$PeopleTable, Person> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $PeopleTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _nameMeta = const VerificationMeta('name');
  @override
  late final GeneratedColumn<String> name = GeneratedColumn<String>(
      'name', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _ageMeta = const VerificationMeta('age');
  @override
  late final GeneratedColumn<int> age = GeneratedColumn<int>(
      'age', aliasedName, true,
      type: DriftSqlType.int, requiredDuringInsert: false);
  static const VerificationMeta _photoUrlMeta =
      const VerificationMeta('photoUrl');
  @override
  late final GeneratedColumn<String> photoUrl = GeneratedColumn<String>(
      'photo_url', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  @override
  late final GeneratedColumnWithTypeConverter<Map<String, dynamic>, String>
      attributes = GeneratedColumn<String>('attributes', aliasedName, false,
              type: DriftSqlType.string, requiredDuringInsert: true)
          .withConverter<Map<String, dynamic>>(
              $PeopleTable.$converterattributes);
  @override
  List<GeneratedColumn> get $columns => [id, name, age, photoUrl, attributes];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'people';
  @override
  VerificationContext validateIntegrity(Insertable<Person> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('name')) {
      context.handle(
          _nameMeta, name.isAcceptableOrUnknown(data['name']!, _nameMeta));
    } else if (isInserting) {
      context.missing(_nameMeta);
    }
    if (data.containsKey('age')) {
      context.handle(
          _ageMeta, age.isAcceptableOrUnknown(data['age']!, _ageMeta));
    }
    if (data.containsKey('photo_url')) {
      context.handle(_photoUrlMeta,
          photoUrl.isAcceptableOrUnknown(data['photo_url']!, _photoUrlMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  Person map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return Person(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      name: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}name'])!,
      age: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}age']),
      photoUrl: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}photo_url']),
      attributes: $PeopleTable.$converterattributes.fromSql(attachedDatabase
          .typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}attributes'])!),
    );
  }

  @override
  $PeopleTable createAlias(String alias) {
    return $PeopleTable(attachedDatabase, alias);
  }

  static TypeConverter<Map<String, dynamic>, String> $converterattributes =
      const JsonConverter();
}

class Person extends DataClass implements Insertable<Person> {
  final String id;
  final String name;
  final int? age;
  final String? photoUrl;
  final Map<String, dynamic> attributes;
  const Person(
      {required this.id,
      required this.name,
      this.age,
      this.photoUrl,
      required this.attributes});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['name'] = Variable<String>(name);
    if (!nullToAbsent || age != null) {
      map['age'] = Variable<int>(age);
    }
    if (!nullToAbsent || photoUrl != null) {
      map['photo_url'] = Variable<String>(photoUrl);
    }
    {
      map['attributes'] =
          Variable<String>($PeopleTable.$converterattributes.toSql(attributes));
    }
    return map;
  }

  PeopleCompanion toCompanion(bool nullToAbsent) {
    return PeopleCompanion(
      id: Value(id),
      name: Value(name),
      age: age == null && nullToAbsent ? const Value.absent() : Value(age),
      photoUrl: photoUrl == null && nullToAbsent
          ? const Value.absent()
          : Value(photoUrl),
      attributes: Value(attributes),
    );
  }

  factory Person.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return Person(
      id: serializer.fromJson<String>(json['id']),
      name: serializer.fromJson<String>(json['name']),
      age: serializer.fromJson<int?>(json['age']),
      photoUrl: serializer.fromJson<String?>(json['photoUrl']),
      attributes: serializer.fromJson<Map<String, dynamic>>(json['attributes']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'name': serializer.toJson<String>(name),
      'age': serializer.toJson<int?>(age),
      'photoUrl': serializer.toJson<String?>(photoUrl),
      'attributes': serializer.toJson<Map<String, dynamic>>(attributes),
    };
  }

  Person copyWith(
          {String? id,
          String? name,
          Value<int?> age = const Value.absent(),
          Value<String?> photoUrl = const Value.absent(),
          Map<String, dynamic>? attributes}) =>
      Person(
        id: id ?? this.id,
        name: name ?? this.name,
        age: age.present ? age.value : this.age,
        photoUrl: photoUrl.present ? photoUrl.value : this.photoUrl,
        attributes: attributes ?? this.attributes,
      );
  Person copyWithCompanion(PeopleCompanion data) {
    return Person(
      id: data.id.present ? data.id.value : this.id,
      name: data.name.present ? data.name.value : this.name,
      age: data.age.present ? data.age.value : this.age,
      photoUrl: data.photoUrl.present ? data.photoUrl.value : this.photoUrl,
      attributes:
          data.attributes.present ? data.attributes.value : this.attributes,
    );
  }

  @override
  String toString() {
    return (StringBuffer('Person(')
          ..write('id: $id, ')
          ..write('name: $name, ')
          ..write('age: $age, ')
          ..write('photoUrl: $photoUrl, ')
          ..write('attributes: $attributes')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, name, age, photoUrl, attributes);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Person &&
          other.id == this.id &&
          other.name == this.name &&
          other.age == this.age &&
          other.photoUrl == this.photoUrl &&
          other.attributes == this.attributes);
}

class PeopleCompanion extends UpdateCompanion<Person> {
  final Value<String> id;
  final Value<String> name;
  final Value<int?> age;
  final Value<String?> photoUrl;
  final Value<Map<String, dynamic>> attributes;
  final Value<int> rowid;
  const PeopleCompanion({
    this.id = const Value.absent(),
    this.name = const Value.absent(),
    this.age = const Value.absent(),
    this.photoUrl = const Value.absent(),
    this.attributes = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  PeopleCompanion.insert({
    required String id,
    required String name,
    this.age = const Value.absent(),
    this.photoUrl = const Value.absent(),
    required Map<String, dynamic> attributes,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        name = Value(name),
        attributes = Value(attributes);
  static Insertable<Person> custom({
    Expression<String>? id,
    Expression<String>? name,
    Expression<int>? age,
    Expression<String>? photoUrl,
    Expression<String>? attributes,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (name != null) 'name': name,
      if (age != null) 'age': age,
      if (photoUrl != null) 'photo_url': photoUrl,
      if (attributes != null) 'attributes': attributes,
      if (rowid != null) 'rowid': rowid,
    });
  }

  PeopleCompanion copyWith(
      {Value<String>? id,
      Value<String>? name,
      Value<int?>? age,
      Value<String?>? photoUrl,
      Value<Map<String, dynamic>>? attributes,
      Value<int>? rowid}) {
    return PeopleCompanion(
      id: id ?? this.id,
      name: name ?? this.name,
      age: age ?? this.age,
      photoUrl: photoUrl ?? this.photoUrl,
      attributes: attributes ?? this.attributes,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (name.present) {
      map['name'] = Variable<String>(name.value);
    }
    if (age.present) {
      map['age'] = Variable<int>(age.value);
    }
    if (photoUrl.present) {
      map['photo_url'] = Variable<String>(photoUrl.value);
    }
    if (attributes.present) {
      map['attributes'] = Variable<String>(
          $PeopleTable.$converterattributes.toSql(attributes.value));
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('PeopleCompanion(')
          ..write('id: $id, ')
          ..write('name: $name, ')
          ..write('age: $age, ')
          ..write('photoUrl: $photoUrl, ')
          ..write('attributes: $attributes, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $NewsLogsTable extends NewsLogs with TableInfo<$NewsLogsTable, NewsLog> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $NewsLogsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _titleMeta = const VerificationMeta('title');
  @override
  late final GeneratedColumn<String> title = GeneratedColumn<String>(
      'title', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _contentMeta =
      const VerificationMeta('content');
  @override
  late final GeneratedColumn<String> content = GeneratedColumn<String>(
      'content', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _timestampMeta =
      const VerificationMeta('timestamp');
  @override
  late final GeneratedColumn<DateTime> timestamp = GeneratedColumn<DateTime>(
      'timestamp', aliasedName, false,
      type: DriftSqlType.dateTime, requiredDuringInsert: true);
  static const VerificationMeta _imageUrlMeta =
      const VerificationMeta('imageUrl');
  @override
  late final GeneratedColumn<String> imageUrl = GeneratedColumn<String>(
      'image_url', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  @override
  List<GeneratedColumn> get $columns =>
      [id, title, content, timestamp, imageUrl];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'news_logs';
  @override
  VerificationContext validateIntegrity(Insertable<NewsLog> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('title')) {
      context.handle(
          _titleMeta, title.isAcceptableOrUnknown(data['title']!, _titleMeta));
    } else if (isInserting) {
      context.missing(_titleMeta);
    }
    if (data.containsKey('content')) {
      context.handle(_contentMeta,
          content.isAcceptableOrUnknown(data['content']!, _contentMeta));
    } else if (isInserting) {
      context.missing(_contentMeta);
    }
    if (data.containsKey('timestamp')) {
      context.handle(_timestampMeta,
          timestamp.isAcceptableOrUnknown(data['timestamp']!, _timestampMeta));
    } else if (isInserting) {
      context.missing(_timestampMeta);
    }
    if (data.containsKey('image_url')) {
      context.handle(_imageUrlMeta,
          imageUrl.isAcceptableOrUnknown(data['image_url']!, _imageUrlMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  NewsLog map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return NewsLog(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      title: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}title'])!,
      content: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}content'])!,
      timestamp: attachedDatabase.typeMapping
          .read(DriftSqlType.dateTime, data['${effectivePrefix}timestamp'])!,
      imageUrl: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}image_url']),
    );
  }

  @override
  $NewsLogsTable createAlias(String alias) {
    return $NewsLogsTable(attachedDatabase, alias);
  }
}

class NewsLog extends DataClass implements Insertable<NewsLog> {
  final String id;
  final String title;
  final String content;
  final DateTime timestamp;
  final String? imageUrl;
  const NewsLog(
      {required this.id,
      required this.title,
      required this.content,
      required this.timestamp,
      this.imageUrl});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['title'] = Variable<String>(title);
    map['content'] = Variable<String>(content);
    map['timestamp'] = Variable<DateTime>(timestamp);
    if (!nullToAbsent || imageUrl != null) {
      map['image_url'] = Variable<String>(imageUrl);
    }
    return map;
  }

  NewsLogsCompanion toCompanion(bool nullToAbsent) {
    return NewsLogsCompanion(
      id: Value(id),
      title: Value(title),
      content: Value(content),
      timestamp: Value(timestamp),
      imageUrl: imageUrl == null && nullToAbsent
          ? const Value.absent()
          : Value(imageUrl),
    );
  }

  factory NewsLog.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return NewsLog(
      id: serializer.fromJson<String>(json['id']),
      title: serializer.fromJson<String>(json['title']),
      content: serializer.fromJson<String>(json['content']),
      timestamp: serializer.fromJson<DateTime>(json['timestamp']),
      imageUrl: serializer.fromJson<String?>(json['imageUrl']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'title': serializer.toJson<String>(title),
      'content': serializer.toJson<String>(content),
      'timestamp': serializer.toJson<DateTime>(timestamp),
      'imageUrl': serializer.toJson<String?>(imageUrl),
    };
  }

  NewsLog copyWith(
          {String? id,
          String? title,
          String? content,
          DateTime? timestamp,
          Value<String?> imageUrl = const Value.absent()}) =>
      NewsLog(
        id: id ?? this.id,
        title: title ?? this.title,
        content: content ?? this.content,
        timestamp: timestamp ?? this.timestamp,
        imageUrl: imageUrl.present ? imageUrl.value : this.imageUrl,
      );
  NewsLog copyWithCompanion(NewsLogsCompanion data) {
    return NewsLog(
      id: data.id.present ? data.id.value : this.id,
      title: data.title.present ? data.title.value : this.title,
      content: data.content.present ? data.content.value : this.content,
      timestamp: data.timestamp.present ? data.timestamp.value : this.timestamp,
      imageUrl: data.imageUrl.present ? data.imageUrl.value : this.imageUrl,
    );
  }

  @override
  String toString() {
    return (StringBuffer('NewsLog(')
          ..write('id: $id, ')
          ..write('title: $title, ')
          ..write('content: $content, ')
          ..write('timestamp: $timestamp, ')
          ..write('imageUrl: $imageUrl')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, title, content, timestamp, imageUrl);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is NewsLog &&
          other.id == this.id &&
          other.title == this.title &&
          other.content == this.content &&
          other.timestamp == this.timestamp &&
          other.imageUrl == this.imageUrl);
}

class NewsLogsCompanion extends UpdateCompanion<NewsLog> {
  final Value<String> id;
  final Value<String> title;
  final Value<String> content;
  final Value<DateTime> timestamp;
  final Value<String?> imageUrl;
  final Value<int> rowid;
  const NewsLogsCompanion({
    this.id = const Value.absent(),
    this.title = const Value.absent(),
    this.content = const Value.absent(),
    this.timestamp = const Value.absent(),
    this.imageUrl = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  NewsLogsCompanion.insert({
    required String id,
    required String title,
    required String content,
    required DateTime timestamp,
    this.imageUrl = const Value.absent(),
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        title = Value(title),
        content = Value(content),
        timestamp = Value(timestamp);
  static Insertable<NewsLog> custom({
    Expression<String>? id,
    Expression<String>? title,
    Expression<String>? content,
    Expression<DateTime>? timestamp,
    Expression<String>? imageUrl,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (title != null) 'title': title,
      if (content != null) 'content': content,
      if (timestamp != null) 'timestamp': timestamp,
      if (imageUrl != null) 'image_url': imageUrl,
      if (rowid != null) 'rowid': rowid,
    });
  }

  NewsLogsCompanion copyWith(
      {Value<String>? id,
      Value<String>? title,
      Value<String>? content,
      Value<DateTime>? timestamp,
      Value<String?>? imageUrl,
      Value<int>? rowid}) {
    return NewsLogsCompanion(
      id: id ?? this.id,
      title: title ?? this.title,
      content: content ?? this.content,
      timestamp: timestamp ?? this.timestamp,
      imageUrl: imageUrl ?? this.imageUrl,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (title.present) {
      map['title'] = Variable<String>(title.value);
    }
    if (content.present) {
      map['content'] = Variable<String>(content.value);
    }
    if (timestamp.present) {
      map['timestamp'] = Variable<DateTime>(timestamp.value);
    }
    if (imageUrl.present) {
      map['image_url'] = Variable<String>(imageUrl.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('NewsLogsCompanion(')
          ..write('id: $id, ')
          ..write('title: $title, ')
          ..write('content: $content, ')
          ..write('timestamp: $timestamp, ')
          ..write('imageUrl: $imageUrl, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $MapLogsTable extends MapLogs with TableInfo<$MapLogsTable, MapLog> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $MapLogsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _sessionIdMeta =
      const VerificationMeta('sessionId');
  @override
  late final GeneratedColumn<String> sessionId = GeneratedColumn<String>(
      'session_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _latMeta = const VerificationMeta('lat');
  @override
  late final GeneratedColumn<double> lat = GeneratedColumn<double>(
      'lat', aliasedName, false,
      type: DriftSqlType.double, requiredDuringInsert: true);
  static const VerificationMeta _lngMeta = const VerificationMeta('lng');
  @override
  late final GeneratedColumn<double> lng = GeneratedColumn<double>(
      'lng', aliasedName, false,
      type: DriftSqlType.double, requiredDuringInsert: true);
  static const VerificationMeta _altitudeMeta =
      const VerificationMeta('altitude');
  @override
  late final GeneratedColumn<double> altitude = GeneratedColumn<double>(
      'altitude', aliasedName, false,
      type: DriftSqlType.double,
      requiredDuringInsert: false,
      defaultValue: const Constant(0.0));
  static const VerificationMeta _timestampMeta =
      const VerificationMeta('timestamp');
  @override
  late final GeneratedColumn<DateTime> timestamp = GeneratedColumn<DateTime>(
      'timestamp', aliasedName, false,
      type: DriftSqlType.dateTime, requiredDuringInsert: true);
  static const VerificationMeta _pointCloudUrlMeta =
      const VerificationMeta('pointCloudUrl');
  @override
  late final GeneratedColumn<String> pointCloudUrl = GeneratedColumn<String>(
      'point_cloud_url', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  @override
  List<GeneratedColumn> get $columns =>
      [id, sessionId, lat, lng, altitude, timestamp, pointCloudUrl];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'map_logs';
  @override
  VerificationContext validateIntegrity(Insertable<MapLog> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('session_id')) {
      context.handle(_sessionIdMeta,
          sessionId.isAcceptableOrUnknown(data['session_id']!, _sessionIdMeta));
    } else if (isInserting) {
      context.missing(_sessionIdMeta);
    }
    if (data.containsKey('lat')) {
      context.handle(
          _latMeta, lat.isAcceptableOrUnknown(data['lat']!, _latMeta));
    } else if (isInserting) {
      context.missing(_latMeta);
    }
    if (data.containsKey('lng')) {
      context.handle(
          _lngMeta, lng.isAcceptableOrUnknown(data['lng']!, _lngMeta));
    } else if (isInserting) {
      context.missing(_lngMeta);
    }
    if (data.containsKey('altitude')) {
      context.handle(_altitudeMeta,
          altitude.isAcceptableOrUnknown(data['altitude']!, _altitudeMeta));
    }
    if (data.containsKey('timestamp')) {
      context.handle(_timestampMeta,
          timestamp.isAcceptableOrUnknown(data['timestamp']!, _timestampMeta));
    } else if (isInserting) {
      context.missing(_timestampMeta);
    }
    if (data.containsKey('point_cloud_url')) {
      context.handle(
          _pointCloudUrlMeta,
          pointCloudUrl.isAcceptableOrUnknown(
              data['point_cloud_url']!, _pointCloudUrlMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  MapLog map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return MapLog(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      sessionId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}session_id'])!,
      lat: attachedDatabase.typeMapping
          .read(DriftSqlType.double, data['${effectivePrefix}lat'])!,
      lng: attachedDatabase.typeMapping
          .read(DriftSqlType.double, data['${effectivePrefix}lng'])!,
      altitude: attachedDatabase.typeMapping
          .read(DriftSqlType.double, data['${effectivePrefix}altitude'])!,
      timestamp: attachedDatabase.typeMapping
          .read(DriftSqlType.dateTime, data['${effectivePrefix}timestamp'])!,
      pointCloudUrl: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}point_cloud_url']),
    );
  }

  @override
  $MapLogsTable createAlias(String alias) {
    return $MapLogsTable(attachedDatabase, alias);
  }
}

class MapLog extends DataClass implements Insertable<MapLog> {
  final String id;
  final String sessionId;
  final double lat;
  final double lng;
  final double altitude;
  final DateTime timestamp;
  final String? pointCloudUrl;
  const MapLog(
      {required this.id,
      required this.sessionId,
      required this.lat,
      required this.lng,
      required this.altitude,
      required this.timestamp,
      this.pointCloudUrl});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['session_id'] = Variable<String>(sessionId);
    map['lat'] = Variable<double>(lat);
    map['lng'] = Variable<double>(lng);
    map['altitude'] = Variable<double>(altitude);
    map['timestamp'] = Variable<DateTime>(timestamp);
    if (!nullToAbsent || pointCloudUrl != null) {
      map['point_cloud_url'] = Variable<String>(pointCloudUrl);
    }
    return map;
  }

  MapLogsCompanion toCompanion(bool nullToAbsent) {
    return MapLogsCompanion(
      id: Value(id),
      sessionId: Value(sessionId),
      lat: Value(lat),
      lng: Value(lng),
      altitude: Value(altitude),
      timestamp: Value(timestamp),
      pointCloudUrl: pointCloudUrl == null && nullToAbsent
          ? const Value.absent()
          : Value(pointCloudUrl),
    );
  }

  factory MapLog.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return MapLog(
      id: serializer.fromJson<String>(json['id']),
      sessionId: serializer.fromJson<String>(json['sessionId']),
      lat: serializer.fromJson<double>(json['lat']),
      lng: serializer.fromJson<double>(json['lng']),
      altitude: serializer.fromJson<double>(json['altitude']),
      timestamp: serializer.fromJson<DateTime>(json['timestamp']),
      pointCloudUrl: serializer.fromJson<String?>(json['pointCloudUrl']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'sessionId': serializer.toJson<String>(sessionId),
      'lat': serializer.toJson<double>(lat),
      'lng': serializer.toJson<double>(lng),
      'altitude': serializer.toJson<double>(altitude),
      'timestamp': serializer.toJson<DateTime>(timestamp),
      'pointCloudUrl': serializer.toJson<String?>(pointCloudUrl),
    };
  }

  MapLog copyWith(
          {String? id,
          String? sessionId,
          double? lat,
          double? lng,
          double? altitude,
          DateTime? timestamp,
          Value<String?> pointCloudUrl = const Value.absent()}) =>
      MapLog(
        id: id ?? this.id,
        sessionId: sessionId ?? this.sessionId,
        lat: lat ?? this.lat,
        lng: lng ?? this.lng,
        altitude: altitude ?? this.altitude,
        timestamp: timestamp ?? this.timestamp,
        pointCloudUrl:
            pointCloudUrl.present ? pointCloudUrl.value : this.pointCloudUrl,
      );
  MapLog copyWithCompanion(MapLogsCompanion data) {
    return MapLog(
      id: data.id.present ? data.id.value : this.id,
      sessionId: data.sessionId.present ? data.sessionId.value : this.sessionId,
      lat: data.lat.present ? data.lat.value : this.lat,
      lng: data.lng.present ? data.lng.value : this.lng,
      altitude: data.altitude.present ? data.altitude.value : this.altitude,
      timestamp: data.timestamp.present ? data.timestamp.value : this.timestamp,
      pointCloudUrl: data.pointCloudUrl.present
          ? data.pointCloudUrl.value
          : this.pointCloudUrl,
    );
  }

  @override
  String toString() {
    return (StringBuffer('MapLog(')
          ..write('id: $id, ')
          ..write('sessionId: $sessionId, ')
          ..write('lat: $lat, ')
          ..write('lng: $lng, ')
          ..write('altitude: $altitude, ')
          ..write('timestamp: $timestamp, ')
          ..write('pointCloudUrl: $pointCloudUrl')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode =>
      Object.hash(id, sessionId, lat, lng, altitude, timestamp, pointCloudUrl);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is MapLog &&
          other.id == this.id &&
          other.sessionId == this.sessionId &&
          other.lat == this.lat &&
          other.lng == this.lng &&
          other.altitude == this.altitude &&
          other.timestamp == this.timestamp &&
          other.pointCloudUrl == this.pointCloudUrl);
}

class MapLogsCompanion extends UpdateCompanion<MapLog> {
  final Value<String> id;
  final Value<String> sessionId;
  final Value<double> lat;
  final Value<double> lng;
  final Value<double> altitude;
  final Value<DateTime> timestamp;
  final Value<String?> pointCloudUrl;
  final Value<int> rowid;
  const MapLogsCompanion({
    this.id = const Value.absent(),
    this.sessionId = const Value.absent(),
    this.lat = const Value.absent(),
    this.lng = const Value.absent(),
    this.altitude = const Value.absent(),
    this.timestamp = const Value.absent(),
    this.pointCloudUrl = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  MapLogsCompanion.insert({
    required String id,
    required String sessionId,
    required double lat,
    required double lng,
    this.altitude = const Value.absent(),
    required DateTime timestamp,
    this.pointCloudUrl = const Value.absent(),
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        sessionId = Value(sessionId),
        lat = Value(lat),
        lng = Value(lng),
        timestamp = Value(timestamp);
  static Insertable<MapLog> custom({
    Expression<String>? id,
    Expression<String>? sessionId,
    Expression<double>? lat,
    Expression<double>? lng,
    Expression<double>? altitude,
    Expression<DateTime>? timestamp,
    Expression<String>? pointCloudUrl,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (sessionId != null) 'session_id': sessionId,
      if (lat != null) 'lat': lat,
      if (lng != null) 'lng': lng,
      if (altitude != null) 'altitude': altitude,
      if (timestamp != null) 'timestamp': timestamp,
      if (pointCloudUrl != null) 'point_cloud_url': pointCloudUrl,
      if (rowid != null) 'rowid': rowid,
    });
  }

  MapLogsCompanion copyWith(
      {Value<String>? id,
      Value<String>? sessionId,
      Value<double>? lat,
      Value<double>? lng,
      Value<double>? altitude,
      Value<DateTime>? timestamp,
      Value<String?>? pointCloudUrl,
      Value<int>? rowid}) {
    return MapLogsCompanion(
      id: id ?? this.id,
      sessionId: sessionId ?? this.sessionId,
      lat: lat ?? this.lat,
      lng: lng ?? this.lng,
      altitude: altitude ?? this.altitude,
      timestamp: timestamp ?? this.timestamp,
      pointCloudUrl: pointCloudUrl ?? this.pointCloudUrl,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (sessionId.present) {
      map['session_id'] = Variable<String>(sessionId.value);
    }
    if (lat.present) {
      map['lat'] = Variable<double>(lat.value);
    }
    if (lng.present) {
      map['lng'] = Variable<double>(lng.value);
    }
    if (altitude.present) {
      map['altitude'] = Variable<double>(altitude.value);
    }
    if (timestamp.present) {
      map['timestamp'] = Variable<DateTime>(timestamp.value);
    }
    if (pointCloudUrl.present) {
      map['point_cloud_url'] = Variable<String>(pointCloudUrl.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('MapLogsCompanion(')
          ..write('id: $id, ')
          ..write('sessionId: $sessionId, ')
          ..write('lat: $lat, ')
          ..write('lng: $lng, ')
          ..write('altitude: $altitude, ')
          ..write('timestamp: $timestamp, ')
          ..write('pointCloudUrl: $pointCloudUrl, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

abstract class _$AppDatabase extends GeneratedDatabase {
  _$AppDatabase(QueryExecutor e) : super(e);
  $AppDatabaseManager get managers => $AppDatabaseManager(this);
  late final $PeopleTable people = $PeopleTable(this);
  late final $NewsLogsTable newsLogs = $NewsLogsTable(this);
  late final $MapLogsTable mapLogs = $MapLogsTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities =>
      [people, newsLogs, mapLogs];
}

typedef $$PeopleTableCreateCompanionBuilder = PeopleCompanion Function({
  required String id,
  required String name,
  Value<int?> age,
  Value<String?> photoUrl,
  required Map<String, dynamic> attributes,
  Value<int> rowid,
});
typedef $$PeopleTableUpdateCompanionBuilder = PeopleCompanion Function({
  Value<String> id,
  Value<String> name,
  Value<int?> age,
  Value<String?> photoUrl,
  Value<Map<String, dynamic>> attributes,
  Value<int> rowid,
});

class $$PeopleTableFilterComposer
    extends Composer<_$AppDatabase, $PeopleTable> {
  $$PeopleTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get name => $composableBuilder(
      column: $table.name, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get age => $composableBuilder(
      column: $table.age, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get photoUrl => $composableBuilder(
      column: $table.photoUrl, builder: (column) => ColumnFilters(column));

  ColumnWithTypeConverterFilters<Map<String, dynamic>, Map<String, dynamic>,
          String>
      get attributes => $composableBuilder(
          column: $table.attributes,
          builder: (column) => ColumnWithTypeConverterFilters(column));
}

class $$PeopleTableOrderingComposer
    extends Composer<_$AppDatabase, $PeopleTable> {
  $$PeopleTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get name => $composableBuilder(
      column: $table.name, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get age => $composableBuilder(
      column: $table.age, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get photoUrl => $composableBuilder(
      column: $table.photoUrl, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get attributes => $composableBuilder(
      column: $table.attributes, builder: (column) => ColumnOrderings(column));
}

class $$PeopleTableAnnotationComposer
    extends Composer<_$AppDatabase, $PeopleTable> {
  $$PeopleTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get name =>
      $composableBuilder(column: $table.name, builder: (column) => column);

  GeneratedColumn<int> get age =>
      $composableBuilder(column: $table.age, builder: (column) => column);

  GeneratedColumn<String> get photoUrl =>
      $composableBuilder(column: $table.photoUrl, builder: (column) => column);

  GeneratedColumnWithTypeConverter<Map<String, dynamic>, String>
      get attributes => $composableBuilder(
          column: $table.attributes, builder: (column) => column);
}

class $$PeopleTableTableManager extends RootTableManager<
    _$AppDatabase,
    $PeopleTable,
    Person,
    $$PeopleTableFilterComposer,
    $$PeopleTableOrderingComposer,
    $$PeopleTableAnnotationComposer,
    $$PeopleTableCreateCompanionBuilder,
    $$PeopleTableUpdateCompanionBuilder,
    (Person, BaseReferences<_$AppDatabase, $PeopleTable, Person>),
    Person,
    PrefetchHooks Function()> {
  $$PeopleTableTableManager(_$AppDatabase db, $PeopleTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$PeopleTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$PeopleTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$PeopleTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> name = const Value.absent(),
            Value<int?> age = const Value.absent(),
            Value<String?> photoUrl = const Value.absent(),
            Value<Map<String, dynamic>> attributes = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              PeopleCompanion(
            id: id,
            name: name,
            age: age,
            photoUrl: photoUrl,
            attributes: attributes,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String name,
            Value<int?> age = const Value.absent(),
            Value<String?> photoUrl = const Value.absent(),
            required Map<String, dynamic> attributes,
            Value<int> rowid = const Value.absent(),
          }) =>
              PeopleCompanion.insert(
            id: id,
            name: name,
            age: age,
            photoUrl: photoUrl,
            attributes: attributes,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$PeopleTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $PeopleTable,
    Person,
    $$PeopleTableFilterComposer,
    $$PeopleTableOrderingComposer,
    $$PeopleTableAnnotationComposer,
    $$PeopleTableCreateCompanionBuilder,
    $$PeopleTableUpdateCompanionBuilder,
    (Person, BaseReferences<_$AppDatabase, $PeopleTable, Person>),
    Person,
    PrefetchHooks Function()>;
typedef $$NewsLogsTableCreateCompanionBuilder = NewsLogsCompanion Function({
  required String id,
  required String title,
  required String content,
  required DateTime timestamp,
  Value<String?> imageUrl,
  Value<int> rowid,
});
typedef $$NewsLogsTableUpdateCompanionBuilder = NewsLogsCompanion Function({
  Value<String> id,
  Value<String> title,
  Value<String> content,
  Value<DateTime> timestamp,
  Value<String?> imageUrl,
  Value<int> rowid,
});

class $$NewsLogsTableFilterComposer
    extends Composer<_$AppDatabase, $NewsLogsTable> {
  $$NewsLogsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get title => $composableBuilder(
      column: $table.title, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get content => $composableBuilder(
      column: $table.content, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get timestamp => $composableBuilder(
      column: $table.timestamp, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get imageUrl => $composableBuilder(
      column: $table.imageUrl, builder: (column) => ColumnFilters(column));
}

class $$NewsLogsTableOrderingComposer
    extends Composer<_$AppDatabase, $NewsLogsTable> {
  $$NewsLogsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get title => $composableBuilder(
      column: $table.title, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get content => $composableBuilder(
      column: $table.content, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get timestamp => $composableBuilder(
      column: $table.timestamp, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get imageUrl => $composableBuilder(
      column: $table.imageUrl, builder: (column) => ColumnOrderings(column));
}

class $$NewsLogsTableAnnotationComposer
    extends Composer<_$AppDatabase, $NewsLogsTable> {
  $$NewsLogsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get title =>
      $composableBuilder(column: $table.title, builder: (column) => column);

  GeneratedColumn<String> get content =>
      $composableBuilder(column: $table.content, builder: (column) => column);

  GeneratedColumn<DateTime> get timestamp =>
      $composableBuilder(column: $table.timestamp, builder: (column) => column);

  GeneratedColumn<String> get imageUrl =>
      $composableBuilder(column: $table.imageUrl, builder: (column) => column);
}

class $$NewsLogsTableTableManager extends RootTableManager<
    _$AppDatabase,
    $NewsLogsTable,
    NewsLog,
    $$NewsLogsTableFilterComposer,
    $$NewsLogsTableOrderingComposer,
    $$NewsLogsTableAnnotationComposer,
    $$NewsLogsTableCreateCompanionBuilder,
    $$NewsLogsTableUpdateCompanionBuilder,
    (NewsLog, BaseReferences<_$AppDatabase, $NewsLogsTable, NewsLog>),
    NewsLog,
    PrefetchHooks Function()> {
  $$NewsLogsTableTableManager(_$AppDatabase db, $NewsLogsTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$NewsLogsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$NewsLogsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$NewsLogsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> title = const Value.absent(),
            Value<String> content = const Value.absent(),
            Value<DateTime> timestamp = const Value.absent(),
            Value<String?> imageUrl = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              NewsLogsCompanion(
            id: id,
            title: title,
            content: content,
            timestamp: timestamp,
            imageUrl: imageUrl,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String title,
            required String content,
            required DateTime timestamp,
            Value<String?> imageUrl = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              NewsLogsCompanion.insert(
            id: id,
            title: title,
            content: content,
            timestamp: timestamp,
            imageUrl: imageUrl,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$NewsLogsTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $NewsLogsTable,
    NewsLog,
    $$NewsLogsTableFilterComposer,
    $$NewsLogsTableOrderingComposer,
    $$NewsLogsTableAnnotationComposer,
    $$NewsLogsTableCreateCompanionBuilder,
    $$NewsLogsTableUpdateCompanionBuilder,
    (NewsLog, BaseReferences<_$AppDatabase, $NewsLogsTable, NewsLog>),
    NewsLog,
    PrefetchHooks Function()>;
typedef $$MapLogsTableCreateCompanionBuilder = MapLogsCompanion Function({
  required String id,
  required String sessionId,
  required double lat,
  required double lng,
  Value<double> altitude,
  required DateTime timestamp,
  Value<String?> pointCloudUrl,
  Value<int> rowid,
});
typedef $$MapLogsTableUpdateCompanionBuilder = MapLogsCompanion Function({
  Value<String> id,
  Value<String> sessionId,
  Value<double> lat,
  Value<double> lng,
  Value<double> altitude,
  Value<DateTime> timestamp,
  Value<String?> pointCloudUrl,
  Value<int> rowid,
});

class $$MapLogsTableFilterComposer
    extends Composer<_$AppDatabase, $MapLogsTable> {
  $$MapLogsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get sessionId => $composableBuilder(
      column: $table.sessionId, builder: (column) => ColumnFilters(column));

  ColumnFilters<double> get lat => $composableBuilder(
      column: $table.lat, builder: (column) => ColumnFilters(column));

  ColumnFilters<double> get lng => $composableBuilder(
      column: $table.lng, builder: (column) => ColumnFilters(column));

  ColumnFilters<double> get altitude => $composableBuilder(
      column: $table.altitude, builder: (column) => ColumnFilters(column));

  ColumnFilters<DateTime> get timestamp => $composableBuilder(
      column: $table.timestamp, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get pointCloudUrl => $composableBuilder(
      column: $table.pointCloudUrl, builder: (column) => ColumnFilters(column));
}

class $$MapLogsTableOrderingComposer
    extends Composer<_$AppDatabase, $MapLogsTable> {
  $$MapLogsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get sessionId => $composableBuilder(
      column: $table.sessionId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<double> get lat => $composableBuilder(
      column: $table.lat, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<double> get lng => $composableBuilder(
      column: $table.lng, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<double> get altitude => $composableBuilder(
      column: $table.altitude, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<DateTime> get timestamp => $composableBuilder(
      column: $table.timestamp, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get pointCloudUrl => $composableBuilder(
      column: $table.pointCloudUrl,
      builder: (column) => ColumnOrderings(column));
}

class $$MapLogsTableAnnotationComposer
    extends Composer<_$AppDatabase, $MapLogsTable> {
  $$MapLogsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get sessionId =>
      $composableBuilder(column: $table.sessionId, builder: (column) => column);

  GeneratedColumn<double> get lat =>
      $composableBuilder(column: $table.lat, builder: (column) => column);

  GeneratedColumn<double> get lng =>
      $composableBuilder(column: $table.lng, builder: (column) => column);

  GeneratedColumn<double> get altitude =>
      $composableBuilder(column: $table.altitude, builder: (column) => column);

  GeneratedColumn<DateTime> get timestamp =>
      $composableBuilder(column: $table.timestamp, builder: (column) => column);

  GeneratedColumn<String> get pointCloudUrl => $composableBuilder(
      column: $table.pointCloudUrl, builder: (column) => column);
}

class $$MapLogsTableTableManager extends RootTableManager<
    _$AppDatabase,
    $MapLogsTable,
    MapLog,
    $$MapLogsTableFilterComposer,
    $$MapLogsTableOrderingComposer,
    $$MapLogsTableAnnotationComposer,
    $$MapLogsTableCreateCompanionBuilder,
    $$MapLogsTableUpdateCompanionBuilder,
    (MapLog, BaseReferences<_$AppDatabase, $MapLogsTable, MapLog>),
    MapLog,
    PrefetchHooks Function()> {
  $$MapLogsTableTableManager(_$AppDatabase db, $MapLogsTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$MapLogsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$MapLogsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$MapLogsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> sessionId = const Value.absent(),
            Value<double> lat = const Value.absent(),
            Value<double> lng = const Value.absent(),
            Value<double> altitude = const Value.absent(),
            Value<DateTime> timestamp = const Value.absent(),
            Value<String?> pointCloudUrl = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              MapLogsCompanion(
            id: id,
            sessionId: sessionId,
            lat: lat,
            lng: lng,
            altitude: altitude,
            timestamp: timestamp,
            pointCloudUrl: pointCloudUrl,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String sessionId,
            required double lat,
            required double lng,
            Value<double> altitude = const Value.absent(),
            required DateTime timestamp,
            Value<String?> pointCloudUrl = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              MapLogsCompanion.insert(
            id: id,
            sessionId: sessionId,
            lat: lat,
            lng: lng,
            altitude: altitude,
            timestamp: timestamp,
            pointCloudUrl: pointCloudUrl,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$MapLogsTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $MapLogsTable,
    MapLog,
    $$MapLogsTableFilterComposer,
    $$MapLogsTableOrderingComposer,
    $$MapLogsTableAnnotationComposer,
    $$MapLogsTableCreateCompanionBuilder,
    $$MapLogsTableUpdateCompanionBuilder,
    (MapLog, BaseReferences<_$AppDatabase, $MapLogsTable, MapLog>),
    MapLog,
    PrefetchHooks Function()>;

class $AppDatabaseManager {
  final _$AppDatabase _db;
  $AppDatabaseManager(this._db);
  $$PeopleTableTableManager get people =>
      $$PeopleTableTableManager(_db, _db.people);
  $$NewsLogsTableTableManager get newsLogs =>
      $$NewsLogsTableTableManager(_db, _db.newsLogs);
  $$MapLogsTableTableManager get mapLogs =>
      $$MapLogsTableTableManager(_db, _db.mapLogs);
}
