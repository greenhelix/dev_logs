## Release Watcher

별도 Python watcher 프로그램입니다. Flutter 앱과 분리해서 Windows 작업 스케줄러에서 실행하는 전제를 가집니다.

요구사항:
- Chrome을 `--remote-debugging-port=9222`로 실행
- 대상 Google 포털에 로그인된 브라우저 세션
- Python 3.11+

산출물:
- `output/latest_snapshot.json`
- `output/diff.json`
- `output/mail_log.json`

권장 스케줄:
- 매일 오전 9시 로컬 시간

예시 실행:
```powershell
python tools/release_watcher/main.py --config tools/release_watcher/config.sample.json
```
