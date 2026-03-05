Param(
  [string]$HostName = "127.0.0.1",
  [int]$Port = 8020,
  [string]$PythonExe = "python"
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
  Param(
    [string]$Method,
    [string]$Url
  )
  try {
    $res = Invoke-RestMethod -Method $Method -Uri $Url -TimeoutSec 20
    return @{
      ok = $true
      detail = ($res | ConvertTo-Json -Depth 6 -Compress)
    }
  } catch {
    return @{
      ok = $false
      detail = $_.Exception.Message
    }
  }
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$baseUrl = "http://$HostName`:$Port"
$serverOutLog = Join-Path $projectRoot "workspace\logs\smoke_server.out.log"
$serverErrLog = Join-Path $projectRoot "workspace\logs\smoke_server.err.log"

if (-not (Test-Path (Split-Path -Parent $serverOutLog))) {
  New-Item -ItemType Directory -Path (Split-Path -Parent $serverOutLog) -Force | Out-Null
}

Push-Location $projectRoot
$proc = $null
try {
  $proc = Start-Process -FilePath $PythonExe `
    -ArgumentList @("-m", "uvicorn", "app.main:app", "--host", $HostName, "--port", "$Port") `
    -PassThru `
    -WindowStyle Hidden `
    -RedirectStandardOutput $serverOutLog `
    -RedirectStandardError $serverErrLog

  $ready = $false
  for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Milliseconds 500
    $ping = Invoke-Api -Method "GET" -Url "$baseUrl/api/health"
    if ($ping.ok) {
      $ready = $true
      break
    }
  }

  if (-not $ready) {
    Write-Host "[FAIL] 서버 기동 확인 실패. 로그: $serverOutLog / $serverErrLog"
    exit 1
  }

  $checks = @(
    @{ name = "health"; method = "GET"; url = "$baseUrl/api/health" },
    @{ name = "tools"; method = "GET"; url = "$baseUrl/api/tools" },
    @{ name = "watcher"; method = "GET"; url = "$baseUrl/api/watcher/status" },
    @{ name = "firebase_status"; method = "GET"; url = "$baseUrl/api/firebase/status" },
    @{ name = "adb_devices"; method = "GET"; url = "$baseUrl/api/adb/devices" },
    @{ name = "sync_monitor"; method = "POST"; url = "$baseUrl/api/firebase/sync/monitor" },
    @{ name = "sync_runs"; method = "POST"; url = "$baseUrl/api/firebase/sync/runs?limit=10" }
  )

  $results = @()
  foreach ($check in $checks) {
    $r = Invoke-Api -Method $check.method -Url $check.url
    $results += [PSCustomObject]@{
      name = $check.name
      ok = $r.ok
      detail = $r.detail
    }
  }

  Write-Host ""
  Write-Host "=== GAH Smoke Test ==="
  $results | Select-Object name, ok | Format-Table -AutoSize
  Write-Host ""

  foreach ($item in $results) {
    Write-Host ("[{0}] {1}" -f ($(if ($item.ok) { "OK" } else { "ERR" }), $item.name))
    Write-Host $item.detail
    Write-Host ""
  }
}
finally {
  if ($proc -and -not $proc.HasExited) {
    Stop-Process -Id $proc.Id -Force
  }
  Pop-Location
}
