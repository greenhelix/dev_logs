# 2026-03-05 / 20-smoke-test-script

## 목표
- 사용자가 진행 상태를 한 번에 확인할 수 있는 자동 점검 스크립트 제공

## 완료 체크리스트
- [x] 서버 기동 + 핵심 API 자동 점검 스크립트 추가
- [x] Firebase 동기화 API 포함 점검
- [x] README 실행 가이드 추가

## 구현 내용
- `scripts/smoke_test.ps1` 추가
- 점검 대상:
- `health`, `tools`, `watcher`, `firebase_status`, `adb_devices`
- `sync_monitor`, `sync_runs`
- 각 항목의 OK/ERR 테이블 + 상세 JSON 출력

## 검증 결과
- 로컬 실행 결과 모든 항목 `OK`
- `firebase_status` -> `ok=true`
- `sync_monitor` -> Firestore `monitor-latest` 업데이트 확인

## 다음 단계
- 필요 시 CI 환경에서 smoke test 자동 실행 연결
