# 2026-03-05 / 09-tool-setup-architecture-update

## 목표
- Ubuntu 도구 경로 설정 방법 명확화
- 현행 구현 상태 기준 아키텍처 다이어그램 업데이트

## 완료 체크리스트
- [x] 도구 경로 설정 가이드 문서 추가
- [x] README 아키텍처 다이어그램 최신화
- [x] docs/architecture.md 최신 구조로 재작성
- [x] 도구 경로가 실제 실행 명령에 반영되도록 레지스트리 개선

## 구현 내용
- `docs/tool-setup.md` 생성
- `ToolRegistry.build_command()`에서 지정 경로의 실행파일을 우선 탐색하도록 수정
- README/architecture 문서에 결과 파서/SQLite/그래프 API/모니터링 API 반영

## 검증 결과
- `python -m compileall app` 통과

## 다음 단계
- 실장비 경로 값으로 `/api/tools` 확인 후 실제 도구 실행 검증

