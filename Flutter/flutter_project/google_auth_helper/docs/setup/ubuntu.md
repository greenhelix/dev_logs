# Ubuntu Setup

## 목표
- Ubuntu Desktop Flutter 빌드 환경 준비
- XTS 도구 경로와 로컬 분석 환경 준비
- Firebase CLI와 데스크톱 쓰기용 credential 준비

## 1. Flutter 설치
- 공식 문서: https://docs.flutter.dev/get-started/install/linux/desktop

필수 패키지 예시:
```bash
sudo apt-get update
sudo apt-get install -y curl git unzip xz-utils zip libglu1-mesa clang cmake ninja-build pkg-config libgtk-3-dev
```

Flutter SDK 설치 후:
```bash
flutter doctor
flutter config --enable-linux-desktop
flutter config --enable-web
```

## 2. XTS 운영 환경 점검
`dev.md` 기준 필수 확인 항목:
- `adb`
- Java
- Python
- 인증 도구 루트 경로
- 필요 시 Redmine 연동 환경

예시 경로:
```text
/home/innopia/xts/cts/android-cts-14_r10-linux_x86-arm
/home/innopia/xts/gts/android-gts-13.1-R1-13-16-14373446
/home/innopia/xts/tvts/android-tvts-2.16R2-arm
/home/innopia/xts/vts/android11-vts-r16-arm
/home/innopia/xts/sts/android-sts-11_sts-r23-linux-arm
```

## 3. 검증
```bash
which adb
java -version
python3 --version
flutter doctor
firebase --version
```

## 4. 저장소 부트스트랩
```bash
bash ./scripts/bootstrap_flutter_project.sh
flutter pub get
flutter test
flutter build linux
flutter build web
```

## 5. Firestore 쓰기 준비
- 권장: 로컬에 `service-account.json` 보관 후 Settings 화면에서 경로 지정
- 대안: Firebase CLI 로그인 후 로컬 access token fallback 사용

## 6. 주의
- Ubuntu에서도 FlutterFire plugin 대신 REST를 사용한다.
- 이유: Linux desktop 공식 지원 공백과 named database(`google-auth`) 일관성 때문이다.

