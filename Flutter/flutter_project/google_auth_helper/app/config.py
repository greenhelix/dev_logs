from __future__ import annotations

import os
from dataclasses import dataclass, field
from pathlib import Path
from typing import List


def env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class ToolSpec:
    id: str
    name: str
    env_key: str
    default_paths: List[str]
    command_template: str


@dataclass(frozen=True)
class Settings:
    host: str = field(default_factory=lambda: os.getenv("APP_HOST", "0.0.0.0"))
    port: int = field(default_factory=lambda: int(os.getenv("APP_PORT", "8000")))
    workspace_dir: Path = field(
        default_factory=lambda: Path(os.getenv("WORKSPACE_DIR", "./workspace")).resolve()
    )
    logs_dir: Path = field(
        default_factory=lambda: Path(os.getenv("LOGS_DIR", "./workspace/logs")).resolve()
    )
    firmware_dir: Path = field(
        default_factory=lambda: Path(os.getenv("FIRMWARE_DIR", "./workspace/firmware")).resolve()
    )
    default_remote_firmware_path: str = field(
        default_factory=lambda: os.getenv("DEFAULT_REMOTE_FIRMWARE_PATH", "/data/local/tmp")
    )
    jira_base_url: str = field(default_factory=lambda: os.getenv("JIRA_BASE_URL", ""))
    jira_token: str = field(default_factory=lambda: os.getenv("JIRA_TOKEN", ""))
    redmine_base_url: str = field(default_factory=lambda: os.getenv("REDMINE_BASE_URL", ""))
    redmine_token: str = field(default_factory=lambda: os.getenv("REDMINE_TOKEN", ""))
    redmine_project_id: str = field(default_factory=lambda: os.getenv("REDMINE_PROJECT_ID", ""))
    notion_token: str = field(default_factory=lambda: os.getenv("NOTION_TOKEN", ""))
    notion_database_id: str = field(default_factory=lambda: os.getenv("NOTION_DATABASE_ID", ""))
    notion_version: str = field(default_factory=lambda: os.getenv("NOTION_VERSION", "2022-06-28"))
    monitor_api_token: str = field(default_factory=lambda: os.getenv("MONITOR_API_TOKEN", ""))
    firebase_backend: str = field(default_factory=lambda: os.getenv("FIREBASE_BACKEND", "firestore"))
    firebase_project_id: str = field(default_factory=lambda: os.getenv("FIREBASE_PROJECT_ID", ""))
    firebase_api_key: str = field(default_factory=lambda: os.getenv("FIREBASE_API_KEY", ""))
    firebase_bearer_token: str = field(
        default_factory=lambda: os.getenv("FIREBASE_BEARER_TOKEN", "")
    )
    firebase_service_account_file: str = field(
        default_factory=lambda: os.getenv("FIREBASE_SERVICE_ACCOUNT_FILE", "")
    )
    firebase_hosting_url: str = field(
        default_factory=lambda: os.getenv("FIREBASE_HOSTING_URL", "https://kani-projects.web.app")
    )
    firestore_default_collection: str = field(
        default_factory=lambda: os.getenv("FIRESTORE_DEFAULT_COLLECTION", "google-auth")
    )
    firestore_database_id: str = field(
        default_factory=lambda: os.getenv("FIRESTORE_DATABASE_ID", "(default)")
    )
    result_watcher_enabled: bool = field(
        default_factory=lambda: env_bool("RESULT_WATCHER_ENABLED", True)
    )
    result_watcher_interval_sec: int = field(
        default_factory=lambda: int(os.getenv("RESULT_WATCHER_INTERVAL_SEC", "15"))
    )
    result_watcher_scan_limit: int = field(
        default_factory=lambda: int(os.getenv("RESULT_WATCHER_SCAN_LIMIT", "300"))
    )
    allowed_origins: List[str] = field(
        default_factory=lambda: [
            origin.strip()
            for origin in os.getenv("ALLOWED_ORIGINS", "*").split(",")
            if origin.strip()
        ]
    )
    tools: List[ToolSpec] = field(
        default_factory=lambda: [
            ToolSpec(
                id="cts",
                name="CTS",
                env_key="CTS_TOOL_PATH",
                default_paths=["/opt/android-tests/cts", "/usr/local/android-tests/cts"],
                command_template="cts-tradefed run cts --serial {serial}",
            ),
            ToolSpec(
                id="gts",
                name="GTS",
                env_key="GTS_TOOL_PATH",
                default_paths=["/opt/android-tests/gts", "/usr/local/android-tests/gts"],
                command_template="gts-tradefed run gts --serial {serial}",
            ),
            ToolSpec(
                id="tvts",
                name="TVTS",
                env_key="TVTS_TOOL_PATH",
                default_paths=["/opt/android-tests/tvts", "/usr/local/android-tests/tvts"],
                command_template="tvts-tradefed run tvts --serial {serial}",
            ),
            ToolSpec(
                id="vts",
                name="VTS",
                env_key="VTS_TOOL_PATH",
                default_paths=["/opt/android-tests/vts", "/usr/local/android-tests/vts"],
                command_template="vts-tradefed run vts --serial {serial}",
            ),
            ToolSpec(
                id="sts",
                name="STS",
                env_key="STS_TOOL_PATH",
                default_paths=["/opt/android-tests/sts", "/usr/local/android-tests/sts"],
                command_template="sts-tradefed run sts --serial {serial}",
            ),
            ToolSpec(
                id="cts_on_gsi",
                name="CTS-on-GSI",
                env_key="CTS_ON_GSI_TOOL_PATH",
                default_paths=[
                    "/opt/android-tests/cts-on-gsi",
                    "/usr/local/android-tests/cts-on-gsi",
                ],
                command_template="cts-tradefed run cts-on-gsi --serial {serial}",
            ),
        ]
    )


def load_settings() -> Settings:
    try:
        from dotenv import load_dotenv

        load_dotenv()
    except Exception:
        # Keep running even if python-dotenv is not installed.
        pass
    return Settings()
