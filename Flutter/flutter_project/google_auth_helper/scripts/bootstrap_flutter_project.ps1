$ErrorActionPreference = 'Stop'

if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) {
  throw 'flutter command was not found. Install Flutter SDK and add it to PATH first.'
}

flutter create . `
  --platforms=windows,linux,web `
  --project-name google_auth_helper `
  --org com.greenhelix `
  --overwrite

flutter pub get

