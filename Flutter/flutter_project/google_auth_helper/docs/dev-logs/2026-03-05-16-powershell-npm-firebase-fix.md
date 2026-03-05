# 2026-03-05 / 16-powershell-npm-firebase-fix

## 목표
- VSCode PowerShell에서 `npm`, `firebase` 명령 미인식/차단 문제 해결
- Firestore 인증 방식을 서비스 계정 기반으로 보강

## 완료 체크리스트
- [x] User PATH에 Node/NPM 경로 반영
- [x] PowerShell 실행 정책 `CurrentUser=RemoteSigned` 반영
- [x] `firebase-tools` 설치 및 실행 확인
- [x] Firestore 서비스 계정 파일 인증(`FIREBASE_SERVICE_ACCOUNT_FILE`) 지원

## 구현 내용
- 로컬 환경:
- `C:\\Program Files\\nodejs`, `C:\\Users\\Kim\\AppData\\Roaming\\npm` 경로 사용
- `firebase.cmd --version` 확인(15.8.0)
- 코드:
- `FIREBASE_SERVICE_ACCOUNT_FILE` 설정 추가
- `google-auth`로 액세스 토큰 자동 발급 후 Firestore REST 호출

## 검증 결과
- `python -m compileall app run_local.py` 통과
- `npm.cmd -v`, `firebase.cmd --version` 확인

## 다음 단계
- `.env`에 서비스 계정 파일 경로 반영 후 `/api/firebase/status` 재검증
