from __future__ import annotations

import asyncio
import shutil
from pathlib import Path
from typing import Dict, List, Optional

from app.models import AdbDevice


class AdbService:
    async def list_devices(self) -> List[AdbDevice]:
        adb_path = shutil.which("adb")
        if not adb_path:
            raise RuntimeError("adb 명령을 찾을 수 없습니다.")

        process = await asyncio.create_subprocess_exec(
            adb_path,
            "devices",
            "-l",
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
        )
        stdout, _ = await process.communicate()
        output = stdout.decode("utf-8", errors="replace")
        if process.returncode not in {0, None}:
            raise RuntimeError(f"adb devices 실행 실패: {output.strip()}")

        devices: List[AdbDevice] = []
        for raw_line in output.splitlines():
            line = raw_line.strip()
            if not line or line.startswith("List of devices attached"):
                continue
            if line.startswith("* ") or line.lower().startswith("adb server"):
                continue
            parts = line.split()
            if len(parts) < 2:
                continue
            serial = parts[0]
            state = parts[1]
            details: Dict[str, str] = {}
            for token in parts[2:]:
                if ":" not in token:
                    continue
                key, value = token.split(":", maxsplit=1)
                details[key] = value
            devices.append(AdbDevice(serial=serial, state=state, details=details))
        return devices

    async def pick_default_serial(self) -> str | None:
        devices = await self.list_devices()
        for item in devices:
            if item.state == "device":
                return item.serial
        return None

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
