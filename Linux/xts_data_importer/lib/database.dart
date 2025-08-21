// lib/database.dart
import 'dart:io';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

part 'database.g.dart';

const String kDatabaseName = 'xts_data.sqlite';

// ... (TestResults table definition is unchanged) ...
@DataClassName('TestResult')
class TestResults extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get category => text().withDefault(const Constant('ETC'))();
  TextColumn get testDate => text().named('test_date')();
  TextColumn get module => text()();
  TextColumn get testName => text().named('test_name')();
  TextColumn get result => text()();
  TextColumn get detail => text().nullable()();
  TextColumn get description => text().nullable()();
  TextColumn get fwVersion => text().named('fw_version').nullable()();
  TextColumn get testToolVersion => text().named('test_tool_version').nullable()();
  TextColumn get securityPatch => text().named('security_patch').nullable()();
  TextColumn get sdkVersion => text().named('sdk_version').nullable()();
  TextColumn get abi => text()();

  @override
  List<String> get customConstraints => ['UNIQUE(test_name, abi, category)'];
}


@DriftDatabase(tables: [TestResults])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 2;

  @override
  MigrationStrategy get migration {
    return MigrationStrategy(
      onCreate: (m) {
        print('[Database] Creating all tables...');
        return m.createAll();
      },
      onUpgrade: (m, from, to) async {
        print('[Database] Upgrading schema from $from to $to...');
        if (from < 2) {
          await m.addColumn(testResults, testResults.category);
        }
      },
    );
  }
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, kDatabaseName));
    
    print('[Database] Opening connection to ${file.path}');

    return NativeDatabase.createInBackground(
      file,
      // logStatements: true will print all executed SQL statements
      logStatements: true, 
      setup: (db) {
        print('[Database] Configuring connection...');
        db.execute('PRAGMA busy_timeout = 5000;');
        db.execute('PRAGMA journal_mode = WAL;');
        db.execute('PRAGMA foreign_keys = ON;');
        print('[Database] Configuration complete.');
      },
    );
  });
}
