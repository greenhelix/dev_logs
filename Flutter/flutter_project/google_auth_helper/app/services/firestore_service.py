from __future__ import annotations

import json
import time
from pathlib import Path
from typing import Any, Dict, List
from urllib import error, request

from app.config import Settings


class FirestoreService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def enabled(self) -> bool:
        return bool(
            self.settings.firebase_project_id
            and (
                self.settings.firebase_bearer_token
                or self.settings.firebase_service_account_file
            )
        )

    def list_documents(self, collection: str, page_size: int = 20) -> Dict[str, Any]:
        self._ensure_enabled()
        safe_size = max(1, min(page_size, 200))
        url = (
            f"{self._base_url()}/{collection}"
            f"?pageSize={safe_size}&key={self.settings.firebase_api_key}"
        )
        payload = self._request("GET", url)
        docs = payload.get("documents", [])
        return {"collection": collection, "documents": [self._decode_document(doc) for doc in docs]}

    def create_document(self, collection: str, data: Dict[str, Any], document_id: str = "") -> Dict[str, Any]:
        self._ensure_enabled()
        params = f"?key={self.settings.firebase_api_key}"
        if document_id:
            params += f"&documentId={document_id}"
        url = f"{self._base_url()}/{collection}{params}"
        body = {"fields": self._encode_map(data)}
        payload = self._request("POST", url, body)
        return self._decode_document(payload)

    def update_document(self, collection: str, document_id: str, data: Dict[str, Any]) -> Dict[str, Any]:
        self._ensure_enabled()
        if not document_id:
            raise ValueError("document_id는 필수입니다.")
        url = f"{self._base_url()}/{collection}/{document_id}?key={self.settings.firebase_api_key}"
        body = {"fields": self._encode_map(data)}
        payload = self._request("PATCH", url, body)
        return self._decode_document(payload)

    def upsert_document(self, collection: str, document_id: str, data: Dict[str, Any]) -> Dict[str, Any]:
        try:
            return self.create_document(collection=collection, data=data, document_id=document_id)
        except RuntimeError as exc:
            if "409" not in str(exc):
                raise
            return self.update_document(collection=collection, document_id=document_id, data=data)

    def _base_url(self) -> str:
        return (
            "https://firestore.googleapis.com/v1/projects/"
            f"{self.settings.firebase_project_id}/databases/{self.settings.firestore_database_id}/documents"
        )

    def _request(self, method: str, url: str, body: Dict[str, Any] | None = None) -> Dict[str, Any]:
        raw = None
        if body is not None:
            raw = json.dumps(body, ensure_ascii=False).encode("utf-8")
        req = request.Request(
            url=url,
            data=raw,
            method=method,
            headers={
                "Authorization": f"Bearer {self._resolve_access_token()}",
                "Content-Type": "application/json",
            },
        )
        try:
            with request.urlopen(req, timeout=20) as response:
                return json.loads(response.read().decode("utf-8"))
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Firestore 요청 실패 ({exc.code}): {detail}") from exc
        except error.URLError as exc:
            raise RuntimeError(f"Firestore 연결 실패: {exc.reason}") from exc

    def _resolve_access_token(self) -> str:
        explicit = self.settings.firebase_bearer_token.strip()
        if explicit:
            return explicit

        sa_file = self.settings.firebase_service_account_file.strip()
        if sa_file:
            service_account_path = Path(sa_file).expanduser()
            if service_account_path.exists():
                try:
                    from google.auth.transport.requests import Request as GoogleAuthRequest
                    from google.oauth2 import service_account
                except Exception as exc:
                    raise RuntimeError(
                        "google-auth 라이브러리가 필요합니다. requirements.txt에 google-auth를 추가하세요."
                    ) from exc

                credentials = service_account.Credentials.from_service_account_file(
                    str(service_account_path),
                    scopes=["https://www.googleapis.com/auth/datastore"],
                )
                credentials.refresh(GoogleAuthRequest())
                token = credentials.token
                if not token:
                    raise RuntimeError("서비스 계정 액세스 토큰 발급에 실패했습니다.")
                return str(token)

        cli_token = self._resolve_cli_access_token()
        if cli_token:
            return cli_token
        raise RuntimeError(
            "Firebase 인증 정보가 없습니다. FIREBASE_BEARER_TOKEN 또는 서비스계정 파일 설정, 또는 firebase login이 필요합니다."
        )

    def _resolve_cli_access_token(self) -> str | None:
        config_path = Path.home() / ".config" / "configstore" / "firebase-tools.json"
        if not config_path.exists():
            return None
        try:
            payload = json.loads(config_path.read_text(encoding="utf-8"))
        except Exception:
            return None
        tokens = payload.get("tokens") or {}
        access_token = str(tokens.get("access_token", "")).strip()
        expires_at = int(tokens.get("expires_at", 0) or 0)
        now_ms = int(time.time() * 1000)
        if access_token and expires_at > (now_ms + 60_000):
            return access_token
        return None

    def _ensure_enabled(self) -> None:
        if self.settings.firebase_backend.strip().lower() != "firestore":
            raise RuntimeError("firebase backend가 firestore로 설정되지 않았습니다.")
        if not self.enabled():
            raise RuntimeError(
                "Firestore 설정이 부족합니다. FIREBASE_PROJECT_ID와 토큰(또는 서비스계정 파일)을 확인하세요."
            )

    def _encode_map(self, data: Dict[str, Any]) -> Dict[str, Any]:
        return {key: self._encode_value(value) for key, value in data.items()}

    def _encode_value(self, value: Any) -> Dict[str, Any]:
        if value is None:
            return {"nullValue": None}
        if isinstance(value, bool):
            return {"booleanValue": value}
        if isinstance(value, int):
            return {"integerValue": str(value)}
        if isinstance(value, float):
            return {"doubleValue": value}
        if isinstance(value, str):
            return {"stringValue": value}
        if isinstance(value, dict):
            return {"mapValue": {"fields": self._encode_map(value)}}
        if isinstance(value, list):
            return {"arrayValue": {"values": [self._encode_value(item) for item in value]}}
        return {"stringValue": str(value)}

    def _decode_document(self, doc: Dict[str, Any]) -> Dict[str, Any]:
        name = doc.get("name", "")
        doc_id = name.rsplit("/", maxsplit=1)[-1] if name else ""
        return {
            "id": doc_id,
            "name": name,
            "create_time": doc.get("createTime"),
            "update_time": doc.get("updateTime"),
            "data": self._decode_map(doc.get("fields", {})),
        }

    def _decode_map(self, fields: Dict[str, Any]) -> Dict[str, Any]:
        result: Dict[str, Any] = {}
        for key, typed in fields.items():
            result[key] = self._decode_value(typed)
        return result

    def _decode_value(self, typed: Dict[str, Any]) -> Any:
        if "stringValue" in typed:
            return typed["stringValue"]
        if "integerValue" in typed:
            try:
                return int(typed["integerValue"])
            except Exception:
                return typed["integerValue"]
        if "doubleValue" in typed:
            return typed["doubleValue"]
        if "booleanValue" in typed:
            return typed["booleanValue"]
        if "nullValue" in typed:
            return None
        if "mapValue" in typed:
            return self._decode_map(typed["mapValue"].get("fields", {}))
        if "arrayValue" in typed:
            values = typed["arrayValue"].get("values", [])
            return [self._decode_value(item) for item in values]
        if "timestampValue" in typed:
            return typed["timestampValue"]
        return typed
