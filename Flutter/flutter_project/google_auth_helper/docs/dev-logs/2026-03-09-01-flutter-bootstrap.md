# 2026-03-09 / 01-flutter-bootstrap

## 목표
- Flutter + Firebase 우선 구조를 새 저장소 기준으로 재구성
- 확장성 우선 원칙과 대시보드 중심 UI 원칙을 문서와 코드에 반영

## 오늘 반영한 대원칙
- 모든 것은 확장성을 고려해서 작성
- `GAH-Code-Repo`와 유사한 반응형 정보 구조 채택
- 그래프 화면, 즉 대시보드를 메인 홈으로 고정
- 단계별로 README와 개발로그를 계속 업데이트

## 환경 확인
- `flutter` 없음
- `dart` 없음
- `node`: `v24.14.0`
- `npm`: `11.9.0`
- `firebase.cmd`: `15.8.0`

## 구현 범위
- README 재작성
- 아키텍처 문서 작성
- Windows / Ubuntu / Firebase 설정 문서 추가
- Firebase Hosting / Functions 설정 파일 추가
- Flutter 수동 스캐폴드 준비

## 메모
- 네이티브 runner 생성은 Flutter SDK 설치 후 `scripts/bootstrap_flutter_project.*`로 이어간다.
- 현재 구현은 Flutter 소스와 Firebase 프록시 뼈대를 우선 만드는 단계다.

