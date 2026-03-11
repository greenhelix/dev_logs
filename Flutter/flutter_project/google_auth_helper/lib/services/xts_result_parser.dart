import 'package:xml/xml.dart';

import '../models/failed_test_record.dart';
import '../models/live_status.dart';
import '../models/test_case_record.dart';
import '../models/test_metric_record.dart';
import 'xts_tf_output_parser.dart';

class XtsParsedResult {
  const XtsParsedResult({
    required this.metric,
    required this.testCases,
    required this.failedTests,
    required this.warnings,
  });

  final TestMetricRecord metric;
  final List<TestCaseRecord> testCases;
  final List<FailedTestRecord> failedTests;
  final List<String> warnings;
}

class XtsResultParser {
  XtsParsedResult parseText(
    String xmlSource, {
    LiveStatus? liveStatus,
    XtsTfOutputParseResult? tfOutput,
  }) {
    final document = XmlDocument.parse(xmlSource);
    final resultNode = document.findElements('Result').first;
    final buildNode = resultNode.findElements('Build').firstOrNull;
    final summaryNode = resultNode.findElements('Summary').firstOrNull;
    final warnings = <String>[];
    final startedAt = DateTime.fromMillisecondsSinceEpoch(
      int.tryParse(resultNode.getAttribute('start') ?? '') ?? 0,
      isUtc: true,
    );
    final endedAt = DateTime.fromMillisecondsSinceEpoch(
      int.tryParse(resultNode.getAttribute('end') ?? '') ?? 0,
      isUtc: true,
    );
    final suiteName = resultNode.getAttribute('suite_name') ?? '';
    final suiteVersion = resultNode.getAttribute('suite_version') ?? '';
    final devices = (resultNode.getAttribute('devices') ?? '')
        .split(',')
        .map((item) => item.trim())
        .where((item) => item.isNotEmpty)
        .toList(growable: false);

    final xmlPassCount =
        int.tryParse(summaryNode?.getAttribute('pass') ?? '') ?? 0;
    final xmlFailCount =
        int.tryParse(summaryNode?.getAttribute('failed') ?? '') ?? 0;
    final xmlWarningCount =
        int.tryParse(summaryNode?.getAttribute('warning') ?? '') ?? 0;
    var xmlIgnoredCount = 0;
    var xmlAssumptionFailureCount = 0;

    final testCases = <TestCaseRecord>[];
    final failuresByKey = <String, FailedTestRecord>{};

    for (final moduleNode in resultNode.findElements('Module')) {
      final suiteModuleName = moduleNode.getAttribute('name') ?? '';
      for (final testCaseNode in moduleNode.findElements('TestCase')) {
        final className = testCaseNode.getAttribute('name') ?? '';
        final classSimpleName = _classSimpleName(className);
        final testNodes =
            testCaseNode.findElements('Test').toList(growable: false);
        testCases.add(
          TestCaseRecord(
            id: _sanitizeId('$suiteModuleName::$className'),
            moduleName: suiteModuleName,
            testCaseName: className,
            testMethodCount: testNodes.length,
            suiteName: suiteName,
            suiteVersion: suiteVersion,
            description: classSimpleName,
          ),
        );

        for (final testNode in testNodes) {
          final result = (testNode.getAttribute('result') ?? '').toUpperCase();
          if (result == 'IGNORED') {
            xmlIgnoredCount += 1;
          }
          if (result == 'ASSUMPTION_FAILURE') {
            xmlAssumptionFailureCount += 1;
          }
          if (result != 'FAIL') {
            continue;
          }
          final testName = testNode.getAttribute('name') ?? '';
          final failureNode = testNode.findElements('Failure').firstOrNull;
          final message = failureNode?.getAttribute('message') ?? '';
          final stackTrace =
              failureNode?.findElements('StackTrace').firstOrNull?.innerText ??
                  '';
          final key = '$className#$testName';
          failuresByKey[key] = FailedTestRecord(
            id: _sanitizeId('$suiteModuleName::$className::$testName'),
            moduleName: classSimpleName,
            testCaseName: className,
            testName: testName,
            failureMessage: message,
            errorLogSnippet: stackTrace.trim(),
            timestamp: startedAt,
            suiteVersion: suiteVersion,
            suiteModuleName: suiteModuleName,
            className: className,
            classSimpleName: classSimpleName,
          );
        }
      }
    }

    final tfFailures = {
      for (final item in tfOutput?.failures ?? const <XtsTfFailureEntry>[])
        item.key: item,
    };
    final failedTests = failuresByKey.entries.map((entry) {
      final xmlFailure = entry.value;
      final tfFailure = tfFailures[entry.key];
      if (tfFailure == null) {
        return xmlFailure.copyWith(
          status: 'FAILURE',
          errorLogSnippet: xmlFailure.errorLogSnippet.trim(),
        );
      }
      return xmlFailure.copyWith(
        moduleName: tfFailure.classSimpleName,
        failureMessage: tfFailure.failureHeadline.isEmpty
            ? xmlFailure.failureMessage
            : tfFailure.failureHeadline,
        errorLogSnippet: tfFailure.logSnippet.isEmpty
            ? xmlFailure.errorLogSnippet
            : tfFailure.logSnippet,
        deviceSerial: tfFailure.deviceSerial,
        status: tfFailure.status,
      );
    }).toList(growable: false);

    final unmatchedLogFailures = tfFailures.keys
        .where((key) => !failuresByKey.containsKey(key))
        .map((key) => tfFailures[key]!)
        .where((item) => item.status == 'FAILURE')
        .toList(growable: false);
    if (unmatchedLogFailures.isNotEmpty) {
      warnings.add(
        'Some failures were found in xts_tf_output.log but not in test_result.xml.',
      );
      failedTests.addAll(
        unmatchedLogFailures.map((item) {
          return FailedTestRecord(
            id: _sanitizeId('${item.className}::${item.testName}'),
            moduleName: item.classSimpleName,
            testCaseName: item.className,
            testName: item.testName,
            failureMessage: item.failureHeadline,
            errorLogSnippet: item.logSnippet,
            timestamp: startedAt,
            suiteVersion: suiteVersion,
            suiteModuleName: '',
            className: item.className,
            classSimpleName: item.classSimpleName,
            deviceSerial: item.deviceSerial,
            status: item.status,
          );
        }),
      );
    }

    final countSource =
        tfOutput != null && tfOutput.hasSummary ? 'xts_tf_output.log' : 'xml';
    final passCount = tfOutput?.passCount ?? xmlPassCount;
    final failCount = tfOutput?.failCount ?? xmlFailCount;
    final ignoredCount = tfOutput?.ignoredCount ?? xmlIgnoredCount;
    final assumptionFailureCount =
        tfOutput?.assumptionFailureCount ?? xmlAssumptionFailureCount;
    final totalTests = tfOutput?.totalTests ??
        (passCount + failCount + ignoredCount + assumptionFailureCount);

    if (tfOutput != null && tfOutput.hasSummary && xmlFailCount != 0) {
      if (tfOutput.failCount != null && tfOutput.failCount != xmlFailCount) {
        warnings.add(
          'xts_tf_output.log failed count differs from test_result.xml. Log summary is used.',
        );
      }
    }

    final buildFingerprint = buildNode?.getAttribute('build_fingerprint') ??
        buildNode?.getAttribute('system_img_info') ??
        '';
    final buildDevice = buildNode?.getAttribute('build_device') ??
        _buildFingerprintPart(buildFingerprint, 1);
    final androidVersion = buildNode?.getAttribute('build_version_release') ??
        _buildFingerprintVersion(buildFingerprint);
    final buildType = buildNode?.getAttribute('build_type') ??
        _buildFingerprintBuildType(buildFingerprint);
    final fwVersion = buildNode?.getAttribute('build_version_incremental') ??
        _buildFingerprintIncremental(buildFingerprint);

    final metric = TestMetricRecord(
      id: _sanitizeId('${suiteVersion}_${startedAt.toIso8601String()}'),
      toolType: _deriveToolType(suiteName),
      suiteName: suiteName,
      suiteVersion: suiteVersion,
      fwVersion: fwVersion,
      totalTests: totalTests,
      passCount: passCount,
      failCount: failCount,
      ignoredCount: ignoredCount,
      assumptionFailureCount: assumptionFailureCount,
      warningCount: xmlWarningCount,
      moduleCount: resultNode.findElements('Module').length,
      durationSeconds: endedAt.difference(startedAt).inSeconds,
      devices: devices.isEmpty ? (liveStatus?.devices ?? const []) : devices,
      timestamp: startedAt,
      buildDevice: buildDevice,
      androidVersion: androidVersion,
      buildType: buildType,
      buildFingerprint: buildFingerprint,
      countSource: countSource,
    );

    return XtsParsedResult(
      metric: metric,
      testCases: testCases,
      failedTests: failedTests,
      warnings: warnings,
    );
  }

  String _sanitizeId(String value) {
    return value.replaceAll(RegExp(r'[^a-zA-Z0-9._-]+'), '_');
  }

  String _deriveToolType(String suiteName) {
    final upper = suiteName.toUpperCase();
    if (upper.contains('TVTS')) {
      return 'TVTS';
    }
    if (upper.contains('GTS')) {
      return 'GTS';
    }
    if (upper.contains('VTS')) {
      return 'VTS';
    }
    if (upper.contains('STS')) {
      return 'STS';
    }
    if (upper.contains('CTS')) {
      return 'CTS';
    }
    return 'UNKNOWN';
  }

  String _classSimpleName(String className) {
    if (className.isEmpty) {
      return '';
    }
    final tokens = className.split('.');
    return tokens.isEmpty ? className : tokens.last;
  }

  String _buildFingerprintVersion(String fingerprint) {
    if (fingerprint.isEmpty) {
      return '';
    }
    final tokens = fingerprint.split(':');
    if (tokens.length < 2) {
      return '';
    }
    final versionToken = tokens[1].split('/').first;
    return versionToken.trim();
  }

  String _buildFingerprintPart(String fingerprint, int index) {
    if (fingerprint.isEmpty) {
      return '';
    }
    final tokens = fingerprint.split('/');
    if (index < 0 || index >= tokens.length) {
      return '';
    }
    final value = tokens[index];
    if (index == 4) {
      final parts = value.split(':');
      return parts.length > 1 ? parts[1] : value;
    }
    return value;
  }

  String _buildFingerprintIncremental(String fingerprint) {
    final token = _buildFingerprintPart(fingerprint, 4);
    if (token.isEmpty) {
      return '';
    }
    final parts = token.split(':');
    return parts.first;
  }

  String _buildFingerprintBuildType(String fingerprint) {
    final token = _buildFingerprintPart(fingerprint, 4);
    if (token.isEmpty) {
      return '';
    }
    final parts = token.split(':');
    return parts.length > 1 ? parts[1] : '';
  }
}

extension on Iterable<XmlElement> {
  XmlElement? get firstOrNull => isEmpty ? null : first;
}
