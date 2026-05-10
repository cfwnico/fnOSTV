$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1")

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$LogFile = Join-Path $ProjectRoot ".tooling\debug-error-watch.log"

New-Item -ItemType Directory -Force (Split-Path $LogFile) | Out-Null
Write-Host "Watching Android errors. Log file: $LogFile"
Write-Host "Press Ctrl+C to stop."

adb logcat -v time AndroidRuntime:E System.err:W chromium:E WebView:E FnOSTV:D com.fnostv.android4:D *:S |
    Tee-Object -FilePath $LogFile -Append
