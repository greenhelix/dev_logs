#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.1.1}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STAGE_ROOT="$PROJECT_ROOT/test_release/linux/$VERSION"
APP_ROOT="$STAGE_ROOT/pkg-root"
APP_INSTALL_DIR="$APP_ROOT/opt/google-auth-helper"
DEBIAN_DIR="$APP_ROOT/DEBIAN"
DESKTOP_DIR="$APP_ROOT/usr/share/applications"
BIN_DIR="$APP_ROOT/usr/bin"
RELEASE_DIR="$PROJECT_ROOT/build/linux/x64/release/bundle"
CONTROL_TEMPLATE="$PROJECT_ROOT/packaging/linux/control"

mkdir -p "$APP_INSTALL_DIR" "$DEBIAN_DIR" "$DESKTOP_DIR" "$BIN_DIR"

pushd "$PROJECT_ROOT" >/dev/null
flutter pub get
flutter analyze
flutter test
flutter build linux --release
flutter build web
firebase deploy --only hosting

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

echo "$VERSION" > "$STAGE_ROOT/VERSION.txt"
cat > "$STAGE_ROOT/INSTALL.txt" <<EOF
1. sudo dpkg -i gah-ubuntu-${VERSION}.deb
2. Launch Google Auth Helper from the app menu or run google-auth-helper
EOF

dpkg-deb --build "$APP_ROOT" "$STAGE_ROOT/gah-ubuntu-${VERSION}.deb"
(cd "$STAGE_ROOT" && sha256sum "$(basename "$STAGE_ROOT/gah-ubuntu-${VERSION}.deb")" VERSION.txt INSTALL.txt > checksums.txt)
popd >/dev/null
