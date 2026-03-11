class XtsTfFailureEntry {
  const XtsTfFailureEntry({
    required this.deviceSerial,
    required this.className,
    required this.classSimpleName,
    required this.testName,
    required this.status,
    required this.failureHeadline,
    required this.logSnippet,
  });

  final String deviceSerial;
  final String className;
  final String classSimpleName;
  final String testName;
  final String status;
  final String failureHeadline;
  final String logSnippet;

  String get key => '$className#$testName';
}

class XtsTfOutputParseResult {
  const XtsTfOutputParseResult({
    required this.failures,
    this.totalTests,
    this.passCount,
    this.failCount,
    this.ignoredCount,
    this.assumptionFailureCount,
  });

  final int? totalTests;
  final int? passCount;
  final int? failCount;
  final int? ignoredCount;
  final int? assumptionFailureCount;
  final List<XtsTfFailureEntry> failures;

  bool get hasSummary {
    return totalTests != null ||
        passCount != null ||
        failCount != null ||
        ignoredCount != null ||
        assumptionFailureCount != null;
  }
}

class XtsTfOutputParser {
  XtsTfOutputParseResult parseText(String source) {
    final normalized = source.replaceAll('\r', '');
    final lines = normalized.split('\n');
    final failures = <XtsTfFailureEntry>[];

    final summary = _parseSummary(normalized);
    var index = 0;
    while (index < lines.length) {
      final line = lines[index];
      final match = _failureHeader.firstMatch(line);
      if (match == null) {
        index += 1;
        continue;
      }

      final deviceSerial = match.group(1) ?? '';
      final className = match.group(2) ?? '';
      final testName = match.group(3) ?? '';
      final status = match.group(4) ?? '';
      final headline = (match.group(5) ?? '').trim();
      final stackLines = <String>[];
      if (status == 'FAILURE') {
        var cursor = index + 1;
        while (cursor < lines.length) {
          final nextLine = lines[cursor];
          if (_failureHeader.hasMatch(nextLine) ||
              _timestampLine.hasMatch(nextLine) ||
              _summaryLine.hasMatch(nextLine) ||
              nextLine.trim().startsWith('============== End of Results')) {
            break;
          }
          if (nextLine.trim().isNotEmpty) {
            stackLines.add(nextLine.trimRight());
          }
          cursor += 1;
        }
        index = cursor;
      } else {
        index += 1;
      }

      failures.add(
        XtsTfFailureEntry(
          deviceSerial: deviceSerial,
          className: className,
          classSimpleName: _classSimpleName(className),
          testName: testName,
          status: status,
          failureHeadline: headline,
          logSnippet: _composeSnippet(headline, stackLines),
        ),
      );
    }

    return XtsTfOutputParseResult(
      totalTests: summary.$1,
      passCount: summary.$2,
      failCount: summary.$3,
      ignoredCount: summary.$4,
      assumptionFailureCount: summary.$5,
      failures: failures,
    );
  }

  (int?, int?, int?, int?, int?) _parseSummary(String source) {
    int? read(String label) {
      final match = RegExp(
        '^$label\\s*:\\s*(\\d+)\$',
        multiLine: true,
      ).firstMatch(source);
      return int.tryParse(match?.group(1) ?? '');
    }

    return (
      read('Total Tests'),
      read('PASSED'),
      read('FAILED'),
      read('IGNORED'),
      read('ASSUMPTION_FAILURE'),
    );
  }

  String _classSimpleName(String className) {
    if (className.isEmpty) {
      return '';
    }
    final tokens = className.split('.');
    return tokens.isEmpty ? className : tokens.last;
  }

  String _composeSnippet(String headline, List<String> stackLines) {
    if (headline.isEmpty && stackLines.isEmpty) {
      return '';
    }
    final snippetLines = <String>[];
    if (headline.isNotEmpty) {
      snippetLines.add('FAILURE: $headline');
    }
    snippetLines.addAll(stackLines.take(6));
    return snippetLines.join('\n').trim();
  }

  static final RegExp _failureHeader = RegExp(
    r'^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\s+\S+/ModuleListener:\s+\[\d+/\d+\]\s+(\S+)\s+.*?([A-Za-z0-9_$.]+)#([A-Za-z0-9_$.]+)\s+(FAILURE|ASSUMPTION_FAILURE|IGNORED):\s*(.*)$',
  );
  static final RegExp _timestampLine = RegExp(
    r'^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\s+',
  );
  static final RegExp _summaryLine = RegExp(
    r'^(Total Tests|PASSED|FAILED|IGNORED|ASSUMPTION_FAILURE)\s*:',
  );
}
