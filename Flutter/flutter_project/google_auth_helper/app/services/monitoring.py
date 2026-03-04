from __future__ import annotations

from datetime import datetime
from typing import Any, Dict

from app.services.env_check import EnvironmentChecker
from app.services.job_runner import JobRunner
from app.services.tool_registry import ToolRegistry


class MonitoringService:
    def __init__(
        self,
        registry: ToolRegistry,
        env_checker: EnvironmentChecker,
        job_runner: JobRunner,
    ) -> None:
        self.registry = registry
        self.env_checker = env_checker
        self.job_runner = job_runner

    def build_summary(self) -> Dict[str, Any]:
        report = self.env_checker.run()
        jobs = self.job_runner.list_jobs()
        running_jobs = [job for job in jobs if job.status == "running"]
        failed_jobs = [job for job in jobs if job.status == "failed"]

        available_tools = [tool for tool in report.tools if tool.available]
        unavailable_tools = [tool for tool in report.tools if not tool.available]

        return {
            "generated_at": datetime.utcnow().isoformat(),
            "platform_focus": "ubuntu-linux",
            "environment": report.summary,
            "tools": {
                "available": len(available_tools),
                "unavailable": len(unavailable_tools),
                "details": [tool.model_dump() for tool in report.tools],
            },
            "jobs": {
                "total": len(jobs),
                "running": len(running_jobs),
                "failed": len(failed_jobs),
                "latest": [job.model_dump() for job in jobs[:20]],
            },
        }
