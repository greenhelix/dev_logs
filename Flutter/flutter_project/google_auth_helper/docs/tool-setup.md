# 인증 도구 경로 설정 가이드 (Ubuntu)

## 핵심 답변
네, **path(경로) 설정이 맞습니다**.  
`.env`의 `CTS_TOOL_PATH`, `GTS_TOOL_PATH` 등 변수에 각 도구 경로를 지정하면 됩니다.

## 1) 권장 디렉터리 구조 예시
```text
/opt/android-tests/
  cts/
    bin/cts-tradefed
  gts/
    bin/gts-tradefed
  tvts/
    bin/tvts-tradefed
  vts/
    bin/vts-tradefed
  sts/
    bin/sts-tradefed
  cts-on-gsi/
    bin/cts-tradefed
```

## 2) `.env` 설정 예시
```env
CTS_TOOL_PATH=/opt/android-tests/cts
GTS_TOOL_PATH=/opt/android-tests/gts
TVTS_TOOL_PATH=/opt/android-tests/tvts
VTS_TOOL_PATH=/opt/android-tests/vts
STS_TOOL_PATH=/opt/android-tests/sts
CTS_ON_GSI_TOOL_PATH=/opt/android-tests/cts-on-gsi
```

## 3) 경로 규칙
- 파일 경로를 직접 넣어도 됨: `/opt/android-tests/cts/bin/cts-tradefed`
- 디렉터리 경로를 넣어도 됨: `/opt/android-tests/cts`
- 디렉터리 경로일 경우 실행파일 탐색 순서:
- `<tool_path>/<명령어>`
- `<tool_path>/bin/<명령어>`
- `<tool_path>/tools/<명령어>`

## 4) 실행 전 확인
```bash
ls -l /opt/android-tests/cts/bin/cts-tradefed
ls -l /opt/android-tests/gts/bin/gts-tradefed
```

필요 시 실행권한:
```bash
chmod +x /opt/android-tests/cts/bin/cts-tradefed
chmod +x /opt/android-tests/gts/bin/gts-tradefed
```

## 5) 앱에서 확인
- 웹 UI `도구 상태` 메뉴에서 각 도구가 `정상`으로 표시되는지 확인
- API로 확인: `GET /api/tools`

## 6) 참고
- `.env`에 경로를 지정하지 않으면 기본 경로(`/opt/android-tests/...`, `/usr/local/android-tests/...`)를 탐색합니다.
- `adb`가 없으면 펌웨어 업로드 기능은 비활성/실패 처리됩니다.

