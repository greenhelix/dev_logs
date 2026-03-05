# 2026-03-05 / 11-firestore-sync-and-repo-record

## 목표
- GitHub 코드/릴리즈 저장소 위치를 공식 문서에 고정 기록
- 로컬 저장 결과를 Firestore로 동기화하는 API 추가

## 완료 체크리스트
- [x] 코드/릴리즈 repo 정보 README/dev_guide/.env.example 반영
- [x] Firestore upsert 동작 추가
- [x] Firestore 동기화 API(`/api/firebase/sync/runs`) 추가

## 구현 내용
- `docs/dev_guide.md`, `README.md`, `.env.example`에 repo 경로 기록
- `FirestoreService.upsert_document()` 추가
- 최근 run 목록을 Firestore 컬렉션에 upsert하는 API 추가

## 검증 결과
- `python -m compileall app run_local.py` 통과

## 다음 단계
- Firestore 인증 토큰 자동 갱신 방식(서비스 계정) 적용
- 대시보드 실데이터를 Firestore 읽기 경로와 연결
