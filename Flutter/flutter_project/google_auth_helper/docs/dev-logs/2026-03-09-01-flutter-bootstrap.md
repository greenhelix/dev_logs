# 2026-03-10 개발 로그

## 반영 내용
- 플랫폼 권한 모델 재정의
  - Web: 조회, zip 업로드, Firestore 업로드, Redmine 업로드, 환경점검, 설정
  - Windows: 조회, zip 업로드, Firestore 업로드, Redmine 업로드, 환경점검, 설정
  - Ubuntu: 조회, zip 업로드, Firestore 업로드, Redmine 업로드, 환경점검, 설정, 테스트 실행
- 메뉴 구조를 `대시보드 / 결과 / 환경점검 / 실행 / 설정`으로 재편
- 화면 문구를 한글로 정리
- 결과 화면에 업로드 대상 선택 추가
  - Firestore
  - Redmine
- 결과 화면에 미리보기 추가
  - Redmine 마크다운
  - Firestore 적재 데이터
- zip 업로드는 메모리에서만 처리하고 업로드 히스토리에는 파일명만 보관하도록 유지
- 환경점검 화면 추가
  - Firebase Hosting
  - Firestore 다운로드
  - Firestore 업로드
  - Redmine
- Firebase Functions 프록시 확장
  - `sync-import`
  - `redmine-health`
  - `redmine-issues`
- README와 아키텍처/Firebase 문서 갱신

## 현재 검증
- `flutter analyze` 통과
- `flutter test` 통과

## 배포 메모
- Hosting/Functions는 `kani-projects` 기준으로 배포
- Windows 데스크톱 빌드는 프로젝트 드라이브가 `NTFS`일 때만 정상 동작
- 현재 저장소가 `exFAT`에 있으면 `flutter build windows`는 symlink 단계에서 실패

## 다음 단계
- `flutter build web` 후 Hosting/Functions 재배포
- 사용자에게 웹 URL 전달
- 사용자 UI 점검 피드백 반영
- 승인 후 Windows/Ubuntu 배포 자동화로 이동
