from __future__ import annotations

from typing import Any, Dict

from app.services.result_store import ResultStore


class AnalyticsService:
    def __init__(self, result_store: ResultStore) -> None:
        self.result_store = result_store

    def dashboard_payload(self) -> Dict[str, Any]:
        stored = self.result_store.build_analytics_payload()
        # 가상 데이터 기반 대시보드 시리즈
        mock = {
            "firmware_timeline_line": {
                "labels": ["2025-12", "2026-01", "2026-02", "2026-03"],
                "series": [
                    {"name": "FW1", "values": [44, 0, 0, 0]},
                    {"name": "FW2", "values": [52, 10, 0, 0]},
                    {"name": "FW3", "values": [0, 38, 28, 8]},
                    {"name": "FW4", "values": [0, 16, 35, 22]},
                    {"name": "FW5", "values": [0, 0, 18, 41]},
                ],
            },
            "cts_fail_trend_area": {
                "labels": ["시작", "1주", "2주", "3주", "종료"],
                "values": [148, 101, 64, 31, 12],
            },
            "upload_count_bar": {
                "labels": ["W1", "W2", "W3", "W4", "W5", "W6"],
                "values": [4, 7, 6, 11, 9, 14],
            },
            "tool_share_pie": {
                "labels": ["CTS", "GTS", "TVTS", "VTS", "STS", "CTS-on-GSI"],
                "values": [46, 18, 14, 8, 7, 7],
            },
            "result_distribution_doughnut": {
                "labels": ["PASS", "FAIL", "BLOCKED", "NOT_RUN"],
                "values": [812, 94, 22, 37],
            },
            "suite_radar": {
                "labels": ["Stability", "Performance", "Media", "Security", "Connectivity", "Power"],
                "values": [82, 69, 74, 91, 77, 66],
            },
        }
        return {"mock": mock, "stored": stored}
