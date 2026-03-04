# 2026-03-05 / 04-execution-test

## 목표
- 실제 서버 실행 기반으로 API/정적 UI/WebSocket 동작 검증

## 완료 체크리스트
- [x] `uvicorn` 서버 기동/종료 자동 테스트
- [x] 핵심 API 엔드포인트 응답 확인
- [x] 정적 웹 리소스 응답 확인
- [x] 외부 업로드 API 샘플 호출 확인
- [x] 에러 처리(도구 미설정, adb 미설치) 확인
- [x] WebSocket(`/ws/logs`, `/ws/terminal`) 연결/송수신 확인

## 구현 내용
- `httpx` 기반 실행 테스트 스크립트로 `/api/*`, `/`, `/static/*` 검증
- `websockets` 기반으로 `/ws/logs` 연결, `/ws/terminal` 명령 echo 검증

## 검증 결과
- `/api/health`: 200
- `/api/tools`: 200 (도구 6개, 현재 환경 available=0)
- `/api/environment/check`: 200 (ok=4, warn=4, error=0)
- `/api/jobs`: 200
- `/api/reports/upload`: 200 (연동 설정 부재로 업로드 스킵 메시지 확인)
- `/api/firmware/upload`: 400 (`adb 명령을 찾을 수 없습니다.`)
- `/api/jobs/start`: 400 (`CTS 도구가 비활성화 상태입니다.`)
- `/`: 200, `/static/styles.css`: 200, `/static/app.js`: 200
- `/ws/logs`: 연결 성공
- `/ws/terminal`: `echo WS_TERMINAL_OK` 수신 확인

## 리스크/메모
- 현재 테스트는 Windows 환경에서 수행되어 실제 Ubuntu 도구 실행 경로는 미검증
- adb/인증 도구 설치 후 Ubuntu에서 통합 재검증 필요

## 다음 단계
- Ubuntu 장비에서 도구 경로/adb 설정 후 실제 CTS 계열 테스트 실행
- Jira/Redmine/Notion 실 API 필드 매핑 및 인증 처리 구현

