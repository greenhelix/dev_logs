# 2026-03-04 / 02-core-services

## 목표
- 핵심 기능(환경점검/도구 활성비활성/작업 실행/터미널/펌웨어/외부업로드) MVP 반영

## 완료 체크리스트
- [x] 환경설정 점검 기능 구현
- [x] 도구 존재 여부 기반 활성/비활성 구현
- [x] 테스트 작업 시작/취소/입력/목록 API 구현
- [x] 실시간 로그 WebSocket 구현
- [x] 실시간 터미널 WebSocket 구현
- [x] 펌웨어 업로드 + adb push API 구현
- [x] Jira/Redmine/Notion 연동 어댑터 뼈대 구현
- [x] 개발로그 운영 규칙 문서화

## 구현 내용
- `EnvironmentChecker`에서 Python/ADB/디렉터리 권한/연동설정을 검사
- `ToolRegistry`에서 도구 경로 자동 탐색 및 상태 반환
- `JobRunner`에서 subprocess 기반 실행과 로그 브로드캐스트 처리
- `AdbService`에서 adb push 처리
- `IntegrationDispatcher`에서 외부 시스템 업로드 흐름 통합

## 검증 결과
- API/서비스 코드 작성 완료
- 구문 검증 실행 필요

## 리스크/메모
- Jira/Redmine/Notion은 실제 API 스키마에 맞춘 필드 매핑이 아직 미구현(샘플 상태)
- 장시간 테스트 로그 저장 정책(rotate/보관기간)은 추후 설계 필요

## 다음 단계
- 구문 검증 및 실행 검증
- 실제 인증도구 명령 파라미터 세부화
- Firebase 조회 전용 대시보드 분리 여부 결정

