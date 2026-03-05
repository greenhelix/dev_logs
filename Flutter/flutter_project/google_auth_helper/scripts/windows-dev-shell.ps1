Param()

$pathsToAdd = @(
  "C:\Program Files\nodejs",
  "$env:USERPROFILE\AppData\Roaming\npm"
)

foreach ($p in $pathsToAdd) {
  if ((Test-Path $p) -and ($env:Path -notmatch [regex]::Escape($p))) {
    $env:Path = "$p;$env:Path"
  }
}

Write-Host "[google_auth_helper] PATH bootstrap complete."
Write-Host "node: $(& 'C:\Program Files\nodejs\node.exe' -v)"
Write-Host "npm:  $(& 'C:\Program Files\nodejs\npm.cmd' -v)"
if (Get-Command firebase.cmd -ErrorAction SilentlyContinue) {
  Write-Host "firebase: $(firebase.cmd --version)"
} else {
  Write-Host "firebase: not found in PATH"
}
