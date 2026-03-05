from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Literal, Optional

from pydantic import BaseModel, Field


class ToolStatus(BaseModel):
    id: str
    name: str
    available: bool
    discovered_path: Optional[str] = None
    command_template: str
    reason: Optional[str] = None


class CheckItem(BaseModel):
    key: str
    title: str
    status: Literal["ok", "warn", "error"]
    message: str
    details: Dict[str, Any] = Field(default_factory=dict)


class EnvironmentReport(BaseModel):
    checked_at: datetime
    summary: Dict[str, int]
    items: List[CheckItem]
    tools: List[ToolStatus]


class StartJobRequest(BaseModel):
    tool_id: str
    serial: str = ""
    extra_args: List[str] = Field(default_factory=list)


class AdbDevice(BaseModel):
    serial: str
    state: str
    details: Dict[str, str] = Field(default_factory=dict)


class JobInputRequest(BaseModel):
    value: str


class JobSnapshot(BaseModel):
    id: str
    tool_id: str
    serial: str
    command: List[str]
    status: Literal["queued", "running", "success", "failed", "canceled"]
    created_at: datetime
    started_at: Optional[datetime] = None
    finished_at: Optional[datetime] = None
    exit_code: Optional[int] = None
    message: Optional[str] = None


class ReportUploadRequest(BaseModel):
    test_name: str
    device_serial: str
    summary: str
    result: Literal["PASS", "FAIL", "BLOCKED", "NOT_RUN"]
    issue_links: List[str] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class IntegrationUploadResult(BaseModel):
    target: str
    success: bool
    message: str
