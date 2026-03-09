# Google Auth Helper

Flutter 기반의 Google Auth Helper 재구성 저장소다. 목표는 Ubuntu 중심의 XTS 분석/운영 흐름과 Windows/Web 조회 흐름을 하나의 확장 가능한 코드베이스로 통합하는 것이다.

## 대원칙
- 모든 코드는 확장성을 우선한다.
- 메인 홈 화면은 항상 대시보드다.
- 화면 구조는 `GAH-Code-Repo`의 정보 계층을 계승하되 Flutter 반응형 레이아웃으로 구현한다.
- Windows 1차 범위는 조회/파싱/수동 입력 중심이다.
- 실제 XTS 실행 우선순위는 Ubuntu다.
- Firestore 연결은 REST로 통일한다.
- Web은 조회 전용, 데스크톱은 읽기/쓰기를 담당한다.
- `test_sample`은 개발용 fixture이며 릴리즈 기본 경로로 사용하지 않는다.
- README와 개발로그는 단계별로 계속 갱신한다.

## 현재 구현 범위
- Flutter 프로젝트 수동 스캐폴드
- Riverpod 기반 앱 구조
- XTS XML/로그 파서 골격
- Firestore REST 저장소 골격
- Firebase Hosting용 read-only proxy 스캐폴드
- Windows / Ubuntu / Firebase 설정 문서

## 현재 환경 진단
- 이 PowerShell 환경에서는 `flutter`와 `dart`가 PATH에 없다.
- `node`, `npm`, `firebase.cmd`는 확인되었다.
- 따라서 네이티브 runner 생성과 `flutter doctor` 검증은 SDK 설치 후 진행해야 한다.

확인된 버전:
- `node`: `v24.14.0`
- `npm`: `11.9.0`
- `firebase.cmd`: `15.8.0`

## 저장소 구조
```text
lib/
  app.dart
  main.dart
  core/
  data/
  features/
  models/
  services/
docs/
  architecture.md
  setup/
  dev-logs/
functions/
scripts/
test/
test_sample/
```

## Flutter 부트스트랩
Flutter SDK 설치 후 아래 스크립트로 누락된 플랫폼 runner를 생성한다.

Windows PowerShell:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap_flutter_project.ps1
```

Ubuntu:
```bash
bash ./scripts/bootstrap_flutter_project.sh
```

## Firebase Hosting / Proxy
- Firebase project: `kani-projects`
- Firestore database ID: `google-auth`
- Web은 `/api/test-cases`, `/api/failed-tests`, `/api/test-metrics` read-only endpoint만 사용한다.
- Hosting rewrites는 `firebase.json`에 정의되어 있다.

## 문서
- [Windows 설정 가이드](docs/setup/windows.md)
- [Ubuntu 설정 가이드](docs/setup/ubuntu.md)
- [Firebase 설정 가이드](docs/setup/firebase.md)
- [아키텍처](docs/architecture.md)
- [개발로그](docs/dev-logs/2026-03-09-01-flutter-bootstrap.md)

## 참고 기준
- Flutter Windows 설치: https://docs.flutter.dev/get-started/install/windows/desktop
- Flutter Linux 설치: https://docs.flutter.dev/get-started/install/linux/desktop
- Firebase CLI: https://firebase.google.com/docs/cli
- Firebase Admin named database: https://firebase.google.com/docs/reference/admin/node/firebase-admin.firestore
- `firebase_core` 지원 플랫폼: https://pub.dev/packages/firebase_core
- `cloud_firestore` 지원 플랫폼: https://pub.dev/packages/cloud_firestore

