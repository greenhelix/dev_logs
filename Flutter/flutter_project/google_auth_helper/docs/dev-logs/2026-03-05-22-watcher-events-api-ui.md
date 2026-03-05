# 2026-03-05 / 22-watcher-events-api-ui

## 목표
- 결과 watcher import 진행상태를 UI에서 즉시 확인 가능하게 개선

## 완료 체크리스트
- [x] watcher 이벤트 저장 큐 추가
- [x] watcher 이벤트 조회 API(`/api/watcher/events`) 추가
- [x] 환경 점검 화면에 최근 watcher 이벤트 표시 추가

## 구현 내용
- `ResultWatcher`에 최근 이벤트(deque, max 300) 저장
- 이벤트 레벨: `info`, `success`, `error`
- import 성공/실패, scan 시작, scan loop error를 이벤트로 기록
- 프론트에서 watcher 상태 조회 시 최근 이벤트 30건 함께 표시

## 검증 결과
- `python -m compileall app run_local.py` 통과
- `/api/watcher/scan-now` 호출 후 `/api/watcher/events`에서 이벤트 증가 확인

## 다음 단계
- watcher 이벤트를 WebSocket 로그 패널(`/ws/logs`)에도 태그 형태로 브로드캐스트
