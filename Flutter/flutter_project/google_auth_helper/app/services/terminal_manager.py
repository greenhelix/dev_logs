from __future__ import annotations

import asyncio
import os

from fastapi import WebSocket, WebSocketDisconnect


class TerminalManager:
    async def interactive_shell(self, websocket: WebSocket) -> None:
        await websocket.accept()

        if os.name == "nt":
            shell = ["powershell", "-NoLogo"]
        else:
            shell = ["/bin/bash", "-i"]

        process = await asyncio.create_subprocess_exec(
            *shell,
            stdin=asyncio.subprocess.PIPE,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
        )

        reader_task = asyncio.create_task(self._read_loop(websocket, process))

        try:
            while True:
                text = await websocket.receive_text()
                if text == "__exit__":
                    break
                if process.stdin:
                    process.stdin.write((text + "\n").encode("utf-8"))
                    await process.stdin.drain()
        except WebSocketDisconnect:
            pass
        finally:
            if process.returncode is None:
                process.terminate()
                try:
                    await asyncio.wait_for(process.wait(), timeout=2)
                except asyncio.TimeoutError:
                    process.kill()
                    await process.wait()
            reader_task.cancel()

    async def _read_loop(
        self,
        websocket: WebSocket,
        process: asyncio.subprocess.Process,
    ) -> None:
        if not process.stdout:
            return
        while True:
            line = await process.stdout.readline()
            if not line:
                break
            text = line.decode("utf-8", errors="replace")
            await websocket.send_text(text)
