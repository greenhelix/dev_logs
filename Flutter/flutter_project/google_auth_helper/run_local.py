from __future__ import annotations

import argparse
import threading
import time
import webbrowser

import uvicorn


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Google Auth Helper 로컬 실행기")
    parser.add_argument("--host", default="127.0.0.1", help="서버 바인드 호스트")
    parser.add_argument("--port", default=8000, type=int, help="서버 포트")
    parser.add_argument(
        "--no-browser",
        action="store_true",
        help="브라우저 자동 오픈을 비활성화합니다.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    url = f"http://{args.host}:{args.port}"

    if not args.no_browser:
        # 서버 기동 직후 브라우저를 열기 위해 짧게 지연 후 실행한다.
        def _open_browser() -> None:
            time.sleep(1.2)
            webbrowser.open(url)

        threading.Thread(target=_open_browser, daemon=True).start()

    uvicorn.run("app.main:app", host=args.host, port=args.port)


if __name__ == "__main__":
    main()
