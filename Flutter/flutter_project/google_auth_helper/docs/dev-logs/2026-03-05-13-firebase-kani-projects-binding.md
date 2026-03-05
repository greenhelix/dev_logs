# 2026-03-05 / 13-firebase-kani-projects-binding

## 목표
- Firebase 운영 기준값(`kani-projects`, `google-auth`, `kani-projects.web.app`) 반영
- Firestore 연동 상태 확인/동기화 API 강화

## 완료 체크리스트
- [x] Firestore 기본 컬렉션/Hosting URL 설정값 추가
- [x] Firebase 상태 확인 API(`/api/firebase/status`) 추가
- [x] 결과 동기화 기본 컬렉션을 `google-auth` 기준으로 적용
- [x] 모니터링 요약 동기화 API(`/api/firebase/sync/monitor`) 추가

## 구현 내용
- `Settings`에 `firebase_hosting_url`, `firestore_default_collection` 추가
- `.env.example`에 `kani-projects` 기본값 반영
- README/firebase-monitoring 문서에 운영값/엔드포인트 반영

## 검증 결과
- `python -m compileall app run_local.py` 통과
- `/api/firebase/status` 응답 확인(설정 부족 시 안내 메시지 반환)

## 다음 단계
- 실제 `FIREBASE_BEARER_TOKEN` 설정 후 `/api/firebase/status`를 `ok=true`로 확인
- `POST /api/firebase/sync/runs`, `POST /api/firebase/sync/monitor` 실동기화 검증
