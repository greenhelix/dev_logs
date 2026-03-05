from __future__ import annotations

import asyncio
import os
from collections import deque
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Deque, Dict, List

from app.config import Settings
from app.services.report_parser import ReportParser
from app.services.result_store import ResultStore
from app.services.tool_registry import ToolRegistry


@dataclass
class WatchTarget:
    tool_id: str
    results_dir: Path | None
    logs_dir: Path | None
    tool_exec: Path | None


class ResultWatcher:
    def __init__(
        self,
        settings: Settings,
        registry: ToolRegistry,
        parser: ReportParser,
        store: ResultStore,
    ) -> None:
        self.settings = settings
        self.registry = registry
        self.parser = parser
        self.store = store

        self._task: asyncio.Task[None] | None = None
        self._stop_event = asyncio.Event()
        self._seen_signatures: Dict[str, tuple[int, int]] = {}

        self._last_scan_at: str | None = None
        self._last_error: str | None = None
        self._imported_total = 0
        self._last_imported: Dict[str, str | int] | None = None
        self._targets_cache: List[WatchTarget] = []
        self._enabled = bool(settings.result_watcher_enabled)
        self._events: Deque[Dict[str, Any]] = deque(maxlen=300)

    async def start(self) -> None:
        if not self._enabled:
            return
        if self._task and not self._task.done():
            return
        self._stop_event.clear()
        self._task = asyncio.create_task(self._run_loop())

    async def stop(self) -> None:
        self._stop_event.set()
        if not self._task:
            return
        await self._task
        self._task = None

    async def set_enabled(self, enabled: bool) -> None:
        self._enabled = bool(enabled)
        if self._enabled:
            await self.start()
        else:
            await self.stop()

    def set_enabled_sync(self, enabled: bool) -> None:
        self._enabled = bool(enabled)

    def scan_now(self) -> None:
        self._scan_once()
        self._last_scan_at = datetime.utcnow().isoformat()
        self._last_error = None

    def status(self) -> Dict[str, object]:
        targets = []
        for target in self._targets_cache:
            targets.append(
                {
                    "tool_id": target.tool_id,
                    "results_dir": str(target.results_dir) if target.results_dir else None,
                    "logs_dir": str(target.logs_dir) if target.logs_dir else None,
                    "tool_exec": str(target.tool_exec) if target.tool_exec else None,
                }
            )
        return {
            "enabled": self._enabled,
            "running": bool(self._task and not self._task.done()),
            "interval_sec": max(1, self.settings.result_watcher_interval_sec),
            "scan_limit": max(10, self.settings.result_watcher_scan_limit),
            "last_scan_at": self._last_scan_at,
            "last_error": self._last_error,
            "imported_total": self._imported_total,
            "last_imported": self._last_imported,
            "targets": targets,
            "events_count": len(self._events),
        }

    def list_events(self, limit: int = 50) -> List[Dict[str, Any]]:
        safe_limit = max(1, min(limit, 300))
        items = list(self._events)
        items.reverse()
        return items[:safe_limit]

    async def _run_loop(self) -> None:
        while not self._stop_event.is_set():
            if not self._enabled:
                try:
                    await asyncio.wait_for(self._stop_event.wait(), timeout=1)
                except asyncio.TimeoutError:
                    continue
            try:
                self._scan_once()
                self._last_error = None
            except Exception as exc:
                self._last_error = str(exc)
                self._push_event("error", f"scan loop error: {exc}")
            self._last_scan_at = datetime.utcnow().isoformat()
            try:
                await asyncio.wait_for(
                    self._stop_event.wait(),
                    timeout=max(1, self.settings.result_watcher_interval_sec),
                )
            except asyncio.TimeoutError:
                continue

    def _scan_once(self) -> None:
        self.registry.refresh()
        targets = self._build_targets()
        self._targets_cache = targets
        candidates = self._collect_candidates(targets)
        self._push_event("info", f"scan start: candidates={len(candidates)}")
        for tool_id, file_path in candidates:
            if self._is_seen(tool_id, file_path):
                continue
            try:
                self._import_file(tool_id, file_path)
                self._mark_seen(tool_id, file_path)
            except Exception as exc:
                self._push_event(
                    "error",
                    f"import failed: tool={tool_id}, file={file_path.name}, error={exc}",
                    tool_id=tool_id,
                    source_file=str(file_path),
                )

        # Keep seen cache bounded to avoid unbounded memory growth.
        while len(self._seen_signatures) > 5000:
            oldest_key = next(iter(self._seen_signatures))
            self._seen_signatures.pop(oldest_key, None)

    def _build_targets(self) -> List[WatchTarget]:
        targets: List[WatchTarget] = []
        for status in self.registry.list_status():
            runtime = self.registry.get_runtime(status.id)
            tool_exec = self._resolve_tool_exec(runtime.discovered_path, status.command_template)
            base_dir = self._resolve_base_dir(runtime.discovered_path, tool_exec)
            results_dir = self._resolve_results_dir(status.id, base_dir)
            logs_dir = self._resolve_logs_dir(status.id, base_dir)
            targets.append(
                WatchTarget(
                    tool_id=status.id,
                    results_dir=results_dir,
                    logs_dir=logs_dir,
                    tool_exec=tool_exec,
                )
            )
        return targets

    def _collect_candidates(self, targets: List[WatchTarget]) -> List[tuple[str, Path]]:
        pairs: List[tuple[str, Path]] = []
        limit = max(10, self.settings.result_watcher_scan_limit)
        for target in targets:
            if target.results_dir is None or not target.results_dir.exists():
                continue
            if not target.results_dir.is_dir():
                continue
            for path in target.results_dir.rglob("*"):
                if not path.is_file():
                    continue
                suffix = path.suffix.lower()
                if suffix not in {".xml", ".html", ".htm"}:
                    continue
                pairs.append((target.tool_id, path))
        pairs.sort(key=self._safe_mtime, reverse=True)
        return pairs[:limit]

    def _import_file(self, tool_id: str, file_path: Path) -> None:
        content = file_path.read_bytes()
        parsed_report = self.parser.parse(file_name=file_path.name, content=content, source_type="auto")
        payload = parsed_report.to_dict()
        payload.setdefault("metadata", {})
        payload["metadata"]["watcher_tool_id"] = tool_id
        run_id = self.store.save_report(
            source_file=str(file_path),
            parsed=payload,
            imported_at=datetime.utcnow().isoformat(),
        )
        self._imported_total += 1
        self._last_imported = {
            "run_id": run_id,
            "tool_id": tool_id,
            "source_file": str(file_path),
            "imported_at": datetime.utcnow().isoformat(),
        }
        self._push_event(
            "success",
            f"imported: tool={tool_id}, file={file_path.name}, run_id={run_id}",
            tool_id=tool_id,
            source_file=str(file_path),
            run_id=run_id,
        )

    def _is_seen(self, tool_id: str, file_path: Path) -> bool:
        key = f"{tool_id}:{file_path}"
        stat = file_path.stat()
        signature = (stat.st_mtime_ns, stat.st_size)
        return self._seen_signatures.get(key) == signature

    def _mark_seen(self, tool_id: str, file_path: Path) -> None:
        key = f"{tool_id}:{file_path}"
        stat = file_path.stat()
        self._seen_signatures[key] = (stat.st_mtime_ns, stat.st_size)

    def _resolve_tool_exec(self, discovered_path: Path | None, command_template: str) -> Path | None:
        if discovered_path is None:
            return None
        executable_name = command_template.split()[0]
        if discovered_path.is_file():
            return discovered_path
        candidates = [
            discovered_path / executable_name,
            discovered_path / "bin" / executable_name,
            discovered_path / "tools" / executable_name,
        ]
        for candidate in candidates:
            if candidate.exists():
                return candidate
        return None

    def _resolve_base_dir(self, discovered_path: Path | None, tool_exec: Path | None) -> Path | None:
        if discovered_path is not None and discovered_path.is_dir():
            return discovered_path
        if tool_exec is None:
            return None
        parent = tool_exec.parent
        if parent.name in {"tools", "bin"}:
            return parent.parent
        return parent

    def _resolve_results_dir(self, tool_id: str, base_dir: Path | None) -> Path | None:
        env_key = f"{tool_id.upper()}_RESULTS_DIR".replace("-", "_")
        configured_raw = os.getenv(env_key, "").strip()
        configured = Path(configured_raw).expanduser() if configured_raw else None
        if configured:
            return configured
        if base_dir is None:
            return None
        return base_dir / "results"

    def _resolve_logs_dir(self, tool_id: str, base_dir: Path | None) -> Path | None:
        env_key = f"{tool_id.upper()}_LOGS_DIR".replace("-", "_")
        configured_raw = os.getenv(env_key, "").strip()
        configured = Path(configured_raw).expanduser() if configured_raw else None
        if configured:
            return configured
        if base_dir is None:
            return None
        return base_dir / "logs"

    def _safe_mtime(self, item: tuple[str, Path]) -> float:
        try:
            return item[1].stat().st_mtime
        except OSError:
            return 0.0

    def _push_event(
        self,
        level: str,
        message: str,
        tool_id: str | None = None,
        source_file: str | None = None,
        run_id: int | None = None,
    ) -> None:
        self._events.append(
            {
                "time_utc": datetime.utcnow().isoformat(),
                "level": level,
                "message": message,
                "tool_id": tool_id,
                "source_file": source_file,
                "run_id": run_id,
            }
        )
