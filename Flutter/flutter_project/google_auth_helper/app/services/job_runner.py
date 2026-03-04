from __future__ import annotations

import asyncio
import uuid
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Set

from app.models import JobSnapshot
from app.services.tool_registry import ToolRegistry


@dataclass
class JobRuntime:
    id: str
    tool_id: str
    serial: str
    command: List[str]
    status: str = "queued"
    created_at: datetime = field(default_factory=datetime.utcnow)
    started_at: datetime | None = None
    finished_at: datetime | None = None
    exit_code: int | None = None
    message: str | None = None
    process: asyncio.subprocess.Process | None = None

    def to_snapshot(self) -> JobSnapshot:
        return JobSnapshot(
            id=self.id,
            tool_id=self.tool_id,
            serial=self.serial,
            command=self.command,
            status=self.status,  # type: ignore[arg-type]
            created_at=self.created_at,
            started_at=self.started_at,
            finished_at=self.finished_at,
            exit_code=self.exit_code,
            message=self.message,
        )


class JobRunner:
    def __init__(self, registry: ToolRegistry) -> None:
        self.registry = registry
        self.jobs: Dict[str, JobRuntime] = {}
        self._subscribers: Set[asyncio.Queue[str]] = set()
        self._lock = asyncio.Lock()

    async def start_job(self, tool_id: str, serial: str, extra_args: List[str]) -> JobSnapshot:
        command = self.registry.build_command(tool_id, serial, extra_args)
        job_id = str(uuid.uuid4())
        runtime = JobRuntime(id=job_id, tool_id=tool_id, serial=serial, command=command)
        self.jobs[job_id] = runtime
        runtime.status = "running"
        runtime.started_at = datetime.utcnow()
        runtime.process = await asyncio.create_subprocess_exec(
            *command,
            stdin=asyncio.subprocess.PIPE,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
        )
        asyncio.create_task(self._collect_output(runtime))
        return runtime.to_snapshot()

    async def _collect_output(self, runtime: JobRuntime) -> None:
        process = runtime.process
        if not process or not process.stdout:
            runtime.status = "failed"
            runtime.message = "프로세스를 시작하지 못했습니다."
            runtime.finished_at = datetime.utcnow()
            return

        await self._publish(f"[{runtime.id}] 작업 시작: {' '.join(runtime.command)}")
        while True:
            line = await process.stdout.readline()
            if not line:
                break
            text = line.decode("utf-8", errors="replace").rstrip()
            await self._publish(f"[{runtime.id}] {text}")

        return_code = await process.wait()
        runtime.exit_code = return_code
        runtime.finished_at = datetime.utcnow()
        if runtime.status == "canceled":
            runtime.message = "사용자가 작업을 취소했습니다."
        elif return_code == 0:
            runtime.status = "success"
            runtime.message = "작업이 정상 완료되었습니다."
        else:
            runtime.status = "failed"
            runtime.message = f"작업이 실패했습니다. exit_code={return_code}"

        await self._publish(f"[{runtime.id}] 종료: {runtime.message}")

    async def cancel_job(self, job_id: str) -> JobSnapshot:
        runtime = self.jobs.get(job_id)
        if not runtime:
            raise KeyError(f"알 수 없는 Job ID: {job_id}")

        if runtime.process and runtime.status == "running":
            runtime.status = "canceled"
            runtime.process.terminate()
            try:
                await asyncio.wait_for(runtime.process.wait(), timeout=5)
            except asyncio.TimeoutError:
                runtime.process.kill()
                await runtime.process.wait()
            runtime.finished_at = datetime.utcnow()
        return runtime.to_snapshot()

    async def send_input(self, job_id: str, value: str) -> None:
        runtime = self.jobs.get(job_id)
        if not runtime:
            raise KeyError(f"알 수 없는 Job ID: {job_id}")
        if not runtime.process or not runtime.process.stdin:
            raise RuntimeError("입력을 받을 수 없는 작업입니다.")
        runtime.process.stdin.write((value + "\n").encode("utf-8"))
        await runtime.process.stdin.drain()

    def list_jobs(self) -> List[JobSnapshot]:
        jobs = sorted(self.jobs.values(), key=lambda item: item.created_at, reverse=True)
        return [job.to_snapshot() for job in jobs]

    async def subscribe(self) -> asyncio.Queue[str]:
        queue: asyncio.Queue[str] = asyncio.Queue()
        async with self._lock:
            self._subscribers.add(queue)
        return queue

    async def unsubscribe(self, queue: asyncio.Queue[str]) -> None:
        async with self._lock:
            self._subscribers.discard(queue)

    async def _publish(self, message: str) -> None:
        async with self._lock:
            queues = list(self._subscribers)
        for queue in queues:
            await queue.put(message)
