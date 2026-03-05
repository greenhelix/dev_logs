# 2026-03-05 / 15-firestore-db-id-and-firebase-cli

## 목표
- Firestore 데이터베이스 ID(`google-auth`)를 코드에 반영
- 로컬 환경에 Firebase CLI 설치 확인

## 완료 체크리스트
- [x] `FIRESTORE_DATABASE_ID` 설정값 추가
- [x] Firestore REST base URL에 DB ID 반영
- [x] Firebase 상태 API에 `database_id` 포함
- [x] `firebase-tools` 설치 및 버전 확인

## 구현 내용
- `Settings.firestore_database_id` 추가
- Firestore URL을 `/databases/{database_id}/documents`로 변경
- `.env.example`에 `FIRESTORE_DATABASE_ID=google-auth` 기본값 추가
- `npm install -g firebase-tools` 설치 후 `firebase.cmd --version` 확인

## 검증 결과
- `python -m compileall app/config.py app/services/firestore_service.py app/main.py` 통과
- `firebase.cmd --version` -> `15.8.0`

## 리스크/메모
- 현재 쉘 정책으로 `firebase`(PowerShell shim) 직접 호출 시 제한될 수 있어 `firebase.cmd` 사용 권장
- 실제 Firestore 연결 성공 여부는 `.env`에 `FIREBASE_PROJECT_ID`, `FIREBASE_BEARER_TOKEN`, `FIRESTORE_DATABASE_ID`를 설정해야 확인 가능
