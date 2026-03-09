# Firebase Setup

## 고정값
- Firebase Project: `kani-projects`
- Firestore database ID: `google-auth`
- Hosting URL: `https://kani-projects.web.app`

## 1. CLI 준비
공식 문서: https://firebase.google.com/docs/cli

```powershell
firebase.cmd login
firebase.cmd use kani-projects
```

또는 Ubuntu:
```bash
firebase login
firebase use kani-projects
```

## 2. Hosting / Functions 배포
이 저장소는 아래 경로를 기준으로 한다.
- Hosting public: `build/web`
- Functions source: `functions`

배포 예시:
```powershell
flutter build web
firebase.cmd deploy --project kani-projects
```

## 3. Read-only Web Proxy
Web은 Firestore에 직접 쓰지 않는다.

노출 API:
- `GET /api/test-cases`
- `GET /api/failed-tests`
- `GET /api/test-metrics`

구현 위치:
- `functions/index.js`

## 4. Desktop Credential 전략
- `serviceAccountFile`
  - 로컬 JSON 파일 경로를 Settings에 저장
  - 파일 자체는 저장소에 커밋하지 않음
- `localToken`
  - Firebase CLI login 상태의 access token 사용
  - 기본 탐색 파일:
    - Windows: `%APPDATA%\\configstore\\firebase-tools.json`
    - Ubuntu: `~/.config/configstore/firebase-tools.json`

## 5. 보안 원칙
- 서비스 계정 파일은 `.gitignore` 대상이다.
- Web 클라이언트에는 privileged credential을 넣지 않는다.
- 쓰기 권한은 Desktop 앱 또는 서버 측 proxy로만 수행한다.

## 6. Named Database 참고
- Admin SDK named database reference:
  - https://firebase.google.com/docs/reference/admin/node/firebase-admin.firestore

