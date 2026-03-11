# Google Auth Helper

Flutter 기반 운영 도구입니다. 현재 메뉴 구조는 `대시보드 / 결과 업로드 / 릴리즈 감시 / 환경점검 / 자동 테스트 / 설정`입니다.

## 현재 구현 범위
- `결과 업로드`
  - zip 선택과 파싱 상태를 분리해 유지
  - `test_result.xml`에서 빌드 메타데이터 파싱
  - `xts_tf_output.log`를 summary와 실패 핵심 로그의 최우선 소스로 사용
  - 실패 항목 제외/메모 편집 후 Firestore/Redmine 업로드 미리보기 생성
- `릴리즈 감시`
  - Excel 업로드 초안 등록
  - Google Sheet 링크 등록
  - `tools/release_watcher/output/latest_snapshot.json` watcher artifact 표시
- `환경점검`
  - Firebase Hosting
  - Firestore 다운로드
  - Firestore 업로드
  - Redmine 연결
  - Redmine 현재 사용자
  - Redmine 프로젝트 접근
- `자동 테스트`
  - 명령, 시리얼, 샤드, 자동 업로드 설정
  - 도구 루트/결과 경로/로그 경로 프로필 저장
  - Ubuntu 데스크톱에서 테스트 실행 지원
- `설정`
  - Firebase/Firestore 연결값
  - Web proxy base URL
  - Redmine Base URL / API Key / Project ID

## 업로드 메모
- zip 파일은 파싱 실패 시에도 선택된 파일명과 오류 상태를 유지합니다.
- 웹 업로드는 best-effort입니다. 브라우저가 파일 접근을 막으면 경고창으로 안내합니다.
- `olc_server_session_log.txt`는 live monitoring 보조용이며, 결과 summary/실패 핵심 로그의 기본 소스는 아닙니다.
- 현재 archive/local import 모두 `xts_tf_output.log`를 우선 사용합니다.

## 릴리즈 감시
- 별도 watcher 프로그램은 `tools/release_watcher/`에 있습니다.
- Windows 작업 스케줄러에서 오전 9시에 실행하는 구조를 전제로 합니다.
- Flutter는 watcher artifact를 읽고 화면에 보여주며, 감시 대상 초안은 로컬에 저장합니다.

## 개발 명령
```powershell
flutter pub get
flutter analyze
flutter test
flutter build web
```

## 플랫폼 참고
- 웹: 업로드와 감시 초안 등록 지원, 자동 테스트 실행은 미지원
- Windows: 업로드/감시/환경점검/설정 지원, 자동 테스트 실행은 현재 미지원
- Ubuntu: 전체 기능 지원

## 검증
- `flutter analyze`
- `flutter test`
- `flutter build web`

## Release Notes
- Current desktop release target: `v0.1.1`
- Desktop packaging outputs:
  - `test_release/windows/v0.1.1/`
  - `test_release/linux/v0.1.1/`
- Update check source: `https://github.com/greenhelix/GAH-Release-Repo/releases/latest`
- Release publish helpers:
  - `scripts/build_windows_release.ps1`
  - `scripts/build_ubuntu_release.sh`
  - `scripts/publish_release_repo.ps1`
  - `scripts/publish_release_repo.sh`
