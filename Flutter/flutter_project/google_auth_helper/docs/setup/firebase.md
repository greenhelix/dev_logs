# Firebase Setup

## 목표
- Firebase Hosting + Functions + Firestore 구성을 고정합니다.
- 웹에서도 결과 zip 업로드와 Firestore 업로드를 테스트할 수 있게 합니다.
- Redmine 연결 상태와 업로드도 Hosting 뒤 프록시를 통해 점검할 수 있게 합니다.

## 고정값
- Firebase project: `kani-projects`
- Firestore database ID: `google-auth`
- Hosting URL: `https://kani-projects.web.app`

## Web API
- `GET /api/health`
- `GET /api/test-cases`
- `GET /api/failed-tests`
- `GET /api/test-metrics`
- `POST /api/upload-health`
- `POST /api/sync-import`
- `POST /api/redmine-health`
- `POST /api/redmine-issues`

## 역할
- Hosting
  - Flutter 웹 앱 배포
- Functions
  - Firestore 조회 프록시
  - Firestore 업로드 프록시
  - Redmine 상태 확인 프록시
  - Redmine 이슈 생성 프록시
- Firestore
  - `TestCases`
  - `FailedTests`
  - `TestMetrics`

## CLI 준비
```bash
firebase login
firebase use kani-projects
```

## 배포 명령
```bash
flutter build web
cd functions
npm install
cd ..
firebase deploy --only hosting,functions --force
```

## 확인 항목
- `https://kani-projects.web.app`
- `https://kani-projects.web.app/api/health`
- 결과 화면에서 zip 업로드 후 Firestore 업로드 가능 여부
- 환경점검 화면에서 Hosting/Firestore/Redmine 상태 확인 가능 여부

## 보안 주의
- 서비스 계정 JSON은 저장소에 커밋하지 않습니다.
- Redmine API Key는 개발/운영 환경에서 별도 관리해야 합니다.
- 웹은 민감 정보를 영구 저장하지 않는 방향을 유지합니다.
