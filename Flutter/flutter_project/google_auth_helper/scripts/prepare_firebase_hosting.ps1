Param()

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$publicDir = Join-Path $projectRoot "deploy\\firebase\\public"
$staticSrc = Join-Path $projectRoot "app\\static"
$staticDst = Join-Path $publicDir "static"

if (Test-Path $publicDir) {
  Remove-Item -Recurse -Force $publicDir
}
New-Item -ItemType Directory -Path $staticDst -Force | Out-Null

Copy-Item -Path (Join-Path $staticSrc "index.html") -Destination (Join-Path $publicDir "index.html") -Force
Copy-Item -Path (Join-Path $staticSrc "app.js") -Destination (Join-Path $staticDst "app.js") -Force
Copy-Item -Path (Join-Path $staticSrc "styles.css") -Destination (Join-Path $staticDst "styles.css") -Force

Write-Host "[prepare_firebase_hosting] prepared: $publicDir"
