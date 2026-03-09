#!/usr/bin/env bash
set -euo pipefail

if ! command -v flutter >/dev/null 2>&1; then
  echo "flutter command was not found. Install Flutter SDK and add it to PATH first." >&2
  exit 1
fi

flutter create . \
  --platforms=windows,linux,web \
  --project-name google_auth_helper \
  --org com.greenhelix \
  --overwrite

flutter pub get
