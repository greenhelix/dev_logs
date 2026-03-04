from __future__ import annotations

import json
import sqlite3
from pathlib import Path
from typing import Any, Dict, List


class ResultStore:
    def __init__(self, db_path: Path) -> None:
        self.db_path = db_path
        self._initialized = False

    def initialize(self) -> None:
        if self._initialized:
            return
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS test_runs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source_file TEXT NOT NULL,
                    source_type TEXT NOT NULL,
                    imported_at TEXT NOT NULL,
                    firmware_version TEXT,
                    tool_version TEXT,
                    elapsed_time TEXT,
                    total_count INTEGER NOT NULL,
                    pass_count INTEGER NOT NULL,
                    fail_count INTEGER NOT NULL,
                    metadata_json TEXT NOT NULL
                )
                """
            )
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS test_cases (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    run_id INTEGER NOT NULL,
                    module_name TEXT NOT NULL,
                    testcase_name TEXT NOT NULL,
                    result TEXT NOT NULL,
                    details TEXT,
                    FOREIGN KEY(run_id) REFERENCES test_runs(id) ON DELETE CASCADE
                )
                """
            )
            conn.commit()
        self._initialized = True

    def save_report(self, source_file: str, parsed: Dict[str, Any], imported_at: str) -> int:
        self.initialize()
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO test_runs (
                    source_file,
                    source_type,
                    imported_at,
                    firmware_version,
                    tool_version,
                    elapsed_time,
                    total_count,
                    pass_count,
                    fail_count,
                    metadata_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    source_file,
                    parsed.get("source_type", "unknown"),
                    imported_at,
                    parsed.get("firmware_version", "UNKNOWN"),
                    parsed.get("tool_version", "UNKNOWN"),
                    parsed.get("elapsed_time", "UNKNOWN"),
                    int(parsed.get("total_count", 0)),
                    int(parsed.get("pass_count", 0)),
                    int(parsed.get("fail_count", 0)),
                    json.dumps(parsed.get("metadata", {}), ensure_ascii=False),
                ),
            )
            run_id = int(cursor.lastrowid)

            cases = parsed.get("cases", [])
            for case in cases:
                cursor.execute(
                    """
                    INSERT INTO test_cases (
                        run_id,
                        module_name,
                        testcase_name,
                        result,
                        details
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    (
                        run_id,
                        str(case.get("module", "unknown_module")),
                        str(case.get("testcase", "unknown_case")),
                        str(case.get("result", "NOT_RUN")),
                        str(case.get("details", "")),
                    ),
                )
            conn.commit()
            return run_id

    def list_runs(self, limit: int = 50) -> List[Dict[str, Any]]:
        self.initialize()
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            rows = conn.execute(
                """
                SELECT
                    id,
                    source_file,
                    source_type,
                    imported_at,
                    firmware_version,
                    tool_version,
                    elapsed_time,
                    total_count,
                    pass_count,
                    fail_count
                FROM test_runs
                ORDER BY id DESC
                LIMIT ?
                """,
                (limit,),
            ).fetchall()
            return [dict(row) for row in rows]

    def get_run(self, run_id: int) -> Dict[str, Any] | None:
        self.initialize()
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            run = conn.execute(
                """
                SELECT
                    id,
                    source_file,
                    source_type,
                    imported_at,
                    firmware_version,
                    tool_version,
                    elapsed_time,
                    total_count,
                    pass_count,
                    fail_count,
                    metadata_json
                FROM test_runs
                WHERE id = ?
                """,
                (run_id,),
            ).fetchone()
            if run is None:
                return None

            case_rows = conn.execute(
                """
                SELECT
                    module_name,
                    testcase_name,
                    result,
                    details
                FROM test_cases
                WHERE run_id = ?
                ORDER BY module_name, testcase_name
                """,
                (run_id,),
            ).fetchall()

            payload = dict(run)
            payload["metadata"] = json.loads(payload.pop("metadata_json", "{}"))
            payload["cases"] = [dict(row) for row in case_rows]
            return payload

    def build_analytics_payload(self) -> Dict[str, Any]:
        self.initialize()
        with sqlite3.connect(self.db_path) as conn:
            conn.row_factory = sqlite3.Row
            by_firmware = conn.execute(
                """
                SELECT firmware_version, COUNT(*) AS run_count, SUM(fail_count) AS fail_sum
                FROM test_runs
                GROUP BY firmware_version
                ORDER BY run_count DESC
                LIMIT 10
                """
            ).fetchall()

            monthly_upload = conn.execute(
                """
                SELECT
                    substr(imported_at, 1, 7) AS month_key,
                    COUNT(*) AS upload_count
                FROM test_runs
                GROUP BY month_key
                ORDER BY month_key
                LIMIT 12
                """
            ).fetchall()

        return {
            "firmware_summary": [dict(row) for row in by_firmware],
            "monthly_upload": [dict(row) for row in monthly_upload],
        }
