# 2026-03-05 / 19-firebase-cli-token-fallback-success

## 목표
- 서비스계정 파일이 없는 개발 PC에서도 Firestore 연동 가능하게 구성
- `kani-projects/google-auth` 실연동 검증

## 완료 체크리스트
- [x] Firebase CLI 토큰 fallback 인증 로직 추가
- [x] Firebase 상태 API `ok=true` 확인
- [x] 모니터링 요약 Firestore 업로드 확인

## 구현 내용
- `FirestoreService` 인증 순서:
- `FIREBASE_BEARER_TOKEN`
- `FIREBASE_SERVICE_ACCOUNT_FILE`
- `firebase-tools.json` access token fallback
- 토큰 fallback은 만료 시간(`expires_at`) 확인 후 유효 토큰만 사용

## 검증 결과
- `GET /api/firebase/status` -> `ok=true`
- `POST /api/firebase/sync/monitor` -> 200
- Firestore 문서 생성 확인:
- `projects/kani-projects/databases/google-auth/documents/google-auth/monitor-latest`

## 다음 단계
- 대시보드의 결과 동기화(`sync/runs`)를 주기 작업으로 연결
- 배포 환경에서는 서비스계정 파일 방식으로 고정
