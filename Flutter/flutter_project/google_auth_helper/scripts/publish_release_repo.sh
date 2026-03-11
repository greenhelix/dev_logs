#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.1.1}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_REPO_URL="${RELEASE_REPO_URL:-https://github.com/greenhelix/GAH-Release-Repo.git}"
CLONE_DIR="${CLONE_DIR:-$HOME/.cache/gah-release-repo}"

if [[ -d "$CLONE_DIR/.git" ]]; then
  git -C "$CLONE_DIR" pull --ff-only
else
  rm -rf "$CLONE_DIR"
  git clone "$RELEASE_REPO_URL" "$CLONE_DIR"
fi

mkdir -p "$CLONE_DIR/releases/$VERSION"
cp -R "$PROJECT_ROOT/test_release/windows/$VERSION"/. "$CLONE_DIR/releases/$VERSION"/ 2>/dev/null || true
cp -R "$PROJECT_ROOT/test_release/linux/$VERSION"/. "$CLONE_DIR/releases/$VERSION"/ 2>/dev/null || true

git -C "$CLONE_DIR" add .
git -C "$CLONE_DIR" commit -m "Release $VERSION" || true
git -C "$CLONE_DIR" push origin main

if command -v gh >/dev/null 2>&1; then
  gh release create "$VERSION" --repo greenhelix/GAH-Release-Repo --generate-notes "$CLONE_DIR/releases/$VERSION"/*
fi
