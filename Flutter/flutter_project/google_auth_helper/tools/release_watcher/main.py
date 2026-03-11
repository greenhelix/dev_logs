import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def load_config(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def ensure_output_dir(base: Path) -> Path:
    output_dir = base / "output"
    output_dir.mkdir(parents=True, exist_ok=True)
    return output_dir


def build_snapshot(config: dict) -> dict:
    version = "unknown"
    release_notes_text = (
        "Chrome remote debugging integration is intentionally left as the next step. "
        "This scaffold writes stable artifact files for Flutter integration."
    )
    return {
        "sourceLabel": "Google Portal Watcher",
        "version": version,
        "releaseNotesHash": hashlib.sha256(release_notes_text.encode("utf-8")).hexdigest(),
        "lastCheckedAt": utc_now(),
        "lastUploadedAt": None,
        "uploadStatus": "pending_flutter_upload",
        "changes": [
            {
                "kind": "scaffold",
                "summary": "Watcher scaffold ran successfully. Replace placeholder scraping with CDP-driven extraction."
            }
        ],
    }


def write_json(path: Path, payload: dict) -> None:
    path.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    args = parser.parse_args()

    config_path = Path(args.config)
    project_root = config_path.parents[1]
    config = load_config(config_path)
    output_dir = ensure_output_dir(project_root)

    snapshot = build_snapshot(config)
    diff = {
        "generatedAt": utc_now(),
        "summary": "Initial scaffold run. No historical comparison yet."
    }
    mail_log = {
        "generatedAt": utc_now(),
        "status": "not_sent",
        "reason": "SMTP delivery is not wired in this scaffold."
    }

    write_json(output_dir / "latest_snapshot.json", snapshot)
    write_json(output_dir / "diff.json", diff)
    write_json(output_dir / "mail_log.json", mail_log)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
