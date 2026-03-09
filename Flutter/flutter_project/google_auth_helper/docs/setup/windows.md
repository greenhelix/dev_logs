# Windows Setup

## 목표
- Flutter Desktop/Web 개발 환경 준비
- Firebase CLI 사용 가능 상태 확보
- 현재 이 저장소 기준으로 `flutter` / `dart` PATH 미설정 문제 해소

## 1. 필수 설치
1. Flutter SDK 설치
   - 공식 문서: https://docs.flutter.dev/get-started/install/windows/desktop
2. Visual Studio 2022 설치
   - `Desktop development with C++` workload 포함
3. Git 설치
4. Node.js 설치
5. Firebase CLI 설치 또는 확인
   - 공식 문서: https://firebase.google.com/docs/cli

## 2. 현재 셸 문제 반영
이 저장소의 현재 PowerShell에서는 아래 명령이 실패했다.

```powershell
flutter --version
dart --version
```

따라서 Flutter SDK `bin` 경로를 PATH에 추가해야 한다.

예시:
```powershell
$env:Path = "C:\src\flutter\bin;$env:Path"
flutter --version
dart --version
```

지속 반영은 시스템 환경 변수 또는 PowerShell profile에서 처리한다.

## 3. 검증
```powershell
flutter doctor
flutter config --enable-windows-desktop
flutter config --enable-web
firebase.cmd --version
```

## 4. 저장소 초기 부트스트랩
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\bootstrap_flutter_project.ps1
flutter pub get
flutter test
flutter build windows
flutter build web
```

## 5. Firebase 개발 준비
- `firebase.cmd login`
- 프로젝트 선택: `kani-projects`
- Hosting/Functions 초기화는 이미 저장소 설정을 기준으로 맞춰둠

## 6. 참고
- `cloud_firestore`는 Windows는 지원하지만 Linux는 공식 지원 목록에 없다.
- 따라서 이 저장소는 Firestore 접근을 REST 계층으로 통일했다.

