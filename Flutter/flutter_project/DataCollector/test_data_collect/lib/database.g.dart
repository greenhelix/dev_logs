// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'database.dart';

// ignore_for_file: type=lint
class $TestResultsTable extends TestResults
    with TableInfo<$TestResultsTable, TestResult> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $TestResultsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
    'id',
    aliasedName,
    false,
    hasAutoIncrement: true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'PRIMARY KEY AUTOINCREMENT',
    ),
  );
  static const VerificationMeta _testDateMeta = const VerificationMeta(
    'testDate',
  );
  @override
  late final GeneratedColumn<String> testDate = GeneratedColumn<String>(
    'test_date',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _abiMeta = const VerificationMeta('abi');
  @override
  late final GeneratedColumn<String> abi = GeneratedColumn<String>(
    'abi',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _moduleMeta = const VerificationMeta('module');
  @override
  late final GeneratedColumn<String> module = GeneratedColumn<String>(
    'module',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _testNameMeta = const VerificationMeta(
    'testName',
  );
  @override
  late final GeneratedColumn<String> testName = GeneratedColumn<String>(
    'test_name',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _resultMeta = const VerificationMeta('result');
  @override
  late final GeneratedColumn<String> result = GeneratedColumn<String>(
    'result',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _detailMeta = const VerificationMeta('detail');
  @override
  late final GeneratedColumn<String> detail = GeneratedColumn<String>(
    'detail',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _descriptionMeta = const VerificationMeta(
    'description',
  );
  @override
  late final GeneratedColumn<String> description = GeneratedColumn<String>(
    'description',
    aliasedName,
    true,
    type: DriftSqlType.string,
    requiredDuringInsert: false,
  );
  static const VerificationMeta _fwVersionMeta = const VerificationMeta(
    'fwVersion',
  );
  @override
  late final GeneratedColumn<String> fwVersion = GeneratedColumn<String>(
    'fw_version',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _testToolVersionMeta = const VerificationMeta(
    'testToolVersion',
  );
  @override
  late final GeneratedColumn<String> testToolVersion = GeneratedColumn<String>(
    'test_tool_version',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _securityPatchMeta = const VerificationMeta(
    'securityPatch',
  );
  @override
  late final GeneratedColumn<String> securityPatch = GeneratedColumn<String>(
    'security_patch',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _sdkVersionMeta = const VerificationMeta(
    'sdkVersion',
  );
  @override
  late final GeneratedColumn<String> sdkVersion = GeneratedColumn<String>(
    'sdk_version',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  @override
  List<GeneratedColumn> get $columns => [
    id,
    testDate,
    abi,
    module,
    testName,
    result,
    detail,
    description,
    fwVersion,
    testToolVersion,
    securityPatch,
    sdkVersion,
  ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'test_results';
  @override
  VerificationContext validateIntegrity(
    Insertable<TestResult> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('test_date')) {
      context.handle(
        _testDateMeta,
        testDate.isAcceptableOrUnknown(data['test_date']!, _testDateMeta),
      );
    } else if (isInserting) {
      context.missing(_testDateMeta);
    }
    if (data.containsKey('abi')) {
      context.handle(
        _abiMeta,
        abi.isAcceptableOrUnknown(data['abi']!, _abiMeta),
      );
    } else if (isInserting) {
      context.missing(_abiMeta);
    }
    if (data.containsKey('module')) {
      context.handle(
        _moduleMeta,
        module.isAcceptableOrUnknown(data['module']!, _moduleMeta),
      );
    } else if (isInserting) {
      context.missing(_moduleMeta);
    }
    if (data.containsKey('test_name')) {
      context.handle(
        _testNameMeta,
        testName.isAcceptableOrUnknown(data['test_name']!, _testNameMeta),
      );
    } else if (isInserting) {
      context.missing(_testNameMeta);
    }
    if (data.containsKey('result')) {
      context.handle(
        _resultMeta,
        result.isAcceptableOrUnknown(data['result']!, _resultMeta),
      );
    } else if (isInserting) {
      context.missing(_resultMeta);
    }
    if (data.containsKey('detail')) {
      context.handle(
        _detailMeta,
        detail.isAcceptableOrUnknown(data['detail']!, _detailMeta),
      );
    }
    if (data.containsKey('description')) {
      context.handle(
        _descriptionMeta,
        description.isAcceptableOrUnknown(
          data['description']!,
          _descriptionMeta,
        ),
      );
    }
    if (data.containsKey('fw_version')) {
      context.handle(
        _fwVersionMeta,
        fwVersion.isAcceptableOrUnknown(data['fw_version']!, _fwVersionMeta),
      );
    } else if (isInserting) {
      context.missing(_fwVersionMeta);
    }
    if (data.containsKey('test_tool_version')) {
      context.handle(
        _testToolVersionMeta,
        testToolVersion.isAcceptableOrUnknown(
          data['test_tool_version']!,
          _testToolVersionMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_testToolVersionMeta);
    }
    if (data.containsKey('security_patch')) {
      context.handle(
        _securityPatchMeta,
        securityPatch.isAcceptableOrUnknown(
          data['security_patch']!,
          _securityPatchMeta,
        ),
      );
    } else if (isInserting) {
      context.missing(_securityPatchMeta);
    }
    if (data.containsKey('sdk_version')) {
      context.handle(
        _sdkVersionMeta,
        sdkVersion.isAcceptableOrUnknown(data['sdk_version']!, _sdkVersionMeta),
      );
    } else if (isInserting) {
      context.missing(_sdkVersionMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  TestResult map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return TestResult(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}id'],
      )!,
      testDate: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}test_date'],
      )!,
      abi: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}abi'],
      )!,
      module: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}module'],
      )!,
      testName: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}test_name'],
      )!,
      result: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}result'],
      )!,
      detail: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}detail'],
      ),
      description: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}description'],
      ),
      fwVersion: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}fw_version'],
      )!,
      testToolVersion: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}test_tool_version'],
      )!,
      securityPatch: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}security_patch'],
      )!,
      sdkVersion: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}sdk_version'],
      )!,
    );
  }

  @override
  $TestResultsTable createAlias(String alias) {
    return $TestResultsTable(attachedDatabase, alias);
  }
}

class TestResult extends DataClass implements Insertable<TestResult> {
  final int id;
  final String testDate;
  final String abi;
  final String module;
  final String testName;
  final String result;
  final String? detail;
  final String? description;
  final String fwVersion;
  final String testToolVersion;
  final String securityPatch;
  final String sdkVersion;
  const TestResult({
    required this.id,
    required this.testDate,
    required this.abi,
    required this.module,
    required this.testName,
    required this.result,
    this.detail,
    this.description,
    required this.fwVersion,
    required this.testToolVersion,
    required this.securityPatch,
    required this.sdkVersion,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['test_date'] = Variable<String>(testDate);
    map['abi'] = Variable<String>(abi);
    map['module'] = Variable<String>(module);
    map['test_name'] = Variable<String>(testName);
    map['result'] = Variable<String>(result);
    if (!nullToAbsent || detail != null) {
      map['detail'] = Variable<String>(detail);
    }
    if (!nullToAbsent || description != null) {
      map['description'] = Variable<String>(description);
    }
    map['fw_version'] = Variable<String>(fwVersion);
    map['test_tool_version'] = Variable<String>(testToolVersion);
    map['security_patch'] = Variable<String>(securityPatch);
    map['sdk_version'] = Variable<String>(sdkVersion);
    return map;
  }

  TestResultsCompanion toCompanion(bool nullToAbsent) {
    return TestResultsCompanion(
      id: Value(id),
      testDate: Value(testDate),
      abi: Value(abi),
      module: Value(module),
      testName: Value(testName),
      result: Value(result),
      detail: detail == null && nullToAbsent
          ? const Value.absent()
          : Value(detail),
      description: description == null && nullToAbsent
          ? const Value.absent()
          : Value(description),
      fwVersion: Value(fwVersion),
      testToolVersion: Value(testToolVersion),
      securityPatch: Value(securityPatch),
      sdkVersion: Value(sdkVersion),
    );
  }

  factory TestResult.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return TestResult(
      id: serializer.fromJson<int>(json['id']),
      testDate: serializer.fromJson<String>(json['testDate']),
      abi: serializer.fromJson<String>(json['abi']),
      module: serializer.fromJson<String>(json['module']),
      testName: serializer.fromJson<String>(json['testName']),
      result: serializer.fromJson<String>(json['result']),
      detail: serializer.fromJson<String?>(json['detail']),
      description: serializer.fromJson<String?>(json['description']),
      fwVersion: serializer.fromJson<String>(json['fwVersion']),
      testToolVersion: serializer.fromJson<String>(json['testToolVersion']),
      securityPatch: serializer.fromJson<String>(json['securityPatch']),
      sdkVersion: serializer.fromJson<String>(json['sdkVersion']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'testDate': serializer.toJson<String>(testDate),
      'abi': serializer.toJson<String>(abi),
      'module': serializer.toJson<String>(module),
      'testName': serializer.toJson<String>(testName),
      'result': serializer.toJson<String>(result),
      'detail': serializer.toJson<String?>(detail),
      'description': serializer.toJson<String?>(description),
      'fwVersion': serializer.toJson<String>(fwVersion),
      'testToolVersion': serializer.toJson<String>(testToolVersion),
      'securityPatch': serializer.toJson<String>(securityPatch),
      'sdkVersion': serializer.toJson<String>(sdkVersion),
    };
  }

  TestResult copyWith({
    int? id,
    String? testDate,
    String? abi,
    String? module,
    String? testName,
    String? result,
    Value<String?> detail = const Value.absent(),
    Value<String?> description = const Value.absent(),
    String? fwVersion,
    String? testToolVersion,
    String? securityPatch,
    String? sdkVersion,
  }) => TestResult(
    id: id ?? this.id,
    testDate: testDate ?? this.testDate,
    abi: abi ?? this.abi,
    module: module ?? this.module,
    testName: testName ?? this.testName,
    result: result ?? this.result,
    detail: detail.present ? detail.value : this.detail,
    description: description.present ? description.value : this.description,
    fwVersion: fwVersion ?? this.fwVersion,
    testToolVersion: testToolVersion ?? this.testToolVersion,
    securityPatch: securityPatch ?? this.securityPatch,
    sdkVersion: sdkVersion ?? this.sdkVersion,
  );
  TestResult copyWithCompanion(TestResultsCompanion data) {
    return TestResult(
      id: data.id.present ? data.id.value : this.id,
      testDate: data.testDate.present ? data.testDate.value : this.testDate,
      abi: data.abi.present ? data.abi.value : this.abi,
      module: data.module.present ? data.module.value : this.module,
      testName: data.testName.present ? data.testName.value : this.testName,
      result: data.result.present ? data.result.value : this.result,
      detail: data.detail.present ? data.detail.value : this.detail,
      description: data.description.present
          ? data.description.value
          : this.description,
      fwVersion: data.fwVersion.present ? data.fwVersion.value : this.fwVersion,
      testToolVersion: data.testToolVersion.present
          ? data.testToolVersion.value
          : this.testToolVersion,
      securityPatch: data.securityPatch.present
          ? data.securityPatch.value
          : this.securityPatch,
      sdkVersion: data.sdkVersion.present
          ? data.sdkVersion.value
          : this.sdkVersion,
    );
  }

  @override
  String toString() {
    return (StringBuffer('TestResult(')
          ..write('id: $id, ')
          ..write('testDate: $testDate, ')
          ..write('abi: $abi, ')
          ..write('module: $module, ')
          ..write('testName: $testName, ')
          ..write('result: $result, ')
          ..write('detail: $detail, ')
          ..write('description: $description, ')
          ..write('fwVersion: $fwVersion, ')
          ..write('testToolVersion: $testToolVersion, ')
          ..write('securityPatch: $securityPatch, ')
          ..write('sdkVersion: $sdkVersion')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
    id,
    testDate,
    abi,
    module,
    testName,
    result,
    detail,
    description,
    fwVersion,
    testToolVersion,
    securityPatch,
    sdkVersion,
  );
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is TestResult &&
          other.id == this.id &&
          other.testDate == this.testDate &&
          other.abi == this.abi &&
          other.module == this.module &&
          other.testName == this.testName &&
          other.result == this.result &&
          other.detail == this.detail &&
          other.description == this.description &&
          other.fwVersion == this.fwVersion &&
          other.testToolVersion == this.testToolVersion &&
          other.securityPatch == this.securityPatch &&
          other.sdkVersion == this.sdkVersion);
}

class TestResultsCompanion extends UpdateCompanion<TestResult> {
  final Value<int> id;
  final Value<String> testDate;
  final Value<String> abi;
  final Value<String> module;
  final Value<String> testName;
  final Value<String> result;
  final Value<String?> detail;
  final Value<String?> description;
  final Value<String> fwVersion;
  final Value<String> testToolVersion;
  final Value<String> securityPatch;
  final Value<String> sdkVersion;
  const TestResultsCompanion({
    this.id = const Value.absent(),
    this.testDate = const Value.absent(),
    this.abi = const Value.absent(),
    this.module = const Value.absent(),
    this.testName = const Value.absent(),
    this.result = const Value.absent(),
    this.detail = const Value.absent(),
    this.description = const Value.absent(),
    this.fwVersion = const Value.absent(),
    this.testToolVersion = const Value.absent(),
    this.securityPatch = const Value.absent(),
    this.sdkVersion = const Value.absent(),
  });
  TestResultsCompanion.insert({
    this.id = const Value.absent(),
    required String testDate,
    required String abi,
    required String module,
    required String testName,
    required String result,
    this.detail = const Value.absent(),
    this.description = const Value.absent(),
    required String fwVersion,
    required String testToolVersion,
    required String securityPatch,
    required String sdkVersion,
  }) : testDate = Value(testDate),
       abi = Value(abi),
       module = Value(module),
       testName = Value(testName),
       result = Value(result),
       fwVersion = Value(fwVersion),
       testToolVersion = Value(testToolVersion),
       securityPatch = Value(securityPatch),
       sdkVersion = Value(sdkVersion);
  static Insertable<TestResult> custom({
    Expression<int>? id,
    Expression<String>? testDate,
    Expression<String>? abi,
    Expression<String>? module,
    Expression<String>? testName,
    Expression<String>? result,
    Expression<String>? detail,
    Expression<String>? description,
    Expression<String>? fwVersion,
    Expression<String>? testToolVersion,
    Expression<String>? securityPatch,
    Expression<String>? sdkVersion,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (testDate != null) 'test_date': testDate,
      if (abi != null) 'abi': abi,
      if (module != null) 'module': module,
      if (testName != null) 'test_name': testName,
      if (result != null) 'result': result,
      if (detail != null) 'detail': detail,
      if (description != null) 'description': description,
      if (fwVersion != null) 'fw_version': fwVersion,
      if (testToolVersion != null) 'test_tool_version': testToolVersion,
      if (securityPatch != null) 'security_patch': securityPatch,
      if (sdkVersion != null) 'sdk_version': sdkVersion,
    });
  }

  TestResultsCompanion copyWith({
    Value<int>? id,
    Value<String>? testDate,
    Value<String>? abi,
    Value<String>? module,
    Value<String>? testName,
    Value<String>? result,
    Value<String?>? detail,
    Value<String?>? description,
    Value<String>? fwVersion,
    Value<String>? testToolVersion,
    Value<String>? securityPatch,
    Value<String>? sdkVersion,
  }) {
    return TestResultsCompanion(
      id: id ?? this.id,
      testDate: testDate ?? this.testDate,
      abi: abi ?? this.abi,
      module: module ?? this.module,
      testName: testName ?? this.testName,
      result: result ?? this.result,
      detail: detail ?? this.detail,
      description: description ?? this.description,
      fwVersion: fwVersion ?? this.fwVersion,
      testToolVersion: testToolVersion ?? this.testToolVersion,
      securityPatch: securityPatch ?? this.securityPatch,
      sdkVersion: sdkVersion ?? this.sdkVersion,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (testDate.present) {
      map['test_date'] = Variable<String>(testDate.value);
    }
    if (abi.present) {
      map['abi'] = Variable<String>(abi.value);
    }
    if (module.present) {
      map['module'] = Variable<String>(module.value);
    }
    if (testName.present) {
      map['test_name'] = Variable<String>(testName.value);
    }
    if (result.present) {
      map['result'] = Variable<String>(result.value);
    }
    if (detail.present) {
      map['detail'] = Variable<String>(detail.value);
    }
    if (description.present) {
      map['description'] = Variable<String>(description.value);
    }
    if (fwVersion.present) {
      map['fw_version'] = Variable<String>(fwVersion.value);
    }
    if (testToolVersion.present) {
      map['test_tool_version'] = Variable<String>(testToolVersion.value);
    }
    if (securityPatch.present) {
      map['security_patch'] = Variable<String>(securityPatch.value);
    }
    if (sdkVersion.present) {
      map['sdk_version'] = Variable<String>(sdkVersion.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('TestResultsCompanion(')
          ..write('id: $id, ')
          ..write('testDate: $testDate, ')
          ..write('abi: $abi, ')
          ..write('module: $module, ')
          ..write('testName: $testName, ')
          ..write('result: $result, ')
          ..write('detail: $detail, ')
          ..write('description: $description, ')
          ..write('fwVersion: $fwVersion, ')
          ..write('testToolVersion: $testToolVersion, ')
          ..write('securityPatch: $securityPatch, ')
          ..write('sdkVersion: $sdkVersion')
          ..write(')'))
        .toString();
  }
}

abstract class _$MyDatabase extends GeneratedDatabase {
  _$MyDatabase(QueryExecutor e) : super(e);
  $MyDatabaseManager get managers => $MyDatabaseManager(this);
  late final $TestResultsTable testResults = $TestResultsTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [testResults];
}

typedef $$TestResultsTableCreateCompanionBuilder =
    TestResultsCompanion Function({
      Value<int> id,
      required String testDate,
      required String abi,
      required String module,
      required String testName,
      required String result,
      Value<String?> detail,
      Value<String?> description,
      required String fwVersion,
      required String testToolVersion,
      required String securityPatch,
      required String sdkVersion,
    });
typedef $$TestResultsTableUpdateCompanionBuilder =
    TestResultsCompanion Function({
      Value<int> id,
      Value<String> testDate,
      Value<String> abi,
      Value<String> module,
      Value<String> testName,
      Value<String> result,
      Value<String?> detail,
      Value<String?> description,
      Value<String> fwVersion,
      Value<String> testToolVersion,
      Value<String> securityPatch,
      Value<String> sdkVersion,
    });

class $$TestResultsTableFilterComposer
    extends Composer<_$MyDatabase, $TestResultsTable> {
  $$TestResultsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get testDate => $composableBuilder(
    column: $table.testDate,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get abi => $composableBuilder(
    column: $table.abi,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get module => $composableBuilder(
    column: $table.module,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get testName => $composableBuilder(
    column: $table.testName,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get result => $composableBuilder(
    column: $table.result,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get detail => $composableBuilder(
    column: $table.detail,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get description => $composableBuilder(
    column: $table.description,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get fwVersion => $composableBuilder(
    column: $table.fwVersion,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get testToolVersion => $composableBuilder(
    column: $table.testToolVersion,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get securityPatch => $composableBuilder(
    column: $table.securityPatch,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get sdkVersion => $composableBuilder(
    column: $table.sdkVersion,
    builder: (column) => ColumnFilters(column),
  );
}

class $$TestResultsTableOrderingComposer
    extends Composer<_$MyDatabase, $TestResultsTable> {
  $$TestResultsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get testDate => $composableBuilder(
    column: $table.testDate,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get abi => $composableBuilder(
    column: $table.abi,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get module => $composableBuilder(
    column: $table.module,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get testName => $composableBuilder(
    column: $table.testName,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get result => $composableBuilder(
    column: $table.result,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get detail => $composableBuilder(
    column: $table.detail,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get description => $composableBuilder(
    column: $table.description,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get fwVersion => $composableBuilder(
    column: $table.fwVersion,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get testToolVersion => $composableBuilder(
    column: $table.testToolVersion,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get securityPatch => $composableBuilder(
    column: $table.securityPatch,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get sdkVersion => $composableBuilder(
    column: $table.sdkVersion,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$TestResultsTableAnnotationComposer
    extends Composer<_$MyDatabase, $TestResultsTable> {
  $$TestResultsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get testDate =>
      $composableBuilder(column: $table.testDate, builder: (column) => column);

  GeneratedColumn<String> get abi =>
      $composableBuilder(column: $table.abi, builder: (column) => column);

  GeneratedColumn<String> get module =>
      $composableBuilder(column: $table.module, builder: (column) => column);

  GeneratedColumn<String> get testName =>
      $composableBuilder(column: $table.testName, builder: (column) => column);

  GeneratedColumn<String> get result =>
      $composableBuilder(column: $table.result, builder: (column) => column);

  GeneratedColumn<String> get detail =>
      $composableBuilder(column: $table.detail, builder: (column) => column);

  GeneratedColumn<String> get description => $composableBuilder(
    column: $table.description,
    builder: (column) => column,
  );

  GeneratedColumn<String> get fwVersion =>
      $composableBuilder(column: $table.fwVersion, builder: (column) => column);

  GeneratedColumn<String> get testToolVersion => $composableBuilder(
    column: $table.testToolVersion,
    builder: (column) => column,
  );

  GeneratedColumn<String> get securityPatch => $composableBuilder(
    column: $table.securityPatch,
    builder: (column) => column,
  );

  GeneratedColumn<String> get sdkVersion => $composableBuilder(
    column: $table.sdkVersion,
    builder: (column) => column,
  );
}

class $$TestResultsTableTableManager
    extends
        RootTableManager<
          _$MyDatabase,
          $TestResultsTable,
          TestResult,
          $$TestResultsTableFilterComposer,
          $$TestResultsTableOrderingComposer,
          $$TestResultsTableAnnotationComposer,
          $$TestResultsTableCreateCompanionBuilder,
          $$TestResultsTableUpdateCompanionBuilder,
          (
            TestResult,
            BaseReferences<_$MyDatabase, $TestResultsTable, TestResult>,
          ),
          TestResult,
          PrefetchHooks Function()
        > {
  $$TestResultsTableTableManager(_$MyDatabase db, $TestResultsTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$TestResultsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$TestResultsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$TestResultsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<String> testDate = const Value.absent(),
                Value<String> abi = const Value.absent(),
                Value<String> module = const Value.absent(),
                Value<String> testName = const Value.absent(),
                Value<String> result = const Value.absent(),
                Value<String?> detail = const Value.absent(),
                Value<String?> description = const Value.absent(),
                Value<String> fwVersion = const Value.absent(),
                Value<String> testToolVersion = const Value.absent(),
                Value<String> securityPatch = const Value.absent(),
                Value<String> sdkVersion = const Value.absent(),
              }) => TestResultsCompanion(
                id: id,
                testDate: testDate,
                abi: abi,
                module: module,
                testName: testName,
                result: result,
                detail: detail,
                description: description,
                fwVersion: fwVersion,
                testToolVersion: testToolVersion,
                securityPatch: securityPatch,
                sdkVersion: sdkVersion,
              ),
          createCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                required String testDate,
                required String abi,
                required String module,
                required String testName,
                required String result,
                Value<String?> detail = const Value.absent(),
                Value<String?> description = const Value.absent(),
                required String fwVersion,
                required String testToolVersion,
                required String securityPatch,
                required String sdkVersion,
              }) => TestResultsCompanion.insert(
                id: id,
                testDate: testDate,
                abi: abi,
                module: module,
                testName: testName,
                result: result,
                detail: detail,
                description: description,
                fwVersion: fwVersion,
                testToolVersion: testToolVersion,
                securityPatch: securityPatch,
                sdkVersion: sdkVersion,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$TestResultsTableProcessedTableManager =
    ProcessedTableManager<
      _$MyDatabase,
      $TestResultsTable,
      TestResult,
      $$TestResultsTableFilterComposer,
      $$TestResultsTableOrderingComposer,
      $$TestResultsTableAnnotationComposer,
      $$TestResultsTableCreateCompanionBuilder,
      $$TestResultsTableUpdateCompanionBuilder,
      (TestResult, BaseReferences<_$MyDatabase, $TestResultsTable, TestResult>),
      TestResult,
      PrefetchHooks Function()
    >;

class $MyDatabaseManager {
  final _$MyDatabase _db;
  $MyDatabaseManager(this._db);
  $$TestResultsTableTableManager get testResults =>
      $$TestResultsTableTableManager(_db, _db.testResults);
}
