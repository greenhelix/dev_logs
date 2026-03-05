from __future__ import annotations

import json
from typing import Dict, List
from urllib import error, request

from app.config import Settings
from app.models import IntegrationUploadResult, ReportUploadRequest


class BaseIntegration:
    target = "unknown"

    def enabled(self) -> bool:
        return False

    async def upload(self, report: ReportUploadRequest) -> IntegrationUploadResult:
        return IntegrationUploadResult(
            target=self.target,
            success=False,
            message="구현되지 않은 연동입니다.",
        )


class JiraIntegration(BaseIntegration):
    target = "jira"

    def __init__(self, base_url: str, token: str) -> None:
        self.base_url = base_url
        self.token = token

    def enabled(self) -> bool:
        return bool(self.base_url and self.token)

    async def upload(self, report: ReportUploadRequest) -> IntegrationUploadResult:
        return IntegrationUploadResult(
            target=self.target,
            success=False,
            message="요구사항에 따라 Jira는 현재 미구현(항목만 유지) 상태입니다.",
        )


class RedmineIntegration(BaseIntegration):
    target = "redmine"

    def __init__(self, base_url: str, token: str, project_id: str) -> None:
        self.base_url = base_url
        self.token = token
        self.project_id = project_id

    def enabled(self) -> bool:
        return bool(self.base_url and self.token and self.project_id)

    async def upload(self, report: ReportUploadRequest) -> IntegrationUploadResult:
        if not self.enabled():
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message="Redmine 설정(REDMINE_BASE_URL/REDMINE_TOKEN/REDMINE_PROJECT_ID)이 부족합니다.",
            )
        url = f"{self.base_url.rstrip('/')}/issues.json"
        payload = {
            "issue": {
                "project_id": self.project_id,
                "subject": report.test_name[:255],
                "description": report.summary,
                "notes": "\n".join(report.issue_links)[:5000],
            }
        }
        req = request.Request(
            url=url,
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            method="POST",
            headers={
                "X-Redmine-API-Key": self.token,
                "Content-Type": "application/json",
            },
        )
        try:
            with request.urlopen(req, timeout=20) as response:
                raw = response.read().decode("utf-8", errors="replace")
                body = json.loads(raw) if raw else {}
                issue_id = body.get("issue", {}).get("id")
                return IntegrationUploadResult(
                    target=self.target,
                    success=True,
                    message=f"Redmine 이슈 생성 완료(id={issue_id})",
                )
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message=f"Redmine 업로드 실패({exc.code}): {detail[:300]}",
            )
        except Exception as exc:
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message=f"Redmine 업로드 실패: {exc}",
            )


class NotionIntegration(BaseIntegration):
    target = "notion"

    def __init__(self, token: str, database_id: str, notion_version: str) -> None:
        self.token = token
        self.database_id = database_id
        self.notion_version = notion_version

    def enabled(self) -> bool:
        return bool(self.token and self.database_id)

    async def upload(self, report: ReportUploadRequest) -> IntegrationUploadResult:
        if not self.enabled():
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message="Notion 설정이 없어 업로드하지 않았습니다.",
            )
        url = "https://api.notion.com/v1/pages"
        payload = {
            "parent": {"database_id": self.database_id},
            "properties": {
                "Name": {"title": [{"text": {"content": report.test_name[:200]}}]},
                "Result": {"select": {"name": report.result}},
                "Device": {"rich_text": [{"text": {"content": report.device_serial[:200]}}]},
                "Summary": {"rich_text": [{"text": {"content": report.summary[:1800]}}]},
            },
        }
        req = request.Request(
            url=url,
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            method="POST",
            headers={
                "Authorization": f"Bearer {self.token}",
                "Notion-Version": self.notion_version,
                "Content-Type": "application/json",
            },
        )
        try:
            with request.urlopen(req, timeout=20) as response:
                raw = response.read().decode("utf-8", errors="replace")
                body = json.loads(raw) if raw else {}
                page_id = body.get("id", "")
                return IntegrationUploadResult(
                    target=self.target,
                    success=True,
                    message=f"Notion 페이지 생성 완료(id={page_id})",
                )
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message=f"Notion 업로드 실패({exc.code}): {detail[:300]}",
            )
        except Exception as exc:
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message=f"Notion 업로드 실패: {exc}",
            )


class IntegrationDispatcher:
    def __init__(self, settings: Settings) -> None:
        self.providers = [
            JiraIntegration(settings.jira_base_url, settings.jira_token),
            RedmineIntegration(
                settings.redmine_base_url,
                settings.redmine_token,
                settings.redmine_project_id,
            ),
            NotionIntegration(
                settings.notion_token,
                settings.notion_database_id,
                settings.notion_version,
            ),
        ]

    async def upload_all(self, report: ReportUploadRequest) -> Dict[str, List[IntegrationUploadResult]]:
        results: List[IntegrationUploadResult] = []
        for provider in self.providers:
            result = await provider.upload(report)
            results.append(result)
        return {"results": results}
