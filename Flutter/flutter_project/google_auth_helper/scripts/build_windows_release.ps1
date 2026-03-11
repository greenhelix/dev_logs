param(
  [string]$Version = "v0.1.1",
  [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path,
  [string]$FlutterExe = "flutter",
  [string]$InnoSetupCompiler = "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
)

$ErrorActionPreference = "Stop"

$stageRoot = Join-Path $ProjectRoot "test_release\windows\$Version"
$appStage = Join-Path $stageRoot "app"
$releaseDir = Join-Path $ProjectRoot "build\windows\x64\runner\Release"
$issPath = Join-Path $ProjectRoot "packaging\windows\gah_installer.iss"
$installerOutput = Join-Path $stageRoot "gah-windows-$Version-setup.exe"

New-Item -ItemType Directory -Force -Path $appStage | Out-Null

Push-Location $ProjectRoot
try {
  & $FlutterExe pub get
  & $FlutterExe build windows --release

  if (-not (Test-Path $releaseDir)) {
    throw "Windows release directory not found: $releaseDir"
  }

  Copy-Item "$releaseDir\*" $appStage -Recurse -Force
  Set-Content -Path (Join-Path $stageRoot "VERSION.txt") -Value $Version -Encoding UTF8
  Set-Content -Path (Join-Path $stageRoot "INSTALL.txt") -Value @"
1. Run gah-windows-$Version-setup.exe
2. Follow the installer steps
3. Launch Google Auth Helper from the Start Menu
"@ -Encoding UTF8

  Get-ChildItem $stageRoot -File -Recurse |
    Get-FileHash -Algorithm SHA256 |
    ForEach-Object { "{0}  {1}" -f $_.Hash.ToLowerInvariant(), ($_.Path.Substring($stageRoot.Length + 1).Replace('\','/')) } |
    Set-Content -Path (Join-Path $stageRoot "checksums.txt") -Encoding UTF8

  if (Test-Path $InnoSetupCompiler) {
    & $InnoSetupCompiler `
      "/DMyAppVersion=$Version" `
      "/DMyAppSourceDir=$appStage" `
      "/DMyAppOutputDir=$stageRoot" `
      "/DMyAppOutputBaseFilename=gah-windows-$Version-setup" `
      $issPath
  } else {
    Write-Warning "Inno Setup compiler not found. Staged app files were created, but installer EXE was not built."
  }
}
finally {
  Pop-Location
}
