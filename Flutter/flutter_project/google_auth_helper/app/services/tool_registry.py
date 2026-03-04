from __future__ import annotations

import os
import shlex
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List

from app.config import Settings, ToolSpec
from app.models import ToolStatus


@dataclass
class ToolRuntime:
    spec: ToolSpec
    available: bool
    discovered_path: Path | None = None
    reason: str | None = None


class ToolRegistry:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._tools: Dict[str, ToolRuntime] = {}
        self.refresh()

    def refresh(self) -> None:
        tools: Dict[str, ToolRuntime] = {}
        for spec in self.settings.tools:
            runtime = self._discover(spec)
            tools[spec.id] = runtime
        self._tools = tools

    def _discover(self, spec: ToolSpec) -> ToolRuntime:
        configured = os.getenv(spec.env_key, "").strip()
        candidates: List[Path] = []
        if configured:
            candidates.append(Path(configured).expanduser())
        for item in spec.default_paths:
            candidates.append(Path(item).expanduser())

        for path in candidates:
            if path.exists():
                return ToolRuntime(spec=spec, available=True, discovered_path=path.resolve())

        command_binary = shlex.split(spec.command_template)[0]
        discovered_binary = shutil.which(command_binary)
        if discovered_binary:
            return ToolRuntime(
                spec=spec,
                available=True,
                discovered_path=Path(discovered_binary).resolve(),
            )
        return ToolRuntime(
            spec=spec,
            available=False,
            reason=f"{spec.env_key} 또는 기본 경로에서 찾지 못했습니다.",
        )

    def list_status(self) -> List[ToolStatus]:
        result: List[ToolStatus] = []
        for runtime in self._tools.values():
            result.append(
                ToolStatus(
                    id=runtime.spec.id,
                    name=runtime.spec.name,
                    available=runtime.available,
                    discovered_path=str(runtime.discovered_path) if runtime.discovered_path else None,
                    command_template=runtime.spec.command_template,
                    reason=runtime.reason,
                )
            )
        return result

    def get_runtime(self, tool_id: str) -> ToolRuntime:
        runtime = self._tools.get(tool_id)
        if not runtime:
            raise KeyError(f"알 수 없는 도구 ID: {tool_id}")
        return runtime

    def build_command(self, tool_id: str, serial: str, extra_args: List[str]) -> List[str]:
        runtime = self.get_runtime(tool_id)
        if not runtime.available:
            raise RuntimeError(f"{runtime.spec.name} 도구가 비활성화 상태입니다.")

        command = runtime.spec.command_template.format(serial=serial)
        base = shlex.split(command)
        executable = base[0]
        resolved_exec = self._resolve_tool_executable(
            discovered_path=runtime.discovered_path,
            executable_name=executable,
        )
        base[0] = resolved_exec
        return base + extra_args

    def _resolve_tool_executable(self, discovered_path: Path | None, executable_name: str) -> str:
        if discovered_path is None:
            return executable_name

        if discovered_path.is_file():
            return str(discovered_path)

        candidates = [
            discovered_path / executable_name,
            discovered_path / "bin" / executable_name,
            discovered_path / "tools" / executable_name,
        ]
        for candidate in candidates:
            if candidate.exists():
                return str(candidate)
        return executable_name
