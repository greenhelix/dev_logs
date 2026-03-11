# Ubuntu Setup

## 목표
- Flutter Linux 개발 환경 구성
- Ubuntu 데스크톱에서 조회/업로드/테스트 실행 가능 상태 준비

## 필수 설치
```bash
sudo apt-get update
sudo apt-get install -y curl git unzip xz-utils zip clang cmake ninja-build pkg-config libgtk-3-dev liblzma-dev
```

Flutter 공식 문서:
- https://docs.flutter.dev/get-started/install/linux/desktop

추가 도구:
- adb
- Java
- Python
- Firebase CLI

## Flutter 준비
```bash
export PATH="$HOME/flutter/bin:$PATH"
flutter --version
flutter doctor
flutter config --enable-linux-desktop
flutter config --enable-web
```

## 저장소 초기화
```bash
cd ~/path/to/google_auth_helper
bash ./scripts/bootstrap_flutter_project.sh
flutter pub get
flutter test
flutter analyze
flutter build linux
```

## Ubuntu 역할
- 허용
  - 대시보드 조회
  - 결과 미리보기
  - Firestore 결과 업로드
  - 테스트 실행
  - 실행 후 자동 업로드
- 실행 화면 기본 범위
  - 도구 선택
  - 명령어 편집
  - serial/shard 설정
  - 시작/중지
  - 실시간 로그

## 경로 설정 예시
- CTS root: `/home/innopia/xts/cts/android-cts-14_r10-linux_x86-arm`
- results root: `/home/innopia/xts/cts/android-cts-14_r10-linux_x86-arm/android-cts/results`
- logs root: `/home/innopia/xts/cts/android-cts-14_r10-linux_x86-arm/android-cts/logs`

`results` 와 `logs`는 특정 세션 디렉터리가 아니라 상위 루트를 넣어도 됩니다. 앱이 최신 `test_result.xml`과 우선 로그 파일을 찾도록 구성되어 있습니다.
## Additional Notes
- Run `flutter build linux` and one manual zip upload smoke test on a Linux host before release.
- Local path imports prefer `xts_tf_output.log` for counts and failure snippets, while `olc_server_session_log.txt` remains a live-status fallback.
