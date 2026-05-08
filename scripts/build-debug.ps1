$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1")

gradle --no-daemon assembleDebug
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$Apk = Resolve-Path (Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk")
Write-Host "Debug APK: $Apk"
