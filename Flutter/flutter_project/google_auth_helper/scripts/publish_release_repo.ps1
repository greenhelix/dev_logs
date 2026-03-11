param(
  [string]$Version = "v0.1.2",
  [string]$ReleaseSummary = "Windows auto test is disabled, platform icons stay visible on focus, and release artifacts are simplified.",
  [string]$ReleaseRepoUrl = "https://github.com/greenhelix/GAH-Release-Repo.git",
  [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path,
  [string]$CloneDir = "",
  [switch]$CreateGitHubRelease
)

$ErrorActionPreference = "Stop"

function Invoke-Git {
  param(
    [Parameter(Mandatory = $true)]
    [string[]]$Arguments
  )

  git @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "git command failed: git $($Arguments -join ' ')"
  }
}

function Copy-ReleaseFiles {
  param(
    [Parameter(Mandatory = $true)]
    [string]$SourceDir,
    [Parameter(Mandatory = $true)]
    [string]$TargetDir,
    [Parameter(Mandatory = $true)]
    [string[]]$Patterns
  )

  if (-not (Test-Path $SourceDir)) {
    return @()
  }

  if (Test-Path $TargetDir) {
    Remove-Item $TargetDir -Recurse -Force
  }
  New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

  $copied = @()
  foreach ($pattern in $Patterns) {
    Get-ChildItem -Path (Join-Path $SourceDir $pattern) -File -ErrorAction SilentlyContinue |
      ForEach-Object {
        Copy-Item $_.FullName $TargetDir -Force
        $copied += (Join-Path $TargetDir $_.Name)
      }
  }

  if ($copied.Count -eq 0) {
    Remove-Item $TargetDir -Recurse -Force
  }

  return $copied
}

if ([string]::IsNullOrWhiteSpace($CloneDir)) {
  $CloneDir = Join-Path $env:TEMP "GAH-Release-Repo"
}

if (Test-Path $CloneDir) {
  Invoke-Git -Arguments @("-C", $CloneDir, "pull", "--ff-only")
} else {
  Invoke-Git -Arguments @("clone", $ReleaseRepoUrl, $CloneDir)
}

$windowsTargetDir = Join-Path $CloneDir "windows\$Version"
$ubuntuTargetDir = Join-Path $CloneDir "ubuntu\$Version"
$windowsFiles = Copy-ReleaseFiles `
  -SourceDir (Join-Path $ProjectRoot "test_release\windows\$Version") `
  -TargetDir $windowsTargetDir `
  -Patterns @("*.exe", "INSTALL.txt")
$ubuntuFiles = Copy-ReleaseFiles `
  -SourceDir (Join-Path $ProjectRoot "test_release\linux\$Version") `
  -TargetDir $ubuntuTargetDir `
  -Patterns @("*.deb", "INSTALL.txt")

$repoReadme = @"
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

- ${Version}: $ReleaseSummary
"@
Set-Content -Path (Join-Path $CloneDir "README.md") -Value $repoReadme -Encoding UTF8

Invoke-Git -Arguments @("-C", $CloneDir, "add", ".")
git -C $CloneDir diff --cached --quiet
if ($LASTEXITCODE -eq 0) {
  $statusOutput = git -C $CloneDir status --short --branch
  if ($statusOutput -notmatch "ahead") {
    Write-Host "No release repo changes to commit or push."
    exit 0
  }
} else {
  Invoke-Git -Arguments @("-C", $CloneDir, "commit", "-m", "Release $Version")
}
try {
  Invoke-Git -Arguments @("-C", $CloneDir, "push", "origin", "main")
} catch {
  Write-Warning "Push failed. Local release repo clone is ready at $CloneDir"
  Write-Warning $_
  exit 0
}

if ($CreateGitHubRelease -and (Get-Command gh -ErrorAction SilentlyContinue)) {
  $assets = @()
  $assets += $windowsFiles
  $assets += $ubuntuFiles
  if ($assets.Count -gt 0) {
    & gh release create $Version --repo greenhelix/GAH-Release-Repo --notes $ReleaseSummary @assets
  }
}
