# 2026-03-05 / 21-watcher-ui-controls

## 목표
- UI에서 결과 watcher 상태 확인/제어를 직접 수행할 수 있게 개선

## 완료 체크리스트
- [x] 환경 점검 화면에 watcher 제어 카드 추가
- [x] watcher 상태 조회 버튼 추가
- [x] watcher 시작/중지/즉시 스캔 버튼 추가
- [x] 상태 요약/상세 JSON 표시 추가

## 구현 내용
- `app/static/index.html`에 watcher 제어 영역 추가
- `app/static/app.js`에 다음 함수 추가:
- `loadWatcherStatus()`
- `controlWatcher(action)`
- 초기 로딩 시 watcher 상태 자동 조회

## 검증 결과
- `python -m compileall app run_local.py` 통과
- `/api/watcher/status`, `/api/watcher/disable`, `/api/watcher/enable`, `/api/watcher/scan-now` 모두 200 응답 확인

## 다음 단계
- watcher 이벤트(import 성공/실패)를 실시간 로그 패널에 별도 태그로 표시
