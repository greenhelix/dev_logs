#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.1.2}"
RELEASE_SUMMARY="${2:-헤더 플랫폼 표시는 제거했고 셸과 주요 화면을 한국어 중심으로 정리했으며 결과 업로드, ADB 경로 설정, 업데이트 설치 흐름을 반영했습니다.}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STAGE_ROOT="$PROJECT_ROOT/test_release/linux/$VERSION"
TEMP_ROOT="$(mktemp -d)"
APP_ROOT="$TEMP_ROOT/pkg-root"
APP_INSTALL_DIR="$APP_ROOT/opt/google-auth-helper"
DEBIAN_DIR="$APP_ROOT/DEBIAN"
DESKTOP_DIR="$APP_ROOT/usr/share/applications"
BIN_DIR="$APP_ROOT/usr/bin"
RELEASE_DIR="$PROJECT_ROOT/build/linux/x64/release/bundle"
CONTROL_TEMPLATE="$PROJECT_ROOT/packaging/linux/control"
BUILD_DATE="$(date '+%Y-%m-%d %H:%M:%S')"

cleanup() {
  rm -rf "$TEMP_ROOT"
}
trap cleanup EXIT

rm -rf "$STAGE_ROOT"
mkdir -p "$STAGE_ROOT" "$APP_INSTALL_DIR" "$DEBIAN_DIR" "$DESKTOP_DIR" "$BIN_DIR"

pushd "$PROJECT_ROOT" >/dev/null
flutter pub get
flutter analyze
flutter test
flutter build linux --release

if [[ ! -d "$RELEASE_DIR" ]]; then
  echo "Linux release directory not found: $RELEASE_DIR" >&2
  exit 1
fi

cp -R "$RELEASE_DIR"/. "$APP_INSTALL_DIR"/
sed "s/__VERSION__/${VERSION#v}/g" "$CONTROL_TEMPLATE" > "$DEBIAN_DIR/control"
install -m 0755 "$PROJECT_ROOT/packaging/linux/postinst" "$DEBIAN_DIR/postinst"
install -m 0644 "$PROJECT_ROOT/packaging/linux/google-auth-helper.desktop" \
  "$DESKTOP_DIR/google-auth-helper.desktop"

cat > "$BIN_DIR/google-auth-helper" <<'EOF'
#!/usr/bin/env bash
exec /opt/google-auth-helper/google_auth_helper "$@"
EOF
chmod 0755 "$BIN_DIR/google-auth-helper"

cat > "$STAGE_ROOT/INSTALL.txt" <<EOF
Version: $VERSION
Built: $BUILD_DATE
Changes: $RELEASE_SUMMARY

1. sudo dpkg -i gah-ubuntu-${VERSION}.deb
2. Launch Google Auth Helper from the app menu or run google-auth-helper
EOF

dpkg-deb --build "$APP_ROOT" "$STAGE_ROOT/gah-ubuntu-${VERSION}.deb"
popd >/dev/null
