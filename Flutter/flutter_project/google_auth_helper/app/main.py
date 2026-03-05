from __future__ import annotations

import asyncio
import shutil
from datetime import datetime
from pathlib import Path

from fastapi import FastAPI, File, Form, Header, HTTPException, UploadFile, WebSocket
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from app.config import load_settings
from app.models import JobInputRequest, ReportUploadRequest, StartJobRequest
from app.services.adb_service import AdbService
from app.services.analytics import AnalyticsService
from app.services.env_check import EnvironmentChecker
from app.services.firestore_service import FirestoreService
from app.services.integrations import IntegrationDispatcher
from app.services.job_runner import JobRunner
from app.services.monitoring import MonitoringService
from app.services.report_parser import ReportParser
from app.services.result_store import ResultStore
from app.services.result_watcher import ResultWatcher
from app.services.terminal_manager import TerminalManager
from app.services.tool_registry import ToolRegistry

settings = load_settings()
tool_registry = ToolRegistry(settings)
env_checker = EnvironmentChecker(settings, tool_registry)
adb_service = AdbService()
job_runner = JobRunner(tool_registry)
terminal_manager = TerminalManager()
integration_dispatcher = IntegrationDispatcher(settings)
monitoring_service = MonitoringService(tool_registry, env_checker, job_runner)
report_parser = ReportParser()
result_store = ResultStore(settings.workspace_dir / "test_results.db")
analytics_service = AnalyticsService(result_store)
result_watcher = ResultWatcher(settings, tool_registry, report_parser, result_store)
firestore_service = FirestoreService(settings)

app = FastAPI(title="Google Auth Helper", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins if settings.allowed_origins else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

static_dir = Path(__file__).parent / "static"
app.mount("/static", StaticFiles(directory=static_dir), name="static")


@app.on_event("startup")
async def startup_event() -> None:
    settings.workspace_dir.mkdir(parents=True, exist_ok=True)
    settings.logs_dir.mkdir(parents=True, exist_ok=True)
    settings.firmware_dir.mkdir(parents=True, exist_ok=True)
    result_store.initialize()
    await result_watcher.start()


@app.on_event("shutdown")
async def shutdown_event() -> None:
    await result_watcher.stop()


@app.get("/")
async def index() -> FileResponse:
    return FileResponse(static_dir / "index.html")


@app.get("/api/health")
async def health() -> dict:
    return {
        "status": "ok",
        "time_utc": datetime.utcnow().isoformat(),
        "platform_focus": "ubuntu-linux",
    }


@app.get("/api/tools")
async def list_tools() -> dict:
    tool_registry.refresh()
    return {"tools": tool_registry.list_status()}


@app.get("/api/adb/devices")
async def list_adb_devices() -> dict:
    try:
        devices = await adb_service.list_devices()
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return {"devices": [item.model_dump() for item in devices]}


@app.get("/api/environment/check")
async def environment_check() -> dict:
    report = env_checker.run()
    return report.model_dump()


@app.get("/api/analytics/dashboard")
async def analytics_dashboard() -> dict:
    return analytics_service.dashboard_payload()


@app.get("/api/watcher/status")
async def watcher_status() -> dict:
    return result_watcher.status()


@app.get("/api/watcher/events")
async def watcher_events(limit: int = 50) -> dict:
    return {"events": result_watcher.list_events(limit=limit)}


@app.post("/api/watcher/enable")
async def watcher_enable() -> dict:
    await result_watcher.set_enabled(True)
    return result_watcher.status()


@app.post("/api/watcher/disable")
async def watcher_disable() -> dict:
    await result_watcher.set_enabled(False)
    return result_watcher.status()


@app.post("/api/watcher/scan-now")
async def watcher_scan_now() -> dict:
    try:
        result_watcher.scan_now()
        return result_watcher.status()
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.get("/api/monitor/summary")
async def monitor_summary(
    x_monitor_token: str | None = Header(default=None),
) -> dict:
    expected = settings.monitor_api_token.strip()
    if expected and x_monitor_token != expected:
        raise HTTPException(status_code=401, detail="유효하지 않은 모니터링 토큰입니다.")
    return monitoring_service.build_summary()


@app.post("/api/firmware/upload")
async def upload_firmware(
    firmware_file: UploadFile = File(...),
    serial: str = Form(""),
    remote_path: str = Form(""),
) -> dict:
    file_name = Path(firmware_file.filename or "firmware.bin").name
    target = settings.firmware_dir / file_name
    try:
        with target.open("wb") as out:
            shutil.copyfileobj(firmware_file.file, out)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"파일 저장 실패: {exc}") from exc
    finally:
        firmware_file.file.close()

    try:
        result = await adb_service.push_firmware(
            local_path=target,
            remote_path=remote_path or settings.default_remote_firmware_path,
            serial=serial or None,
        )
        return {
            "saved_path": str(target),
            "adb_result": result,
        }
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/api/jobs/start")
async def start_job(request: StartJobRequest) -> dict:
    try:
        serial = request.serial.strip()
        if not serial:
            serial = (await adb_service.pick_default_serial()) or ""
        if not serial:
            raise RuntimeError("연결된 adb device가 없습니다. 시리얼을 확인하세요.")
        snapshot = await job_runner.start_job(
            tool_id=request.tool_id,
            serial=serial,
            extra_args=request.extra_args,
        )
        return snapshot.model_dump()
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.get("/api/jobs")
async def get_jobs() -> dict:
    return {"jobs": [item.model_dump() for item in job_runner.list_jobs()]}


@app.post("/api/jobs/{job_id}/cancel")
async def cancel_job(job_id: str) -> dict:
    try:
        snapshot = await job_runner.cancel_job(job_id)
        return snapshot.model_dump()
    except Exception as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@app.post("/api/jobs/{job_id}/input")
async def send_job_input(job_id: str, request: JobInputRequest) -> dict:
    try:
        await job_runner.send_input(job_id, request.value)
        return {"ok": True}
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/api/reports/upload")
async def upload_report(request: ReportUploadRequest) -> dict:
    result = await integration_dispatcher.upload_all(request)
    return {
        "uploaded_at": datetime.utcnow().isoformat(),
        "report": request.model_dump(),
        "result": {
            "results": [item.model_dump() for item in result["results"]],
        },
    }


@app.post("/api/reports/import-file")
async def import_report_file(
    result_file: UploadFile = File(...),
    source_type: str = Form("auto"),
    save_full: bool = Form(True),
    firmware_version: str = Form(""),
    tool_version: str = Form(""),
    elapsed_time: str = Form(""),
) -> dict:
    file_name = Path(result_file.filename or "result_file").name
    try:
        content = await result_file.read()
        parsed_report = report_parser.parse(
            file_name=file_name,
            content=content,
            source_type=source_type,
        )
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"결과서 파싱 실패: {exc}") from exc
    finally:
        result_file.file.close()

    parsed_payload = parsed_report.to_dict()
    if firmware_version.strip():
        parsed_payload["firmware_version"] = firmware_version.strip()
    if tool_version.strip():
        parsed_payload["tool_version"] = tool_version.strip()
    if elapsed_time.strip():
        parsed_payload["elapsed_time"] = elapsed_time.strip()

    run_id = None
    if save_full:
        run_id = result_store.save_report(
            source_file=file_name,
            parsed=parsed_payload,
            imported_at=datetime.utcnow().isoformat(),
        )

    return {
        "run_id": run_id,
        "parsed": parsed_payload,
    }


@app.get("/api/reports/runs")
async def list_report_runs(limit: int = 50) -> dict:
    safe_limit = max(1, min(limit, 500))
    return {"runs": result_store.list_runs(limit=safe_limit)}


@app.get("/api/firebase/firestore/{collection}")
async def firestore_list(collection: str, limit: int = 20) -> dict:
    try:
        return firestore_service.list_documents(collection=collection, page_size=limit)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.get("/api/firebase/status")
async def firebase_status() -> dict:
    payload = {
        "backend": settings.firebase_backend,
        "project_id": settings.firebase_project_id,
        "hosting_url": settings.firebase_hosting_url,
        "database_id": settings.firestore_database_id,
        "default_collection": settings.firestore_default_collection,
        "configured": firestore_service.enabled(),
        "ok": False,
        "detail": "",
    }
    if not firestore_service.enabled():
        payload["detail"] = "FIREBASE_PROJECT_ID/FIREBASE_BEARER_TOKEN 설정 필요"
        return payload
    try:
        firestore_service.list_documents(
            collection=settings.firestore_default_collection,
            page_size=1,
        )
        payload["ok"] = True
        payload["detail"] = "Firestore 연결 확인 완료"
        return payload
    except Exception as exc:
        payload["detail"] = str(exc)
        return payload


@app.post("/api/firebase/firestore/{collection}")
async def firestore_create(collection: str, payload: dict) -> dict:
    data = payload.get("data")
    if not isinstance(data, dict):
        raise HTTPException(status_code=400, detail="payload.data(dict)가 필요합니다.")
    document_id = str(payload.get("document_id", "")).strip()
    try:
        doc = firestore_service.create_document(collection=collection, data=data, document_id=document_id)
        return {"document": doc}
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.put("/api/firebase/firestore/{collection}/{document_id}")
async def firestore_update(collection: str, document_id: str, payload: dict) -> dict:
    data = payload.get("data")
    if not isinstance(data, dict):
        raise HTTPException(status_code=400, detail="payload.data(dict)가 필요합니다.")
    try:
        doc = firestore_service.update_document(collection=collection, document_id=document_id, data=data)
        return {"document": doc}
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/api/firebase/sync/runs")
async def firestore_sync_runs(limit: int = 20, collection: str | None = None) -> dict:
    safe_limit = max(1, min(limit, 200))
    target_collection = (collection or settings.firestore_default_collection).strip()
    if not target_collection:
        raise HTTPException(status_code=400, detail="Firestore collection이 비어 있습니다.")
    runs = result_store.list_runs(limit=safe_limit)
    synced = 0
    for run in runs:
        doc_id = f"run-{run.get('id')}"
        firestore_service.upsert_document(
            collection=target_collection,
            document_id=doc_id,
            data=run,
        )
        synced += 1
    return {
        "collection": target_collection,
        "requested": safe_limit,
        "synced": synced,
    }


@app.post("/api/firebase/sync/monitor")
async def firestore_sync_monitor(collection: str | None = None) -> dict:
    target_collection = (collection or settings.firestore_default_collection).strip()
    if not target_collection:
        raise HTTPException(status_code=400, detail="Firestore collection이 비어 있습니다.")
    summary = monitoring_service.build_summary()
    doc = firestore_service.upsert_document(
        collection=target_collection,
        document_id="monitor-latest",
        data=summary,
    )
    return {"collection": target_collection, "document": doc}


@app.get("/api/reports/runs/{run_id}")
async def get_report_run(run_id: int) -> dict:
    run = result_store.get_run(run_id=run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="결과 데이터가 없습니다.")
    return run


@app.websocket("/ws/logs")
async def ws_logs(websocket: WebSocket) -> None:
    await websocket.accept()
    queue = await job_runner.subscribe()
    try:
        while True:
            message = await queue.get()
            await websocket.send_text(message)
    except Exception:
        pass
    finally:
        await job_runner.unsubscribe(queue)


@app.websocket("/ws/terminal")
async def ws_terminal(websocket: WebSocket) -> None:
    await terminal_manager.interactive_shell(websocket)


def create_app() -> FastAPI:
    return app
