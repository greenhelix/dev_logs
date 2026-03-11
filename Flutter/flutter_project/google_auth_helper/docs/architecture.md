# Architecture

## 요약
- 메인 홈은 `대시보드`입니다.
- 메뉴 구조는 `대시보드 / 결과 / 환경점검 / 실행 / 설정`입니다.
- 플랫폼 권한은 런타임에서 자동 계산합니다.
- 업로드 화면은 `zip 업로드 -> 파싱 -> 미리보기 -> 업로드` 흐름으로 동작합니다.
- 웹은 Firebase Hosting 뒤 Functions 프록시를 통해 Firestore와 Redmine을 사용합니다.

## 계층 구조
```text
features/
  shell/        반응형 레이아웃, 헤더, 사이드바
  dashboard/    그래프, 최근 실패, 요약 카드
  results/      zip 업로드, 파싱 미리보기, 업로드 실행
  environment/  Hosting/Firestore/Redmine 상태 점검
  run/          Ubuntu 전용 실행/로그 화면
  settings/     경로, Firebase, Redmine, 실행 기본값 설정
services/
  archive_import_service.dart   zip 메모리 파싱
  import_service.dart           로컬 경로 기반 파싱
  redmine_service.dart          Redmine 마크다운/업로드
  environment_check_service.dart 환경점검
  xts_execution_service.dart    Ubuntu 실행
data/
  firestore_rest_client.dart    Firestore REST + Hosting 프록시 호출
  firestore_repository.dart     Firestore 조회/업로드 저장소
  demo_seed_data.dart           샘플 데이터
models/
  app_settings.dart             전역 설정 모델
  tool_config.dart              도구별 설정
  import_bundle.dart            파싱 결과 묶음
  upload_target.dart            업로드 대상 타입
providers/
  app_providers.dart            Riverpod 상태 조합
```

## 플랫폼 권한 모델
- Firebase Hosting Web
  - 대시보드, 결과, 환경점검, 설정 가능
  - zip 업로드와 Firestore/Redmine 업로드 가능
  - 테스트 실행 불가
- Windows Desktop
  - 대시보드, 결과, 환경점검, 설정 가능
  - 로컬 경로 기반 파싱과 업로드 가능
  - 테스트 실행 불가
- Ubuntu Desktop
  - 대시보드, 결과, 환경점검, 설정, 실행 가능
  - 로컬 경로 기반 파싱과 업로드 가능
  - 테스트 실행 가능

## 화면 구조
- 대시보드
  - 주요 지표 카드
  - 성공/실패 추이
  - 소요 시간 차트
  - 최근 실패 목록
- 결과
  - 도구 선택
  - 업로드 대상 선택
  - zip 업로드
  - 설정 경로 미리보기
  - Redmine 마크다운 미리보기
  - Firestore 적재 데이터 미리보기
  - 업로드 히스토리
- 환경점검
  - Firebase Hosting 상태
  - Firestore 다운로드 상태
  - Firestore 업로드 상태
  - Redmine 연결 상태
- 실행
  - 우분투 전용
  - 도구 선택, 명령 편집, 시리얼, 샤드, 시작/중지, 실시간 로그
- 설정
  - Firebase 프로젝트/DB
  - 자격증명
  - Redmine 연결 정보
  - 도구별 경로/명령/샤드/자동 업로드

## 데이터 흐름
1. `test_sample` 또는 사용자가 업로드한 zip/지정 경로에서 결과를 읽습니다.
2. XML과 로그를 파싱해 `ImportBundle`을 만듭니다.
3. 결과 화면에서 Redmine/Firestore 미리보기를 생성합니다.
4. Firestore 업로드는 Firestore REST 또는 Hosting 프록시를 통해 처리합니다.
5. Redmine 업로드는 데스크톱 직접 호출 또는 Hosting 프록시를 통해 처리합니다.
6. 대시보드는 원격 데이터와 현재 미리보기를 함께 보여줍니다.

## 샘플과 릴리즈 모드
- 개발 모드
  - `test_sample` 기반 기본 미리보기를 허용합니다.
- 릴리즈 모드
  - 샘플 경로를 자동 주입하지 않습니다.
  - 사용자가 직접 경로를 입력합니다.
