from __future__ import annotations

from typing import Dict, List

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
        if not self.enabled():
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message="Jira 설정이 없어 업로드하지 않았습니다.",
            )
        # 실제 Jira API 연동은 운영 환경에서 필드 매핑 후 추가 구현
        return IntegrationUploadResult(
            target=self.target,
            success=True,
            message=f"Jira 업로드 준비 완료(샘플). test={report.test_name}",
        )


class RedmineIntegration(BaseIntegration):
    target = "redmine"

    def __init__(self, base_url: str, token: str) -> None:
        self.base_url = base_url
        self.token = token

    def enabled(self) -> bool:
        return bool(self.base_url and self.token)

    async def upload(self, report: ReportUploadRequest) -> IntegrationUploadResult:
        if not self.enabled():
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message="Redmine 설정이 없어 업로드하지 않았습니다.",
            )
        # 실제 Redmine API 연동은 운영 환경에서 프로젝트별 필드 정의 필요
        return IntegrationUploadResult(
            target=self.target,
            success=True,
            message=f"Redmine 업로드 준비 완료(샘플). test={report.test_name}",
        )


class NotionIntegration(BaseIntegration):
    target = "notion"

    def __init__(self, token: str, database_id: str) -> None:
        self.token = token
        self.database_id = database_id

    def enabled(self) -> bool:
        return bool(self.token and self.database_id)

    async def upload(self, report: ReportUploadRequest) -> IntegrationUploadResult:
        if not self.enabled():
            return IntegrationUploadResult(
                target=self.target,
                success=False,
                message="Notion 설정이 없어 업로드하지 않았습니다.",
            )
        # 실제 Notion API 연동은 DB 스키마에 맞춘 속성 매핑 필요
        return IntegrationUploadResult(
            target=self.target,
            success=True,
            message=f"Notion 업로드 준비 완료(샘플). test={report.test_name}",
        )


class IntegrationDispatcher:
    def __init__(self, settings: Settings) -> None:
        self.providers = [
            JiraIntegration(settings.jira_base_url, settings.jira_token),
            RedmineIntegration(settings.redmine_base_url, settings.redmine_token),
            NotionIntegration(settings.notion_token, settings.notion_database_id),
        ]

    async def upload_all(self, report: ReportUploadRequest) -> Dict[str, List[IntegrationUploadResult]]:
        results: List[IntegrationUploadResult] = []
        for provider in self.providers:
            result = await provider.upload(report)
            results.append(result)
        return {"results": results}
