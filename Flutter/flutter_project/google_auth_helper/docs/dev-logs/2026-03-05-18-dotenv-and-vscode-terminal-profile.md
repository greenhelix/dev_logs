# 2026-03-05 / 18-dotenv-and-vscode-terminal-profile

## 목표
- `.env` 값이 서버에 실제 반영되도록 설정 로더 보강
- VSCode 터미널 기본 프로필에서 Node/Firebase 명령 인식 보장

## 완료 체크리스트
- [x] `load_settings()`에서 `.env` 자동 로드 적용
- [x] `requirements.txt`에 `python-dotenv` 명시
- [x] 워크스페이스 `.vscode/settings.json`에 터미널 프로필 설정 추가
- [x] `/api/firebase/status`로 `.env` 반영 확인

## 구현 내용
- `app/config.py`: `load_dotenv()` 적용
- `.vscode/settings.json` 생성:
- PowerShell `-ExecutionPolicy Bypass`
- PATH에 `nodejs`, `Roaming\\npm` 선반영

## 검증 결과
- `/api/firebase/status` 응답:
- `project_id=kani-projects`
- `database_id=google-auth`
- 서비스계정 파일 미존재 오류 메시지로 현재 차단 지점 명확화

## 다음 단계
- 서비스계정 JSON 파일을 실제 경로에 배치
- `ok=true` 확인 후 Firestore 동기화 API 실행 검증
