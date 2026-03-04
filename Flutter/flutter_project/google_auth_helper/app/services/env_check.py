from __future__ import annotations

import shutil
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List

from app.config import Settings
from app.models import CheckItem, EnvironmentReport
from app.services.tool_registry import ToolRegistry


class EnvironmentChecker:
    def __init__(self, settings: Settings, registry: ToolRegistry) -> None:
        self.settings = settings
        self.registry = registry

    def run(self) -> EnvironmentReport:
        self.registry.refresh()
        checks: List[CheckItem] = []
        checks.append(self._check_python())
        checks.append(self._check_adb())
        checks.extend(self._check_directories())
        checks.extend(self._check_integrations())

        summary: Dict[str, int] = {"ok": 0, "warn": 0, "error": 0}
        for item in checks:
            summary[item.status] += 1

        return EnvironmentReport(
            checked_at=datetime.utcnow(),
            summary=summary,
            items=checks,
            tools=self.registry.list_status(),
        )

    def _check_python(self) -> CheckItem:
        current = f"{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}"
        if sys.version_info >= (3, 11):
            status = "ok"
            message = f"Python 버전이 요구사항을 만족합니다. ({current})"
        else:
            status = "error"
            message = f"Python 3.11+ 필요, 현재 버전: {current}"
        return CheckItem(key="python", title="Python 버전", status=status, message=message)

    def _check_adb(self) -> CheckItem:
        adb_path = shutil.which("adb")
        if adb_path:
            return CheckItem(
                key="adb",
                title="ADB 도구",
                status="ok",
                message="adb 명령을 찾았습니다.",
                details={"path": adb_path},
            )
        return CheckItem(
            key="adb",
            title="ADB 도구",
            status="warn",
            message="adb를 찾지 못했습니다. 펌웨어 업로드 기능이 비활성화됩니다.",
        )

    def _check_directories(self) -> List[CheckItem]:
        results: List[CheckItem] = []
        targets = [
            ("workspace", "작업 디렉터리", self.settings.workspace_dir),
            ("logs", "로그 디렉터리", self.settings.logs_dir),
            ("firmware", "펌웨어 디렉터리", self.settings.firmware_dir),
        ]
        for key, title, path in targets:
            results.append(self._ensure_writable_directory(key, title, path))
        return results

    def _ensure_writable_directory(self, key: str, title: str, path: Path) -> CheckItem:
        try:
            path.mkdir(parents=True, exist_ok=True)
            probe = path / ".permission_probe"
            probe.write_text("ok", encoding="utf-8")
            probe.unlink(missing_ok=True)
            return CheckItem(
                key=key,
                title=title,
                status="ok",
                message="읽기/쓰기 권한이 확인되었습니다.",
                details={"path": str(path)},
            )
        except Exception as exc:
            return CheckItem(
                key=key,
                title=title,
                status="error",
                message=f"디렉터리 권한 확인 실패: {exc}",
                details={"path": str(path)},
            )

    def _check_integrations(self) -> List[CheckItem]:
        items: List[CheckItem] = []
        providers = [
            ("jira", "Jira 연동", bool(self.settings.jira_base_url and self.settings.jira_token)),
            (
                "redmine",
                "Redmine 연동",
                bool(self.settings.redmine_base_url and self.settings.redmine_token),
            ),
            (
                "notion",
                "Notion 연동",
                bool(self.settings.notion_token and self.settings.notion_database_id),
            ),
        ]
        for key, title, enabled in providers:
            if enabled:
                items.append(
                    CheckItem(
                        key=key,
                        title=title,
                        status="ok",
                        message="연동 설정이 준비되었습니다.",
                    )
                )
            else:
                items.append(
                    CheckItem(
                        key=key,
                        title=title,
                        status="warn",
                        message="설정이 없어 업로드 기능이 비활성화됩니다.",
                    )
                )
        return items
