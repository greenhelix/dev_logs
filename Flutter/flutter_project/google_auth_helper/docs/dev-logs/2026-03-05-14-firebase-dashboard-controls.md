# 2026-03-05 / 14-firebase-dashboard-controls

## 목표
- 대시보드에서 Firebase 연동 상태 확인 및 동기화 제어 제공

## 완료 체크리스트
- [x] Firebase 상태 확인 버튼 추가
- [x] 결과 동기화 버튼 추가
- [x] 모니터링 동기화 버튼 추가
- [x] 상태/응답 JSON 표시 영역 추가

## 구현 내용
- `app/static/index.html` 대시보드에 Firebase 제어 카드 추가
- `app/static/app.js`에 `loadFirebaseStatus`, `syncFirebaseRuns`, `syncFirebaseMonitor` 추가
- 초기화 시 Firebase 상태 자동 조회

## 검증 결과
- ID/event 연결 점검 통과
- `python -m compileall app run_local.py` 통과

## 다음 단계
- Firebase 동기화 실행 결과를 그래프/지표에 실시간 반영
