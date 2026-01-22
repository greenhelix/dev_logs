import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

import 'connection/connection_stub.dart'
    if (dart.library.io) 'connection/connection_native.dart'
    if (dart.library.js_interop) 'connection/connection_web.dart';

// 코드 생성기가 만들 파일 이름 (나중에 생성됨)
part 'app_database.g.dart';

// ─── 1. Tables Definition ────────────────────

/// [인물 위키] 테이블
/// 자유로운 확장을 위해 attributes 필드에 JSON을 문자열로 저장합니다.
class People extends Table {
  TextColumn get id => text()(); // UUID
  TextColumn get name => text()();
  IntColumn get age => integer().nullable()();
  TextColumn get photoUrl => text().nullable()();
  // JSON 데이터를 저장하는 컬럼 (TypeConverter 사용)
  TextColumn get attributes => text().map(const JsonConverter())();

  @override
  Set<Column> get primaryKey => {id};
}

/// [뉴스 아카이브] 테이블
class NewsLogs extends Table {
  TextColumn get id => text()();
  TextColumn get title => text()();
  TextColumn get content => text()();
  DateTimeColumn get timestamp => dateTime()();
  TextColumn get imageUrl => text().nullable()();

  @override
  Set<Column> get primaryKey => {id};
}

/// [지도 데이터] 테이블
/// 대량의 좌표 데이터를 저장합니다.
class MapLogs extends Table {
  TextColumn get id => text()();
  TextColumn get sessionId => text()(); // 한 번의 트래킹을 묶는 ID
  RealColumn get lat => real()();
  RealColumn get lng => real()();
  RealColumn get altitude => real().withDefault(const Constant(0.0))();
  DateTimeColumn get timestamp => dateTime()();

  // 추후 3D/LiDAR 데이터 파일 경로
  TextColumn get pointCloudUrl => text().nullable()();

  @override
  Set<Column> get primaryKey => {id};
}

// ─── 2. Type Converters ──────────────────────

/// Map<String, dynamic> <-> String (JSON) 변환기
class JsonConverter extends TypeConverter<Map<String, dynamic>, String> {
  const JsonConverter();

  @override
  Map<String, dynamic> fromSql(String fromDb) {
    return json.decode(fromDb) as Map<String, dynamic>;
  }

  @override
  String toSql(Map<String, dynamic> value) {
    return json.encode(value);
  }
}

// ─── 3. Database Class ───────────────────────

@DriftDatabase(tables: [People, NewsLogs, MapLogs])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(openConnection());

  @override
  int get schemaVersion => 1;
}
