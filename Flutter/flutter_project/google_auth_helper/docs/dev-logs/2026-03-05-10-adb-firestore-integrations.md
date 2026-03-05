# 2026-03-05 / 10-adb-firestore-integrations

## 목표
- ADB 디바이스 목록 조회/선택 기반 실행 UX 추가
- Firestore CRUD API 골격 추가
- Redmine/Notion 실 API 업로드 연동

## 완료 체크리스트
- [x] ADB 디바이스 조회 API(`/api/adb/devices`) 추가
- [x] 테스트 시작 시 시리얼 미입력 시 자동 선택 로직 추가
- [x] 실행 UI에 ADB 디바이스 선택/새로고침 추가
- [x] Firestore 조회/쓰기/수정 API(`/api/firebase/firestore/*`) 추가
- [x] Redmine 이슈 생성 API 연동 추가
- [x] Notion 페이지 생성 API 연동 추가
- [x] Jira는 미구현 항목 상태 유지

## 구현 내용
- `AdbService.list_devices()`로 `adb devices -l` 파싱
- `start_job`에서 시리얼 공백이면 online device 자동 선택
- `FirestoreService` 추가(REST 기반)
- Redmine: `POST /issues.json`
- Notion: `POST /v1/pages`

## 검증 결과
- `python -m compileall app run_local.py` 통과

## 리스크/메모
- Firestore REST 쓰기/수정은 `FIREBASE_BEARER_TOKEN` 권한에 의존
- Notion DB 속성명(`Name`, `Result`, `Device`, `Summary`)은 대상 DB 스키마와 동일해야 함

## 다음 단계
- Firebase 인증 토큰 자동 갱신(서비스 계정) 방식 추가
- Firestore 조회 결과를 대시보드 실데이터와 연결
- Redmine/Notion 필드 매핑 옵션화
