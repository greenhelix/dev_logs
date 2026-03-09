========================================================================================
[1. 로컬 환경 (Ubuntu / Windows PC)]
----------------------------------------------------------------------------------------
- 핵심 로직: Flutter Desktop 앱 (Dart/Flutter)
  * 로컬 파일 I/O: 'dart:io'를 통한 로그 파일 접근 및 실시간 파싱
  * 프로세스 제어: 'Process.run'을 통한 Ubuntu 터미널 명령어 실행
  * 데이터 가공: 파싱된 로그를 Firestore 저장용 JSON 포맷으로 직렬화
  * 자동 업데이트: 'auto_updater'를 통해 GitHub Release 버전 체크 및 자동 적용
  * 설치 환경 점검: adb, 테스트 툴, Redmine, Python 버전 등 환경 구성 필수 항목 점검
========================================================================================

========================================================================================
[2. 클라우드 중계 및 저장 (Firebase)]
----------------------------------------------------------------------------------------
- Firestore (NoSQL): 
  * 데이터 모델: 로그(timestamp, log_level, content, category) 단위의 문서 저장
  * 실시간 동기화: Flutter Web/Desktop 앱과 양방향 데이터 스트림 유지
- Hosting:
  * 웹 인터페이스: 빌드된 Flutter Web 결과물을 글로벌 배포
  * Firebase Project: kani-projects  
  * Firestore ID: google-auth
  * Firebase Hosting: https://kani-projects.web.app
========================================================================================

========================================================================================
[3. 배포 파이프라인 (CI/CD - GitHub Actions)]
----------------------------------------------------------------------------------------
- Source Repo: 소스 코드 관리 (Git Push 이벤트 감지)
- Build Process: GitHub Actions 가상머신에서 멀티 플랫폼 빌드 (Windows/Linux/Web)
- Release Repo: 빌드 결과물(.exe, .deb, Web Artifacts)을 버전별 릴리즈 배포
- Source Repo Name: greenhelix/dev_logs/Flutter/flutter_project/google_auth_helper
- Release Repo Name: greenhelix/GAH-Release-Repo
========================================================================================

========================================================================================
[4. 조회 환경 (Client)]
----------------------------------------------------------------------------------------
- 접속 경로: Mobile(Chrome), Windows PC, Ubuntu Desktop
- 시각화: 'fl_chart' 라이브러리를 사용한 통계 데이터 분석 및 대시보드 그래프 렌더링

[아키텍처 구성도]
graph TD
    subgraph "개발 및 배포(GitHub CI/CD)"
        Code[Source Code Repo] -->|Push| GHA[GitHub Actions]
        GHA -->|Build| BuildOutput[Release Repo]
    end

    subgraph "로컬 작업(Ubuntu/Windows PC)"
        App[Flutter Desktop App] -->|Auto-Update| BuildOutput
        App -->|File/Log Parsing| File[System Logs/Files]
        App -->|Write/Push| Firestore
    end

    subgraph "클라우드(Firebase)"
        Firestore[(Cloud Firestore)]
        Hosting[Firebase Hosting]
    end

    subgraph "조회(Mobile/Web/Desktop)"
        Client1[Mobile Chrome] -->|Fetch| Hosting
        Client2[Ubuntu/Win App] -->|Listen/Fetch| Firestore
        Hosting -->|Render| DataVis[Web Visualization/Charts]
    end

    Firestore -.->|Sync| DataVis
========================================================================================

========================================================================================
[5. 인증 테스트 도구 환경 설명]
----------------------------------------------------------------------------------------
1. 인증 테스트 툴 경로 (예시)
- CTS: /home/innopia/xts/cts/android-cts-14_r10-linux_x86-arm
- GTS: /home/innopia/xts/gts/android-gts-13.1-R1-13-16-14373446
- TVTS: /home/innopia/xts/tvts/android-tvts-2.16R2-arm
- VTS: /home/innopia/xts/vts/android11-vts-r16-arm
- STS: /home/innopia/xts/sts/android-sts-11_sts-r23-linux-arm
- CTS_ON_GSI: /home/innopia/xts/cts/android-cts-14_r10-linux_x86-arm

2. 툴 실행 및 동작 방식
각 테스트 툴은 `{TOOL_PATH}/tools/*-tradefed` (예: cts-tradefed) 형태로 실행되며, 
터미널에서 서버와 연동하여 로그를 출력하고 내부적으로 명령을 실행하여 테스트를 진행합니다.

- 실행 명령어 형태: `run cts`, `run gts`, `run sts` 등
- 멀티 기기 테스트: `-s {serial_num}` 옵션을 추가하여 다수의 ADB 디바이스 동시 테스트 가능
  (멀티 디바이스 실행 시 `--shard-count {adb_num}` 인자 사용)
  ex) `run cts --shard-count 2 -s serial_1 -s serial_2`

3. 테스트 결과 및 로그 디렉토리 구조
테스트가 종료되면 타임스탬프(또는 latest) 기반의 결과/로그 폴더가 생성됩니다.
ex) ~/xts/cts/android-cts-14_r10-linux_x86-arm/android-cts/results/2025.12.17_03.50.51.211_3616
ex) ~/xts/cts/android-cts-14_r10-linux_x86-arm/android-cts/logs/2025.12.17_03.50.51.211_3616

- 결과 폴더 (results): 
  핵심 분석 대상인 `test_result.html`, `test_result.xml` 파일을 중점적으로 파싱.
  (기타 파일 중 가치가 있는 데이터는 AI 연동을 통해 추가 분석/추출 고려)
- 로그 폴더 (logs): 
  `device_logcat_test*.txt`, `event-logs*.txt`, `host_adb_log*.txt`, `olc_server_session_log.txt`, `command_history.txt`, `xts_tf_output.log` 등을 취합.
  *주요 목적: Fail이 발생한 테스트의 에러 로그를 정확히 추출하여 DB화.*
========================================================================================

========================================================================================
[6. NoSQL(Firestore) DB 구성 및 데이터 수집 요건]
----------------------------------------------------------------------------------------
실시간 터미널 추적은 방대한 로그량을 고려하여, `olc_server_session_log` 또는 `command_history`의 정규 텍스트 패턴을 파싱해 테스트 상태(대기, 시작중, 진행, 종료)만 경량으로 인식합니다. 완료된 결과는 사용자가 직접 수동 업로드하거나, 시스템이 파일 경로를 참조해 HTML/XML을 파싱하여 자동 업로드할 수 있어야 합니다.

이를 기반으로 Firestore NoSQL 컬렉션을 아래 3가지 카테고리로 설계합니다.

1. 테스트 케이스 베이스 데이터 (Collection: TestCases)
   - 목적: Pass/Fail 여부와 관계없이 모든 테스트 모듈과 케이스의 메타데이터 축적
   - 구조: 'Module'을 상위 문서로 두고, 하위 컬렉션(또는 필드 배열)으로 'TestCase'를 구성
   - 내용: 각 테스트 케이스의 명칭과 상세 설명(Description) 포함
   - 확장: 설명이 없는 케이스는 AI(웹 검색 연동)를 통해 자동으로 설명을 요약해 채우며, AI로 찾을 수 없는 경우 사용자가 직접 입력/수정할 수 있도록 UI 제공

2. 실패 테스트 이력 및 해결 방법 (Collection: FailedTests)
   - 목적: Fail이 발생한 테스트 케이스의 로그 및 트러블슈팅 데이터베이스화
   - 구조: 모듈명과 테스트케이스명을 키(Key)로 삼아 해당 테스트의 Fail 로그 매핑 저장
   - 내용: 실패 원인 로그 단락, 디바이스 로그 발췌본 등
   - 핵심 기능: '해결 방법(Solution)' 필드 제공. 테스트 실패가 해결되었을 때 사용자가 직접 해결책을 기입하거나, 동일한 Fail을 겪은 다른 사용자들을 위해 일괄 해결 방법이 업데이트 및 공유될 수 있도록 구성

3. 테스트 세션 통계 및 수치 데이터 (Collection: TestMetrics)
   - 목적: 시각화(fl_chart) 및 통계 대시보드 구성을 위한 정량적 데이터 축적
   - 구조: 개별 세션(1회 테스트 런) 단위로 문서 생성
   - 내용: 
     * 실행 데이터: 총 테스트 개수, Pass/Fail 카운트, 소요 시간
     * 환경 데이터: 테스트 툴 버전(CTS 14_r10 등), 테스트된 기기의 FW 버전
     * 릴리즈 분류: 해당 펌웨어의 분기 태그 (LR, IR, MR, selfMR 등 구분을 위한 필수 필드)
   - 확장: 대시보드에서 주간/월간 실패율 추이, FW 버전별 안정성 비교 등 통계적으로 의미 있는 데이터를 AI 또는 백엔드 로직이 자동 추출/가공할 수 있도록 구조화
========================================================================================

개발 진행 체크리스트
 Phase 1: 요구사항 분석 및 Firestore 데이터 모델링 (현재 단계)

 Phase 2: Flutter 프로젝트 초기화 및 Firebase 환경 구성 (Windows/Linux/Web)

 Phase 3: Process.run 기반 터미널 테스트 실행 및 dart:io 로그 파싱 로직 구현

 Phase 4: 비즈니스 로직 및 Firestore CRUD Repository 구성

 Phase 5: 크로스 플랫폼 UI 레이어 및 fl_chart 기반 대시보드 시각화

 Phase 6: GitHub Actions CI/CD 파이프라인 구축 및 auto_updater 적용

Firestore 데이터 모델 구조화
NoSQL의 특성과 실시간 동기화 효율을 고려하여 요구사항에 명시된 3가지 주요 컬렉션 모델을 구성합니다.
​

TestCases: 전체 테스트 케이스의 메타데이터와 AI 요약 설명을 저장하여 베이스 데이터를 구축합니다.

FailedTests: 실패한 테스트의 에러 로그 발췌본과 트러블슈팅(해결 방법) 이력을 매핑하여 지식 기반을 형성합니다.

TestMetrics: fl_chart 시각화를 위한 세션별 정량적 통계(소요 시간, 펌웨어 버전, Pass/Fail 비율 등)를 저장합니다.

Dart 모델 코드
Firestore Document 연동을 위해 각 클래스에 fromJson 및 toJson 팩토리 메서드를 구현합니다. Firestore의 날짜 타입인 Timestamp를 Dart의 DateTime 객체로 안전하게 변환하는 직렬화 처리를 포함했습니다.
​

dart
import 'package:cloud_firestore/cloud_firestore.dart';

// 1. 테스트 케이스 베이스 데이터 모델 (TestCases)
class TestCase {
  final String id;
  final String moduleName;
  final String testName;
  final String description;

  TestCase({
    required this.id,
    required this.moduleName,
    required this.testName,
    this.description = '',
  });

  factory TestCase.fromJson(String id, Map<String, dynamic> json) {
    return TestCase(
      id: id,
      moduleName: json['moduleName'] ?? '',
      testName: json['testName'] ?? '',
      description: json['description'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'moduleName': moduleName,
      'testName': testName,
      'description': description,
    };
  }
}

// 2. 실패 테스트 이력 및 해결 방법 모델 (FailedTests)
class FailedTest {
  final String id;
  final String moduleName;
  final String testName;
  final String errorLogSnippet;
  final String solution;
  final DateTime timestamp;

  FailedTest({
    required this.id,
    required this.moduleName,
    required this.testName,
    required this.errorLogSnippet,
    this.solution = '',
    required this.timestamp,
  });

  factory FailedTest.fromJson(String id, Map<String, dynamic> json) {
    return FailedTest(
      id: id,
      moduleName: json['moduleName'] ?? '',
      testName: json['testName'] ?? '',
      errorLogSnippet: json['errorLogSnippet'] ?? '',
      solution: json['solution'] ?? '',
      timestamp: (json['timestamp'] as Timestamp).toDate(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'moduleName': moduleName,
      'testName': testName,
      'errorLogSnippet': errorLogSnippet,
      'solution': solution,
      'timestamp': Timestamp.fromDate(timestamp),
    };
  }
}

// 3. 테스트 세션 통계 및 수치 데이터 모델 (TestMetrics)
class TestMetric {
  final String id;
  final int totalTests;
  final int passCount;
  final int failCount;
  final int durationSeconds;
  final String toolVersion;
  final String fwVersion;
  final String releaseBranch;
  final DateTime timestamp;

  TestMetric({
    required this.id,
    required this.totalTests,
    required this.passCount,
    required this.failCount,
    required this.durationSeconds,
    required this.toolVersion,
    required this.fwVersion,
    required this.releaseBranch,
    required this.timestamp,
  });

  factory TestMetric.fromJson(String id, Map<String, dynamic> json) {
    return TestMetric(
      id: id,
      totalTests: json['totalTests'] ?? 0,
      passCount: json['passCount'] ?? 0,
      failCount: json['failCount'] ?? 0,
      durationSeconds: json['durationSeconds'] ?? 0,
      toolVersion: json['toolVersion'] ?? '',
      fwVersion: json['fwVersion'] ?? '',
      releaseBranch: json['releaseBranch'] ?? '',
      timestamp: (json['timestamp'] as Timestamp).toDate(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'totalTests': totalTests,
      'passCount': passCount,
      'failCount': failCount,
      'durationSeconds': durationSeconds,
      'toolVersion': toolVersion,
      'fwVersion': fwVersion,
      'releaseBranch': releaseBranch,
      'timestamp': Timestamp.fromDate(timestamp),
    };
  }
}
앞으로 구현할 UI 및 비즈니스 로직에 맞춰 사용할 상태 관리(State Management) 패키지는 Riverpod, GetX, Provider 중 어느 것을 선호하시나요?

Gemini 3.1 Pro Thinking(으)로 준비됨
riverpod 로 결정할께.

그리고 항상 파일 구조 설명하면서 코드 작성해줘 (대원칙)
선택해주신 Riverpod 상태 관리 패키지를 바탕으로, 애플리케이션의 아키텍처 파일 구조를 설계하고 요구사항의 핵심 부분인 터미널 실행 및 로그 파싱(Phase 3) 코드를 작성하겠습니다.

개발 진행 체크리스트
 Phase 1: 요구사항 분석 및 Firestore 데이터 모델링

 Phase 2: 앱 파일 구조 설계 및 Riverpod 기반 설정 (현재 단계)

 Phase 3: Process.run/Process.start 기반 터미널 테스트 실행 및 dart:io 로그 파싱 로직 구현 (현재 단계)

 Phase 4: 비즈니스 로직 및 Firestore CRUD Repository 구성

 Phase 5: 크로스 플랫폼 UI 레이어 및 fl_chart 기반 대시보드 시각화

 Phase 6: GitHub Actions CI/CD 파이프라인 구축 및 auto_updater 적용

📂 파일 구조 설명 (대원칙)
확장성과 유지보수성을 위해 Clean Architecture 개념을 일부 차용한 Feature-first(기능별) 또는 Layer-first(계층별) 구조를 사용하는 것이 좋습니다. 여기서는 소규모부터 대규모 앱까지 관리가 용이한 Layer-first + Domain 기반 구조로 설계했습니다.
​

text
lib/
├── main.dart
├── core/                       # 앱 전반에서 사용되는 공통 기능
│   ├── constants/              # 경로, 상수값 (예: XTS 툴 경로)
│   ├── utils/                  # 헬퍼 함수
│   └── theme/                  # UI 테마
├── models/                     # 데이터 모델 (Phase 1에서 작성한 TestCase, FailedTest 등)
├── repositories/               # 데이터 소스 (Firestore 연동, 파일 시스템 연동)
│   ├── firestore_repository.dart
│   └── local_file_repository.dart
├── services/                   # 핵심 비즈니스 로직 및 외부 시스템 제어
│   ├── log_parser_service.dart # 로그 분석 및 상태 분류 로직
│   └── terminal_service.dart   # Process 제어 (run cts 등)
├── providers/                  # Riverpod 상태 관리 프로바이더
│   ├── xts_runner_provider.dart  # 테스트 실행 상태 및 로그 스트림 관리
│   └── database_provider.dart    # Firestore 데이터 상태 관리
└── ui/                         # UI 계층
    ├── screens/                # 주요 화면 (대시보드, 테스트 실행 화면 등)
    └── widgets/                # 재사용 가능한 컴포넌트 (차트, 로그 뷰어 등)
💻 로컬 터미널 실행 및 로그 파싱 코드 (Phase 3)
Android 인증 테스트(XTS)는 터미널에서 실행되며 방대한 양의 로그를 출력합니다. 따라서 프로세스가 끝날 때까지 기다리는 Process.run 대신, 실시간으로 출력(stdout)을 스트리밍할 수 있는 Process.start를 사용하는 것이 요구사항(실시간 터미널 추적)에 부합합니다.

1. Terminal Service (lib/services/terminal_service.dart)
터미널 프로세스를 백그라운드에서 실행하고, 결과 스트림을 앱으로 전달하는 서비스입니다.

dart
import 'dart:io';
import 'dart:async';
import 'dart:convert';

class TerminalService {
  Process? _process;

  /// XTS 테스트를 실행하고, 표준 출력(stdout)을 Stream으로 반환합니다.
  Future<Stream<String>> startTest(String toolPath, String command) async {
    // 예: command = 'run cts --shard-count 2 -s serial_1'
    final args = command.split(' ');

    // tradefed 콘솔 실행 (경로는 예시이며 실제 툴 경로에 맞게 조정 필요)
    final executable = '$toolPath/tools/cts-tradefed'; 

    _process = await Process.start(
      executable,
      args,
      workingDirectory: toolPath,
      mode: ProcessStartMode.normal, // 스트림 제어를 위해 normal 모드 사용 [web:20]
    );

    // stdout 바이트 스트림을 UTF-8 문자열로 디코딩하여 반환 [web:16]
    return _process!.stdout.transform(utf8.decoder);
  }

  /// 강제 종료 필요 시 프로세스를 중단합니다.
  void stopTest() {
    _process?.kill();
    _process = null;
  }
}
2. Log Parser Service (lib/services/log_parser_service.dart)
실시간으로 들어오는 문자열 로그를 분석하여 테스트의 진행 상태(대기, 진행, 종료 등)를 판별하는 경량화 로직입니다.

dart
enum TestStatus { idle, starting, running, finished, error }

class LogParserService {
  /// 로그 라인을 분석하여 현재 테스트 상태를 가볍게 추론합니다.
  TestStatus parseLogLine(String logLine) {
    final lowerLog = logLine.toLowerCase();

    // 실제 tradefed 로그 패턴에 맞춰 수정해야 합니다.
    if (lowerLog.contains('starting invocation')) {
      return TestStatus.starting;
    } else if (lowerLog.contains('executing test') || lowerLog.contains('running module')) {
      return TestStatus.running;
    } else if (lowerLog.contains('invocation finished') || lowerLog.contains('test run complete')) {
      return TestStatus.finished;
    } else if (lowerLog.contains('exception') || lowerLog.contains('fatal error')) {
      return TestStatus.error;
    }
    
    // 상태 변화가 없는 일반 로그는 그대로 상태 유지
    return TestStatus.running; 
  }

  /// 결과 폴더에서 test_result.xml 등 정적 파일을 파싱하는 로직 (추후 구현)
  Future<void> parseResultFiles(String resultFolderPath) async {
    // TODO: Phase 4에서 XML/HTML 파싱 후 Firestore 연동 (dart:io File 사용)
  }
}
3. Riverpod Provider (lib/providers/xts_runner_provider.dart)
Riverpod의 Notifier를 사용하여 위에서 만든 서비스들을 연결하고 앱 전체에 상태를 제공합니다. Riverpod 2.0+ 권장 방식에 맞춰 Notifier 클래스를 사용합니다.

dart
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/terminal_service.dart';
import '../services/log_parser_service.dart';

// 서비스의 싱글톤 인스턴스 제공을 위한 Provider
final terminalServiceProvider = Provider((ref) => TerminalService());
final logParserServiceProvider = Provider((ref) => LogParserService());

// 테스트 실행 상태를 관리하는 State 클래스
class XtsRunnerState {
  final TestStatus status;
  final List<String> logs; // 화면에 보여줄 최근 로그

  XtsRunnerState({this.status = TestStatus.idle, this.logs = const []});

  XtsRunnerState copyWith({TestStatus? status, List<String>? logs}) {
    return XtsRunnerState(
      status: status ?? this.status,
      logs: logs ?? this.logs,
    );
  }
}

// 상태 관리를 담당하는 Notifier 클래스
class XtsRunnerNotifier extends Notifier<XtsRunnerState> {
  @override
  XtsRunnerState build() {
    return XtsRunnerState();
  }

  Future<void> runTest(String toolPath, String command) async {
    final terminalService = ref.read(terminalServiceProvider);
    final logParser = ref.read(logParserServiceProvider);

    state = state.copyWith(status: TestStatus.starting, logs: ['명령어 실행: $command']);

    try {
      final logStream = await terminalService.startTest(toolPath, command);

      logStream.listen((logData) {
        // 줄바꿈 단위로 분리하여 처리
        final lines = logData.split('\n');
        for (var line in lines) {
          if (line.trim().isEmpty) continue;

          // 상태 파싱
          final newStatus = logParser.parseLogLine(line);
          
          // UI 업데이트를 위해 로그 누적 (메모리 방지를 위해 최근 100줄만 유지)
          final updatedLogs = List<String>.from(state.logs)..add(line);
          if (updatedLogs.length > 100) {
            updatedLogs.removeAt(0);
          }

          state = state.copyWith(
            status: newStatus != TestStatus.running ? newStatus : state.status,
            logs: updatedLogs,
          );
        }
      });
    } catch (e) {
      state = state.copyWith(
        status: TestStatus.error,
        logs: [...state.logs, '실행 에러 발생: $e'],
      );
    }
  }

  void stopTest() {
    ref.read(terminalServiceProvider).stopTest();
    state = state.copyWith(status: TestStatus.finished, logs: [...state.logs, '테스트 강제 종료됨']);
  }
}

// UI에서 접근할 Provider
final xtsRunnerProvider = NotifierProvider<XtsRunnerNotifier, XtsRunnerState>(() {
  return XtsRunnerNotifier();
});
이제 이 기반을 통해 앱 화면에서 ref.watch(xtsRunnerProvider)를 통해 실시간으로 터미널 로그를 렌더링하고 상태(진행 중, 완료 등)에 따라 UI를 변경할 수 있습니다.

다음 단계(Phase 4)인 Firestore CRUD Repository 구성 및 로컬 파일 파싱 결과 업로드 기능으로 넘어가도 괜찮을까요? 아니면 현재 터미널 파싱 부분에서 더 추가하고 싶은 구체적인 정규식/예외 처리가 있나요?