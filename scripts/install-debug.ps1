$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1")

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $Apk)) {
    & (Join-Path $PSScriptRoot "build-debug.ps1")
}

adb install -r $Apk
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

adb shell monkey -p com.fnostv.android4 -c android.intent.category.LAUNCHER 1
