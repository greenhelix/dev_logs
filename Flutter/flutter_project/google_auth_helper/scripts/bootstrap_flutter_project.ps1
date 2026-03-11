$ErrorActionPreference = 'Stop'

if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) {
  throw 'flutter command was not found. Install Flutter SDK and add it to PATH first.'
}

$projectDrive = Split-Path -Path (Get-Location) -Qualifier
$projectVolume = Get-Volume ($projectDrive.TrimEnd('\', ':'))
if ($projectVolume.FileSystem -ne 'NTFS') {
  throw "Windows desktop build requires an NTFS project drive. Current drive $projectDrive uses $($projectVolume.FileSystem). Move the repository to an NTFS drive."
}

$pubCache = 'E:\Pub\Cache'
if (-not (Test-Path $pubCache)) {
  New-Item -ItemType Directory -Path $pubCache | Out-Null
}

$env:PUB_CACHE = $pubCache
Write-Host "Using PUB_CACHE=$env:PUB_CACHE"

flutter create . `
  --platforms=windows,linux,web `
  --project-name google_auth_helper `
  --org com.greenhelix `
  --overwrite

flutter pub get
