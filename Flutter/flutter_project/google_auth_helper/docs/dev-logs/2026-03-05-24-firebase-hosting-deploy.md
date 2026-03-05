# 2026-03-05 / 24-firebase-hosting-deploy

## 목표
- Firebase Hosting(`kani-projects.web.app`)에 현재 UI 배포

## 완료 체크리스트
- [x] Hosting 설정 파일(`firebase.json`, `.firebaserc`) 추가
- [x] Hosting 배포 준비 스크립트(`prepare_firebase_hosting.ps1`) 추가
- [x] API Base URL 설정 UI 추가(Hosting 환경 대응)
- [x] Firebase Hosting live 배포 성공

## 구현 내용
- 정적 배포 경로: `deploy/firebase/public`
- 배포 파일: `index.html`, `static/app.js`, `static/styles.css`
- `firebase.cmd deploy --only hosting --project kani-projects` 실행

## 검증 결과
- Deploy complete
- Hosting URL: `https://kani-projects.web.app`
- `Invoke-WebRequest https://kani-projects.web.app` -> `200`

## 다음 단계
- Hosting에서 API Base URL을 Ubuntu API 주소로 저장 후 기능 검증
