param(
  [string]$Version = "v0.1.1",
  [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path,
  [string]$FlutterExe = "flutter",
  [string]$FirebaseExe = "firebase",
  [string]$InnoSetupCompiler = ""
)

$ErrorActionPreference = "Stop"

$stageRoot = Join-Path $ProjectRoot "test_release\windows\$Version"
$appStage = Join-Path $stageRoot "app"
$releaseDir = Join-Path $ProjectRoot "build\windows\x64\runner\Release"
$issPath = Join-Path $ProjectRoot "packaging\windows\gah_installer.iss"
$installerOutput = Join-Path $stageRoot "gah-windows-$Version-setup.exe"

function Invoke-Step {
  param(
    [Parameter(Mandatory = $true)]
    [string]$FilePath,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
  )

  & $FilePath @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Command failed: $FilePath $($Arguments -join ' ')"
  }
}

function Resolve-InnoSetupCompiler {
  param([string]$RequestedPath)

  if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
    return $RequestedPath
  }

  $candidates = @(
    "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
    "D:\Program Files (x86)\Inno Setup 6\ISCC.exe"
  )

  foreach ($candidate in $candidates) {
    if (Test-Path $candidate) {
      return $candidate
    }
  }

  return ""
}

New-Item -ItemType Directory -Force -Path $appStage | Out-Null
$InnoSetupCompiler = Resolve-InnoSetupCompiler $InnoSetupCompiler

Push-Location $ProjectRoot
try {
  Invoke-Step $FlutterExe pub get
  Invoke-Step $FlutterExe analyze
  Invoke-Step $FlutterExe test
  Invoke-Step $FlutterExe build windows --release
  Invoke-Step $FlutterExe build web
  Invoke-Step $FirebaseExe deploy --only hosting

  if (-not (Test-Path $releaseDir)) {
    throw "Windows release directory not found: $releaseDir"
  }

  Copy-Item "$releaseDir\*" $appStage -Recurse -Force
  Set-Content -Path (Join-Path $stageRoot "VERSION.txt") -Value $Version -Encoding UTF8

  if (Test-Path $InnoSetupCompiler) {
    Invoke-Step $InnoSetupCompiler `
      "/DMyAppVersion=$Version" `
      "/DMyAppSourceDir=$appStage" `
      "/DMyAppOutputDir=$stageRoot" `
      "/DMyAppOutputBaseFilename=gah-windows-$Version-setup" `
      $issPath
    Set-Content -Path (Join-Path $stageRoot "INSTALL.txt") -Value @"
1. Run gah-windows-$Version-setup.exe
2. Follow the installer steps
3. Launch Google Auth Helper from the Start Menu
"@ -Encoding UTF8
  } else {
    Set-Content -Path (Join-Path $stageRoot "INSTALL.txt") -Value @"
1. Open test_release/windows/$Version/app
2. Run google_auth_helper.exe directly
3. Install Inno Setup and rerun this script to create the setup EXE
"@ -Encoding UTF8
    Write-Warning "Inno Setup compiler not found. Staged app files were created, but installer EXE was not built."
  }

  $checksumPath = Join-Path $stageRoot "checksums.txt"
  $checksumTempPath = Join-Path $stageRoot "checksums.txt.tmp"
  Get-ChildItem $stageRoot -File -Recurse |
    Get-FileHash -Algorithm SHA256 |
    ForEach-Object { "{0}  {1}" -f $_.Hash.ToLowerInvariant(), ($_.Path.Substring($stageRoot.Length + 1).Replace('\','/')) } |
    Set-Content -Path $checksumTempPath -Encoding UTF8
  Move-Item -Path $checksumTempPath -Destination $checksumPath -Force
}
finally {
  Pop-Location
}
