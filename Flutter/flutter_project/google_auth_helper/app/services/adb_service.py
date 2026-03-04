from __future__ import annotations

import asyncio
import shutil
from pathlib import Path
from typing import Dict, Optional


class AdbService:
    async def push_firmware(
        self,
        local_path: Path,
        remote_path: str,
        serial: Optional[str] = None,
    ) -> Dict[str, str | int]:
        if not local_path.exists():
            raise FileNotFoundError(f"파일을 찾을 수 없습니다: {local_path}")

        adb_path = shutil.which("adb")
        if not adb_path:
            raise RuntimeError("adb 명령을 찾을 수 없습니다.")

        command = [adb_path]
        if serial:
            command.extend(["-s", serial])
        command.extend(["push", str(local_path), remote_path])

        process = await asyncio.create_subprocess_exec(
            *command,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
        )
        stdout, _ = await process.communicate()
        output = stdout.decode("utf-8", errors="replace")

        return {"exit_code": process.returncode or 0, "output": output}
