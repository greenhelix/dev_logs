# Windows Setup

## 목표
- Flutter Windows/Web 개발 환경 구성
- Windows 데스크톱 빌드 오류 방지
- 결과 조회/업로드용 Windows 앱 실행 준비

## 필수 설치
1. Flutter SDK
2. Visual Studio 2022
3. `Desktop development with C++` workload
4. Git
5. Node.js
6. Firebase CLI

공식 문서:
- https://docs.flutter.dev/get-started/install/windows/desktop
- https://firebase.google.com/docs/cli

## 표준 경로
- Flutter SDK: `E:\flutter`
- Pub Cache: `E:\Pub\Cache`
- 저장소: `NTFS` 드라이브의 경로 사용

## 파일시스템 전제 조건
- Windows 데스크톱 빌드는 프로젝트 드라이브가 `NTFS`여야 합니다.
- `exFAT` 드라이브에서는 Flutter가 `windows/flutter/ephemeral/.plugin_symlinks` 를 만들지 못해 빌드가 실패합니다.
- 현재 저장소가 `exFAT`에 있으면 `C:` 같은 `NTFS` 드라이브로 저장소를 옮긴 뒤 빌드해야 합니다.

## PUB_CACHE 설정
PowerShell:
```powershell
setx PUB_CACHE E:\Pub\Cache
```

새 PowerShell을 열고 확인:
```powershell
echo $env:PUB_CACHE
```

`E:\Pub\Cache` 가 출력되어야 합니다.

## PATH 확인
```powershell
$env:Path = "E:\flutter\bin;$env:Path"
flutter --version
dart --version
```

## 저장소 초기화
```powershell
cd E:\github\dev_logs\Flutter\flutter_project\google_auth_helper
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap_flutter_project.ps1
flutter pub get
flutter test
flutter analyze
flutter build web
flutter build windows
```

## Windows 역할
- 허용
  - 대시보드 조회
  - 결과 미리보기
  - Firestore 결과 업로드
  - 설정 수정
- 비허용
  - 테스트 실행

## 주의사항
- `Creating symlink ... ERROR_INVALID_FUNCTION` 오류가 나면 아래 순서로 확인합니다.
  - 저장소 드라이브가 `NTFS`인지 확인
  - `PUB_CACHE=E:\Pub\Cache`가 적용됐는지 확인
  - 새 셸에서 다시 `flutter pub get`, `flutter build windows` 실행
## Additional Notes
- Native Windows smoke tests must be run on an NTFS drive that is compatible with Flutter plugin symlinks.
- Zip upload on Windows should allow file selection even if parsing fails afterward.
- The separate release watcher is expected to run through Windows Task Scheduler.
