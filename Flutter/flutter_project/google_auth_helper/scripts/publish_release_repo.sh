#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.1.2}"
RELEASE_SUMMARY="${2:-Windows auto test is disabled, platform icons stay visible on focus, and release artifacts are simplified.}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_REPO_URL="${RELEASE_REPO_URL:-https://github.com/greenhelix/GAH-Release-Repo.git}"

if [[ -z "${CLONE_DIR:-}" ]]; then
  if [[ -d "/mnt/e/github/GAH-Release-Repo/.git" ]]; then
    CLONE_DIR="/mnt/e/github/GAH-Release-Repo"
  else
    CLONE_DIR="$HOME/.cache/gah-release-repo"
  fi
fi

copy_release_files() {
  local source_dir="$1"
  local target_dir="$2"
  shift 2
  local copied=0

  if [[ ! -d "$source_dir" ]]; then
    return 0
  fi

  rm -rf "$target_dir"
  mkdir -p "$target_dir"

  for pattern in "$@"; do
    shopt -s nullglob
    for file in "$source_dir"/$pattern; do
      cp "$file" "$target_dir"/
      copied=1
    done
    shopt -u nullglob
  done

  if [[ "$copied" -eq 0 ]]; then
    rmdir "$target_dir"
  fi
}

if [[ -d "$CLONE_DIR/.git" ]]; then
  git -C "$CLONE_DIR" pull --ff-only
else
  rm -rf "$CLONE_DIR"
  git clone "$RELEASE_REPO_URL" "$CLONE_DIR"
fi

copy_release_files \
  "$PROJECT_ROOT/test_release/windows/$VERSION" \
  "$CLONE_DIR/windows/$VERSION" \
  "*.exe" "INSTALL.txt"

copy_release_files \
  "$PROJECT_ROOT/test_release/linux/$VERSION" \
  "$CLONE_DIR/ubuntu/$VERSION" \
  "*.deb" "INSTALL.txt"

cat > "$CLONE_DIR/README.md" <<EOF
# GAH Release Repo

Release-only repository for Google Auth Helper installers.

## Layout

windows/
  vX.Y.Z/
    gah-windows-vX.Y.Z-setup.exe
    INSTALL.txt
ubuntu/
  vX.Y.Z/
    gah-ubuntu-vX.Y.Z.deb
    INSTALL.txt

## Latest

- $VERSION: $RELEASE_SUMMARY
EOF

git -C "$CLONE_DIR" add .
if git -C "$CLONE_DIR" diff --cached --quiet; then
  if ! git -C "$CLONE_DIR" status --short --branch | grep -q "ahead"; then
    echo "No release repo changes to commit or push."
    exit 0
  fi
else
  git -C "$CLONE_DIR" commit -m "Release $VERSION"
fi
if ! git -C "$CLONE_DIR" push origin main; then
  echo "Push failed. Local release repo clone is ready at $CLONE_DIR"
  exit 0
fi

if command -v gh >/dev/null 2>&1; then
  assets=()
  if [[ -d "$CLONE_DIR/windows/$VERSION" ]]; then
    while IFS= read -r -d '' file; do
      assets+=("$file")
    done < <(find "$CLONE_DIR/windows/$VERSION" -maxdepth 1 -type f -print0)
  fi
  if [[ -d "$CLONE_DIR/ubuntu/$VERSION" ]]; then
    while IFS= read -r -d '' file; do
      assets+=("$file")
    done < <(find "$CLONE_DIR/ubuntu/$VERSION" -maxdepth 1 -type f -print0)
  fi
  if [[ "${#assets[@]}" -gt 0 ]]; then
    gh release create "$VERSION" --repo greenhelix/GAH-Release-Repo --notes "$RELEASE_SUMMARY" "${assets[@]}"
  fi
fi
