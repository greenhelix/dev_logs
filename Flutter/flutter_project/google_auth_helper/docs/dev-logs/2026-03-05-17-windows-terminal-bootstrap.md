# 2026-03-05 / 17-windows-terminal-bootstrap

## 목표
- VSCode PowerShell에서 `npm`/`firebase` 미인식 이슈 해소
- 개발자가 매번 동일 환경으로 진입할 수 있는 부트스트랩 스크립트 제공

## 완료 체크리스트
- [x] PowerShell PATH 부트스트랩 스크립트 추가
- [x] Firebase 문서에 트러블슈팅 섹션 추가
- [x] 스크립트 실행 검증

## 구현 내용
- `scripts/windows-dev-shell.ps1` 추가
- 스크립트가 Node/NPM/Firebase 버전을 즉시 출력해 상태 확인 가능
- `docs/firebase-monitoring.md`에 PowerShell 명령/필수 .env 값 정리

## 검증 결과
- `powershell -ExecutionPolicy Bypass -File .\scripts\windows-dev-shell.ps1` 실행 성공
- 출력: node `v24.14.0`, npm `11.9.0`, firebase `15.8.0`
- `python -m compileall app run_local.py` 통과

## 다음 단계
- `.env` 실제 값 세팅 후 `GET /api/firebase/status`를 `ok=true`로 맞춤
