from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass
from html import unescape
from typing import Any, Dict, List


@dataclass
class ParsedCase:
    module: str
    testcase: str
    result: str
    details: str = ""


@dataclass
class ParsedReport:
    source_type: str
    firmware_version: str
    tool_version: str
    elapsed_time: str
    total_count: int
    pass_count: int
    fail_count: int
    cases: List[ParsedCase]
    failed_items: List[Dict[str, str]]
    metadata: Dict[str, Any]

    def to_dict(self) -> Dict[str, Any]:
        return {
            "source_type": self.source_type,
            "firmware_version": self.firmware_version,
            "tool_version": self.tool_version,
            "elapsed_time": self.elapsed_time,
            "total_count": self.total_count,
            "pass_count": self.pass_count,
            "fail_count": self.fail_count,
            "cases": [asdict(case) for case in self.cases],
            "failed_items": self.failed_items,
            "metadata": self.metadata,
        }


class ReportParser:
    def parse(
        self,
        file_name: str,
        content: bytes,
        source_type: str = "auto",
    ) -> ParsedReport:
        detected = self._detect_type(file_name=file_name, content=content, source_type=source_type)
        if detected == "xml":
            return self._parse_xml(content)
        if detected == "html":
            return self._parse_html(content)
        raise ValueError("지원하지 않는 결과 파일 형식입니다. (xml/html)")

    def _detect_type(self, file_name: str, content: bytes, source_type: str) -> str:
        if source_type in {"xml", "html"}:
            return source_type
        lower = file_name.lower()
        if lower.endswith(".xml"):
            return "xml"
        if lower.endswith(".html") or lower.endswith(".htm"):
            return "html"
        head = content[:200].decode("utf-8", errors="ignore").lower()
        if "<html" in head:
            return "html"
        if "<" in head:
            return "xml"
        raise ValueError("파일 형식을 자동 판별하지 못했습니다.")

    def _parse_xml(self, content: bytes) -> ParsedReport:
        root = ET.fromstring(content)
        cases: List[ParsedCase] = []

        firmware_version = ""
        tool_version = ""
        elapsed_time = ""
        for key, value in root.attrib.items():
            key_lower = key.lower()
            if "firmware" in key_lower or "build" in key_lower:
                firmware_version = firmware_version or value
            if "tool" in key_lower or "tradefed" in key_lower or "version" in key_lower:
                tool_version = tool_version or value
            if "elapsed" in key_lower or "duration" in key_lower or "time" in key_lower:
                elapsed_time = elapsed_time or value

        for module_elem in root.findall(".//Module"):
            module_name = (
                module_elem.attrib.get("name")
                or module_elem.attrib.get("id")
                or "unknown_module"
            )
            for case_elem in module_elem.findall(".//TestCase"):
                case_name = case_elem.attrib.get("name") or "unknown_case"
                test_elems = case_elem.findall(".//Test")
                if not test_elems:
                    status = self._normalize_result(case_elem.attrib.get("result") or "")
                    cases.append(
                        ParsedCase(
                            module=module_name,
                            testcase=case_name,
                            result=status,
                            details="",
                        )
                    )
                    continue
                for test_elem in test_elems:
                    test_name = test_elem.attrib.get("name") or case_name
                    status = self._normalize_result(
                        test_elem.attrib.get("result")
                        or test_elem.attrib.get("status")
                        or ""
                    )
                    details = test_elem.attrib.get("message") or ""
                    cases.append(
                        ParsedCase(
                            module=module_name,
                            testcase=f"{case_name}.{test_name}" if test_name != case_name else case_name,
                            result=status,
                            details=details,
                        )
                    )

        if not cases:
            for test_elem in root.findall(".//*[@result]"):
                result = self._normalize_result(test_elem.attrib.get("result", ""))
                name = test_elem.attrib.get("name", "unknown_case")
                cases.append(
                    ParsedCase(
                        module="unknown_module",
                        testcase=name,
                        result=result,
                        details="",
                    )
                )

        return self._finalize_report(
            source_type="xml",
            firmware_version=firmware_version,
            tool_version=tool_version,
            elapsed_time=elapsed_time,
            cases=cases,
            metadata={"root_tag": self._strip_namespace(root.tag)},
        )

    def _parse_html(self, content: bytes) -> ParsedReport:
        raw = content.decode("utf-8", errors="replace")
        text = self._strip_html(raw)
        metadata = self._extract_text_metadata(text)

        lines = [line.strip() for line in text.splitlines() if line.strip()]
        cases: List[ParsedCase] = []
        for line in lines:
            upper = line.upper()
            if "FAIL" not in upper and "FAILED" not in upper:
                continue
            parsed = self._parse_failure_line(line)
            if not parsed:
                continue
            cases.append(parsed)

        return self._finalize_report(
            source_type="html",
            firmware_version=metadata.get("firmware_version", ""),
            tool_version=metadata.get("tool_version", ""),
            elapsed_time=metadata.get("elapsed_time", ""),
            cases=cases,
            metadata=metadata,
        )

    def _finalize_report(
        self,
        source_type: str,
        firmware_version: str,
        tool_version: str,
        elapsed_time: str,
        cases: List[ParsedCase],
        metadata: Dict[str, Any],
    ) -> ParsedReport:
        normalized_cases: List[ParsedCase] = []
        pass_count = 0
        fail_count = 0
        for case in cases:
            result = self._normalize_result(case.result)
            normalized = ParsedCase(
                module=case.module or "unknown_module",
                testcase=case.testcase or "unknown_case",
                result=result,
                details=case.details,
            )
            normalized_cases.append(normalized)
            if result == "FAIL":
                fail_count += 1
            elif result == "PASS":
                pass_count += 1

        total_count = len(normalized_cases)
        failed_items = [
            {
                "module": case.module,
                "testcase": case.testcase,
                "reason": case.details or "실패 세부사유 없음",
            }
            for case in normalized_cases
            if case.result == "FAIL"
        ]

        return ParsedReport(
            source_type=source_type,
            firmware_version=firmware_version or "UNKNOWN",
            tool_version=tool_version or "UNKNOWN",
            elapsed_time=elapsed_time or "UNKNOWN",
            total_count=total_count,
            pass_count=pass_count,
            fail_count=fail_count,
            cases=normalized_cases,
            failed_items=failed_items,
            metadata=metadata,
        )

    def _parse_failure_line(self, line: str) -> ParsedCase | None:
        pattern = re.compile(
            r"(?P<module>[A-Za-z0-9_.-]+)\s*[:#/]\s*(?P<test>[A-Za-z0-9_.$-]+).*?(FAIL|FAILED)",
            re.IGNORECASE,
        )
        match = pattern.search(line)
        if match:
            return ParsedCase(
                module=match.group("module"),
                testcase=match.group("test"),
                result="FAIL",
                details=line[:500],
            )
        if "FAIL" in line.upper() and any(separator in line for separator in [":", "#", "/"]):
            tokens = re.findall(r"[A-Za-z0-9_.-]+", line)
            if len(tokens) >= 2:
                return ParsedCase(
                    module=tokens[0],
                    testcase=tokens[1],
                    result="FAIL",
                    details=line[:500],
                )
        return None

    def _extract_text_metadata(self, text: str) -> Dict[str, str]:
        metadata: Dict[str, str] = {}
        patterns = {
            "firmware_version": [
                r"(?i)firmware(?:\s+version)?\s*[:=]\s*([^\n]+)",
                r"(?i)build(?:\s+fingerprint)?\s*[:=]\s*([^\n]+)",
            ],
            "tool_version": [
                r"(?i)(?:tool|tradefed|cts)\s+version\s*[:=]\s*([^\n]+)",
                r"(?i)version\s*[:=]\s*([^\n]+)",
            ],
            "elapsed_time": [
                r"(?i)(?:elapsed|duration|time)\s*[:=]\s*([^\n]+)",
            ],
        }
        for key, key_patterns in patterns.items():
            for pattern in key_patterns:
                match = re.search(pattern, text)
                if match:
                    metadata[key] = match.group(1).strip()[:200]
                    break
        return metadata

    def _strip_html(self, html_text: str) -> str:
        no_script = re.sub(r"(?is)<script.*?>.*?</script>", " ", html_text)
        no_style = re.sub(r"(?is)<style.*?>.*?</style>", " ", no_script)
        no_tags = re.sub(r"(?is)<[^>]+>", "\n", no_style)
        plain = unescape(no_tags)
        plain = re.sub(r"\r", "", plain)
        plain = re.sub(r"\n{3,}", "\n\n", plain)
        return plain

    def _normalize_result(self, value: str) -> str:
        upper = (value or "").upper()
        if upper in {"PASS", "PASSED", "OK"}:
            return "PASS"
        if upper in {"FAIL", "FAILED", "ERROR"}:
            return "FAIL"
        if upper in {"NOT_EXECUTED", "NOTRUN", "NOT_RUN", "SKIP", "SKIPPED"}:
            return "NOT_RUN"
        if upper in {"BLOCKED"}:
            return "BLOCKED"
        return "NOT_RUN"

    def _strip_namespace(self, tag: str) -> str:
        if "}" in tag:
            return tag.split("}", maxsplit=1)[1]
        return tag
