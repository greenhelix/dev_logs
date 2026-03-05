# 2026-03-05 / 12-watcher-control-api

## 목표
- 결과 watcher를 운영 중에도 제어할 수 있도록 API 확장

## 완료 체크리스트
- [x] watcher enable/disable API 추가
- [x] watcher 즉시 스캔(scan-now) API 추가
- [x] 상태 API에 런타임 enabled 상태 반영

## 구현 내용
- `ResultWatcher`에 런타임 제어 메서드(`set_enabled`, `scan_now`) 추가
- `/api/watcher/enable`, `/api/watcher/disable`, `/api/watcher/scan-now` 엔드포인트 추가
- README에 watcher 제어 API 문서화

## 검증 결과
- `python -m compileall app/services/result_watcher.py app/main.py` 통과

## 다음 단계
- UI에서 watcher on/off 및 즉시 스캔 버튼 연결
