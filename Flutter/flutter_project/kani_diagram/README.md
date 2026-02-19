# Kani Diagram

Flutter Web 기반 코드 분석/다이어그램 생성 도구입니다.

## 목표
- 여러 프로젝트 코드 파일을 업로드하여 핵심 구현 파일을 자동 탐지
- 사용자가 선택한 파일을 조합해 관계 다이어그램 생성
- 다이어그램 템플릿(5종) 미리보기 제공
- 선택 다이어그램 편집(노드 이름 수정)
- PNG/JPG 내보내기 지원
- Firestore 저장소 연동

## 기술 스택
- Flutter (Web)
- Riverpod (상태관리)
- Firebase Core / Cloud Firestore
- File Picker (다중 파일 업로드)

## 현재 구현 범위
1. 폴더 업로드 및 핵심 코드 점수화 리스트
- 업로드된 폴더 내부 코드 파일 중 테스트/생성 파일을 제외하고 점수 기반으로 핵심 파일 후보를 정렬
- 클래스/상태관리/서비스/비동기/Firestore 키워드 기반 점수 계산
- 다이어그램 생성 후 임시 업로드 파일은 메모리에서 즉시 삭제

2. 다이어그램 생성
- 선택 파일에서 클래스/상속/구현 관계를 파싱해 노드/엣지 생성
- 다이어그램 타입 선택 가능:
  - Flow
  - Class
  - Dependency
  - Layered
  - Sequence

3. 미리보기 + 편집
- 캔버스 미리보기 제공
- 노드 이름 수정 기능 제공

4. 용량 경고
- 다이어그램 대상 파일의 총 용량이 4MB를 초과하면 경고 메시지 표시

5. 내보내기
- PNG/JPG 다운로드 기능 제공 (웹 전용)

6. Firestore 저장
- 다이어그램을 Firestore `diagrams` 컬렉션에 저장/조회

## Firebase 설정 방법
`lib/firebase_options.dart`의 아래 값을 실제 Firebase 프로젝트 값으로 변경하세요.
- `apiKey`
- `appId`
- `messagingSenderId`
- `projectId` (`greenhelix-web-db`로 기본 반영됨)
- `authDomain`
- `storageBucket`
- `measurementId`

Firestore는 `greenhelix-web-db` 프로젝트의 멀티 DB인 `kani-diagram`을 사용하도록 코드에 반영되어 있습니다.

## Firebase Hosting 분리 배포
기존 상용 도메인(`greenhelix-web-db.web.app`)과 분리하기 위해 별도 Hosting 사이트 타겟을 사용합니다.

- 기본 사이트 ID: `greenhelix-web-db-kani-diagram`
- 배포 타겟: `kaniDiagramWeb`
- 설정 파일:
  - `.firebaserc`
  - `firebase.json`

필요 시 사이트 ID만 변경해서 같은 프로젝트에서 독립 배포할 수 있습니다.

```bash
firebase hosting:sites:create greenhelix-web-db-kani-diagram
flutter build web
firebase deploy --only hosting:kaniDiagramWeb
```

## import 구현 관련
- 웹에서는 브라우저 보안 정책상 임의 로컬 경로 접근(`dart:io`)이 불가능합니다.
- 따라서 현재 웹 앱은 파일 선택 업로드 기반으로 동작합니다.
- 추후 데스크톱/서버 확장을 위해 `lib/features/shared/project_importer_io.dart`에 `dart:io` 방식 import 구현을 추가해 두었습니다.

## 대용량 분석 확장 방향(향후)
- 현재 목표: 일반 앱 프로젝트 단위(안드로이드 앱 코드 규모) 분석
- 향후 확장 계획:
  1. 백그라운드 파싱 큐(Chunk 기반 스트리밍 분석)
  2. 파일 인덱싱 + 캐시(해시 기반 증분 분석)
  3. 대규모 프로젝트는 서버 측 분석 워커 분리
  4. 다이어그램 레벨-오브-디테일(LOD)로 렌더링 최적화

## 실행
```bash
flutter pub get
flutter run -d chrome
```

## 디버깅 체크리스트 (진행사항 확인)
1. 코드 품질 확인
```bash
flutter analyze
flutter test
```

2. 웹 빌드 확인
```bash
flutter build web
```

3. 앱 화면에서 확인할 항목
- 우측 패널 `디버그/진행 상태`에서 아래 확인:
  - Project: `greenhelix-web-db`
  - Firestore DB: `kani-diagram`
  - Hosting Site: `greenhelix-web-db-kani-diagram`
  - 업로드 파일 수 / 핵심 후보 수 / 선택 파일 수
  - 다이어그램 노드 수 / 관계 수 / 크기(MB)

4. Firebase Hosting 분리 배포 확인
```bash
firebase target:apply hosting kaniDiagramWeb greenhelix-web-db-kani-diagram
firebase deploy --only hosting:kaniDiagramWeb
```

## 개발 로그
- 2026-02-19 18:27:22 KST
  - Flutter Web 프로젝트 초기 생성
  - Riverpod/Firebase/파일 업로드 기반 구조 설계 시작
- 2026-02-19 18:30:04 KST
  - 분석 엔진(CodeParserService) 구현
  - 핵심 파일 리스트업/선택/다이어그램 생성 로직 구현
- 2026-02-19 18:30:04 KST
  - 다이어그램 템플릿 5종 미리보기 캔버스 구현
  - 노드 레이블 편집, PNG/JPG 내보내기, Firestore 저장 기능 연결
- 2026-02-19 18:45:59 KST
  - Firestore를 `greenhelix-web-db` 프로젝트의 `kani-diagram` 데이터베이스로 연결
  - Hosting을 기존 상용 도메인과 분리하기 위해 별도 사이트 타겟(`kaniDiagramWeb`) 배포 설정 추가
- 2026-02-19 18:47:06 KST
  - 테스트 환경에서 Firebase 미초기화 시 메모리 저장소 fallback 적용
  - 웹/테스트 작은 화면 폭에서 버튼 영역 오버플로우가 나지 않도록 `Wrap` 기반 반응형 수정
- 2026-02-19 18:49:08 KST
  - 앱 우측 패널에 `디버그/진행 상태` 추가 (프로젝트/DB/호스팅/파일수/다이어그램 상태)
  - 실행/빌드/배포 점검용 디버깅 체크리스트 문서화
- 2026-02-19 18:52:35 KST
  - 우측 디버그 패널의 작은 폭 줄바꿈으로 인한 세로 오버플로우 수정(한 줄 + ellipsis)
  - 검증 완료: `flutter analyze` 통과, `flutter test` 통과, `flutter build web` 성공
- 2026-02-19 18:54:46 KST
  - Firebase Hosting 멀티사이트 구성 완료 (`greenhelix-web-db-kani-diagram`)
  - 배포 완료: `firebase deploy --only hosting:kaniDiagramWeb`
  - 서비스 URL: `https://greenhelix-web-db-kani-diagram.web.app`
- 2026-02-19 19:00:00 KST
  - 업로드 방식을 파일 선택에서 폴더 업로드로 변경(`webkitdirectory`)
  - 업로드 파일은 임시 메모리로만 보관하고 다이어그램 생성 직후 자동 삭제되도록 수정
  - 용량 절감을 위해 결과 다이어그램 중심 저장 흐름으로 정리
- 2026-02-19 19:02:17 KST
  - 폴더 업로드 전용 피커 서비스 도입(`project_folder_picker_web.dart`)
  - 다이어그램 생성 시 선택 파일 소비 후 원본 임시데이터 초기화(`consumeSelectedFilesForDiagram`)
  - 검증 완료: `flutter analyze` 통과, `flutter test` 통과, `flutter build web --no-wasm-dry-run` 성공
- 2026-02-19 19:06:34 KST
  - 다이어그램 파싱 정규식 버그 수정(`\\s` 오사용 -> `\s`)으로 클래스/관계 추출 정상화
  - 다이어그램 생성 실패 시 임시파일이 삭제되지 않도록 삭제 시점을 생성 성공 이후로 변경
  - 검증 완료: `flutter analyze` 통과, `flutter test` 통과
- 2026-02-19 19:07:22 KST
  - 업로드된 핵심 파일 리스트에 `전체 선택`, `전체 해제` 기능 추가
  - 선택 개수(선택/전체) 표시 추가
  - 검증 완료: `flutter analyze` 통과, `flutter test` 통과
- 2026-02-19 19:10:59 KST
  - 다이어그램 생성 실패 원인을 스낵바/안내문으로 즉시 표시하도록 수정
  - 파일 미선택 상태에서는 `다이어그램 생성` 버튼 비활성화하도록 조건 추가
  - 검증 완료: `flutter analyze` 통과, `flutter test` 통과
