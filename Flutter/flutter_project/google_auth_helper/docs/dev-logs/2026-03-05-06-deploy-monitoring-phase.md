# 2026-03-05 / 06-deploy-monitoring-phase

## 목표
- Ubuntu 운영 배포 준비(systemd)와 Firebase 조회 전용 연동 기반 추가

## 완료 체크리스트
- [x] `systemd` 서비스 템플릿 추가
- [x] Ubuntu 설치 스크립트 추가
- [x] 모니터링 요약 API(`/api/monitor/summary`) 추가
- [x] 모니터링 토큰(`MONITOR_API_TOKEN`) 환경변수 추가
- [x] 배포/모니터링 문서 추가
- [x] 구문/실행 검증 수행

## 구현 내용
- `deploy/google-auth-helper.service` 생성
- `scripts/install_systemd.sh` 생성
- `MonitoringService` 및 `/api/monitor/summary` 엔드포인트 추가
- 토큰이 설정된 경우 `x-monitor-token` 헤더 검증 적용
- `docs/ubuntu-deploy.md`, `docs/firebase-monitoring.md` 문서화

## 검증 결과
- `python -m compileall app run_local.py` 통과
- 토큰 검증 테스트:
  - 토큰 없이 `/api/monitor/summary` 호출 -> 401
  - 올바른 토큰 헤더 포함 호출 -> 200

## 리스크/메모
- Windows 환경 정책으로 `__pycache__`/샘플 산출물 자동 삭제 명령은 차단됨
- 실제 Ubuntu 배포 시 서비스 유저/경로(`User`, `WorkingDirectory`) 값은 현장 환경에 맞게 수정 필요

## 다음 단계
- Ubuntu 실장비에서 systemd 기동/재기동/로그 확인 검증
- Firebase Cloud Functions 프록시 샘플 코드 추가

