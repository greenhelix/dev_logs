# Architecture

## Summary
- 대시보드가 메인 홈이다.
- 화면 구조는 `GAH-Code-Repo`의 `sidebar + topbar + dashboard cards + chart grid + utility panels` 패턴을 계승한다.
- 데이터 흐름은 `sample/local paths -> parser -> domain models -> Firestore REST -> dashboard/proxy` 순서로 고정한다.

## Application Layers
```text
features/
  shell/         responsive shell, navigation, dashboard-first UX
  dashboard/     charts, summary cards, latest failures
  import/        local path import, parser execution, preview
  firebase/      sync status, remote collections preview
  settings/      app mode, credentials, path config
services/
  import_service.dart
  xts_result_parser.dart
  xts_live_log_parser.dart
  local_file_gateway.dart
  auth_header_provider.dart
data/
  firestore_rest_client.dart
  firestore_repository.dart
models/
  app_settings.dart
  tool_config.dart
  test_case_record.dart
  failed_test_record.dart
  test_metric_record.dart
```

## UI Structure
- 좌측 또는 드로어: 브랜드, 실행 모드, 연결 상태, 주요 섹션 이동
- 상단: 현재 화면 제목, 설명, 환경/연결 배지
- 홈: 핵심 수치 카드 + 차트 그리드 + 최근 실패 항목
- 보조 화면: Import, Firebase Center, Settings, Guide
- 모바일: Drawer 기반, 차트와 카드가 1열로 접힘

## Data Strategy
- `TestCases`: 모듈 + 테스트케이스 class 단위 메타데이터
- `FailedTests`: 실제 실패 테스트 메서드와 오류 스니펫
- `TestMetrics`: 세션 단위 집계와 대시보드용 수치
- Web은 read-only proxy를 통해 조회만 수행한다.
- Desktop은 REST + credential provider를 사용해 읽기/쓰기를 수행한다.

## Local Path Strategy
- `dev` 모드: `test_sample` 경로가 기본값으로 주입된다.
- `release` 모드: 경로 공란 상태로 시작한다.
- 사용자 설정은 로컬 저장소에 저장된다.
- 릴리즈에서 샘플 경로는 자동 주입하지 않는다.

