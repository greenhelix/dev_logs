param(
  [string]$Version = "v0.1.1",
  [string]$ReleaseRepoUrl = "https://github.com/greenhelix/GAH-Release-Repo.git",
  [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path,
  [string]$CloneDir = "",
  [switch]$CreateGitHubRelease
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($CloneDir)) {
  $CloneDir = Join-Path $env:TEMP "GAH-Release-Repo"
}

if (Test-Path $CloneDir) {
  git -C $CloneDir pull --ff-only
} else {
  git clone $ReleaseRepoUrl $CloneDir
}

$targetDir = Join-Path $CloneDir "releases\$Version"
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

Copy-Item (Join-Path $ProjectRoot "test_release\windows\$Version\*") $targetDir -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item (Join-Path $ProjectRoot "test_release\linux\$Version\*") $targetDir -Recurse -Force -ErrorAction SilentlyContinue

git -C $CloneDir add .
git -C $CloneDir commit -m "Release $Version" 2>$null
git -C $CloneDir push origin main

if ($CreateGitHubRelease -and (Get-Command gh -ErrorAction SilentlyContinue)) {
  gh release create $Version --repo greenhelix/GAH-Release-Repo --generate-notes "$targetDir\*"
}
