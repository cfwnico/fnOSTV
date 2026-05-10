$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1")

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$KeystoreProperties = Join-Path $ProjectRoot "keystore.properties"

if (-not (Test-Path $KeystoreProperties)) {
    & (Join-Path $PSScriptRoot "create-release-keystore.ps1")
}

gradle --no-daemon assembleRelease
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$Apk = Resolve-Path (Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk")
Write-Host "Release APK: $Apk"
