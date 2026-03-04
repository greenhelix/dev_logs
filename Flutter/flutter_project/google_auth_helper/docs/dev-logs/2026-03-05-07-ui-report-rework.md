# 2026-03-05 / 07-ui-report-rework

## 목표
- UI 구조 개편(사이드 메뉴 + 하단 고정 로그/터미널)
- 결과서 자동 분석/저장 기능 추가
- 대시보드 그래프(가상 데이터 포함) 제공

## 완료 체크리스트
- [x] 메인 화면에서 환경점검/도구상태 분리(사이드 메뉴 이동)
- [x] 하단 고정 콘솔(작업로그/터미널 + 입력창) 배치
- [x] 결과서(XML/HTML) 자동 파싱 API 추가
- [x] 펌웨어/도구버전/경과시간 자동 추출 반영
- [x] 전체 테스트케이스 저장(SQLite) 및 조회 API 추가
- [x] 대시보드 그래프 API/UI 추가
- [x] 스모크 검증(파싱/저장/조회/UI 응답) 수행

## 구현 내용
- 프론트:
  - 사이드 네비게이션 기반 뷰 전환
  - 대시보드 그래프(Chart.js)
  - 결과서 업로드/분석/외부업로드 자동요약 흐름
  - 저장된 결과서 목록/상세 조회
  - 하단 고정 로그/터미널 도킹 패널
- 백엔드:
  - `ReportParser`로 XML/HTML 파싱
  - `ResultStore`로 run/case 저장
  - `AnalyticsService`로 그래프 데이터 제공
  - `/api/reports/import-file`, `/api/reports/runs`, `/api/reports/runs/{id}`, `/api/analytics/dashboard`

## 검증 결과
- `python -m compileall app run_local.py` 통과
- XML 파싱: fail_count=1, firmware/tool/elapsed 추출 확인
- HTML 파싱: fail_count=1, 실패 케이스 추출 확인
- 저장 조회: run 목록/상세 케이스 조회 정상
- UI 응답: `/` 및 `/static/*` 응답 정상

## 리스크/메모
- Chart.js CDN 의존(오프라인 환경 시 로컬 번들링 필요)
- HTML 결과서 형식이 벤더별로 다르면 정규식 보강 필요

## 다음 단계
- 결과서 파서 규칙 샘플 데이터로 추가 튜닝
- 저장된 테스트케이스 필터/검색 UI 고도화

