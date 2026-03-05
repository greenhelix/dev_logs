# Google Auth Helper (GAH) 개발 가이드

## 0. 확정 결정 사항 (2026-03-05)
- Firebase 범위: 조회 + 쓰기 + 수정 포함 (백엔드: Firestore)
- 릴리즈 우선순위:
- Ubuntu: 운영 기준, 모든 기능 제공
- Windows: 조회 + 데이터 입력까지 제공 (웹 접속형 우선, `python run_local.py` 형태)
- 자동 결과 수집 방식: 결과 폴더 watcher 기반
- 도구별 기본 경로 예시:
- tool: `./xts/cts/android-cts-R11-arm/android-cts/tools/cts-tradefed`
- results: `./xts/cts/android-cts-R11-arm/android-cts/results/...`
- logs: `./xts/cts/android-cts-R11-arm/android-cts/logs/...`

## 1. 목표와 운영 환경
- Ubuntu PC
- GAH 운영 본체
- CTS/GTS/TVTS/VTS/STS/CTS-on-GSI 실행
- 결과 파싱/저장/업로드/모니터링/터미널 제어 제공
- HDFury/CecAdapter 등 장비 연동 점검 포함
- Windows PC
- GAH 설치형: 조회 + 데이터 입력 기능 제공
- GAH 미설치: Firebase Hosting에서 조회 기능 제공
- Smartphone
- Firebase Hosting 기반 조회 기능 제공

## 2. 개발 단계 (Phase)
### Phase 1 (우선, Ubuntu 운영 완성)
- 환경점검, 도구 실행, 작업 제어, 결과 폴더 watcher, 자동 파싱/저장
- Jira/Redmine/Notion 업로드 실구현
- Firebase Cloud 조회/쓰기/수정 API
- 모니터링 대시보드 실데이터 연결

### Phase 2 (Windows 확장)
- Windows 클라이언트 기능: 조회 + 데이터 입력
- Ubuntu API 연동 안정화
- 권한/네트워크/설치 가이드 정리

### Phase 3 (운영 고도화)
- 업데이트 확인/버전 관리 자동화
- 인증도구 버전 추적/비교 리포트
- 히스토리 분석, 장기 보관 정책, 성능 최적화

## 3. 핵심 기능 요구사항
- 테스트 실행 전 환경 점검
- ADB 디바이스 검색, 다중 디바이스 선택
- 사용자 등록 명령 실행 + 장비 선택 반영
- 결과 폴더 watcher가 새 결과 감지 시 자동 import(on/off)
- 결과 파싱 후 대시보드 자동 갱신
- 외부 업로드(Jira/Redmine/Notion) 포맷 고정 + 선택 업로드
- Firebase Cloud 데이터 조회/쓰기/수정
- 진행 중 테스트 모니터링 + 최근 기록 조회

## 4. 데이터 모델 기준
- 수치 데이터(통계)
- 테스트 단위 집계: pass/fail/skip, 경과시간, FW/tool version, 인증 타입
- 케이스 데이터(상세)
- 모듈, 모듈 설명, 테스트케이스, 설명, 에러로그, 해결 히스토리
- 구분 체계
- (1) 인증 테스트별 수치 데이터
- (2) 테스트케이스별 상세 데이터

## 5. 완료 기준 (Definition of Done)
- 기능 단위로 아래 항목 모두 충족 시 완료 처리
- API 명세/요청/응답 예시 문서화
- UI 동작 확인(주요 시나리오)
- 실패/예외 케이스 처리
- 로컬 검증(컴파일/테스트)
- `docs/dev-logs/YYYY-MM-DD-XX-*.md` 기록
- README/Architecture/Diagram 동기화

## 6. 배포/유지보수 원칙
- Ubuntu 배포 우선 (systemd 서비스 기준)
- Windows는 조회/입력 클라이언트로 단계 확장
- 실행 시 업데이트 확인(버전 체크) 제공
- 코드 저장소(private)와 릴리즈 저장소(public 또는 별도) 분리 운영
- Google 인증도구 바이너리는 저장소 미포함, 경로/버전 정보만 관리

## 7. 문서 운영 규칙
- 구조 변경 시:
- `README.md` 업데이트
- `docs/architecture.md` 업데이트
- 필요 시 다이어그램 갱신
- 릴리즈 시:
- 코드 repo: 변경 요약 + 개발로그 링크
- 릴리즈 repo: 버전명 + 사용자 설치/사용 요약
- GitHub 저장소 고정:
- 코드 repo: `greenhelix/dev_logs/Flutter/flutter_project/google_auth_helper`
- 릴리즈 repo: `greenhelix/GAH-Release-Repo`

## 8. 다음 구현 우선순위 (실행 항목)
- 결과 폴더 watcher 서비스 추가 + on/off 설정
- ADB 디바이스 목록 API + UI 선택 기능
- Firebase Cloud CRUD API 설계/구현
- Jira/Redmine/Notion 실 API 매핑
- Windows 조회/입력 UI 경량 모드 정의


