param(
  [string]$Version = "v0.1.2",
  [string]$ReleaseSummary = "헤더 플랫폼 표시는 제거했고 셸과 주요 화면을 한국어 중심으로 정리했으며 결과 업로드, ADB 경로 설정, 업데이트 설치 흐름을 반영했습니다.",
  [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path,
  [string]$FlutterExe = "flutter",
  [string]$InnoSetupCompiler = ""
)

$ErrorActionPreference = "Stop"

$stageRoot = Join-Path $ProjectRoot "test_release\windows\$Version"
$releaseDir = Join-Path $ProjectRoot "build\windows\x64\runner\Release"
$issPath = Join-Path $ProjectRoot "packaging\windows\gah_installer.iss"
$installerOutput = Join-Path $stageRoot "gah-windows-$Version-setup.exe"
$stagingRoot = Join-Path $env:TEMP "gah-windows-$Version-stage"
$appStage = Join-Path $stagingRoot "app"
$buildDate = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")

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

$InnoSetupCompiler = Resolve-InnoSetupCompiler $InnoSetupCompiler
if (-not (Test-Path $InnoSetupCompiler)) {
  throw "Inno Setup compiler was not found."
}

if (Test-Path $stageRoot) {
  Remove-Item $stageRoot -Recurse -Force
}
if (Test-Path $stagingRoot) {
  Remove-Item $stagingRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $stageRoot | Out-Null
New-Item -ItemType Directory -Force -Path $appStage | Out-Null

Push-Location $ProjectRoot
try {
  Invoke-Step $FlutterExe pub get
  Invoke-Step $FlutterExe analyze
  Invoke-Step $FlutterExe test
  Invoke-Step $FlutterExe build windows --release

  if (-not (Test-Path $releaseDir)) {
    throw "Windows release directory not found: $releaseDir"
  }

  Copy-Item "$releaseDir\*" $appStage -Recurse -Force

  Invoke-Step $InnoSetupCompiler `
    "/DMyAppVersion=$Version" `
    "/DMyAppSourceDir=$appStage" `
    "/DMyAppOutputDir=$stageRoot" `
    "/DMyAppOutputBaseFilename=gah-windows-$Version-setup" `
    $issPath

  Set-Content -Path (Join-Path $stageRoot "INSTALL.txt") -Value @"
Version: $Version
Built: $buildDate
Changes: $ReleaseSummary

1. Run gah-windows-$Version-setup.exe
2. Follow the installer steps
3. Launch Google Auth Helper from the Start Menu
"@ -Encoding UTF8

  if (-not (Test-Path $installerOutput)) {
    throw "Windows installer was not created: $installerOutput"
  }
}
finally {
  Pop-Location
  if (Test-Path $stagingRoot) {
    Remove-Item $stagingRoot -Recurse -Force
  }
}
