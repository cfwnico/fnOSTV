$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1")

$AvdName = "fnostv_api19"
$SystemImage = "system-images;android-19;default;x86"

1..20 | ForEach-Object { "y" } | sdkmanager.bat --sdk_root=$env:ANDROID_HOME --licenses
sdkmanager.bat --sdk_root=$env:ANDROID_HOME "emulator" $SystemImage
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$ExistingAvds = (& avdmanager.bat list avd) -join "`n"
if ($ExistingAvds -notmatch "Name:\s+$AvdName") {
    avdmanager.bat create avd --force --name $AvdName --package $SystemImage --device "tv_720p"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

$ConfigPath = Join-Path $env:ANDROID_AVD_HOME "$AvdName.avd\config.ini"
if (Test-Path $ConfigPath) {
    $Config = Get-Content $ConfigPath
    $Config = $Config -replace '^avd\.id=.*$', "avd.id=$AvdName"
    $Config = $Config -replace '^avd\.name=.*$', "avd.name=$AvdName"
    $Config = $Config -replace '^hw\.initialOrientation=.*$', "hw.initialOrientation=landscape"
    $Config = $Config -replace '^hw\.gpu\.enabled=.*$', "hw.gpu.enabled=yes"
    $Config = $Config -replace '^hw\.gpu\.mode=.*$', "hw.gpu.mode=swiftshader_indirect"
    [System.IO.File]::WriteAllText($ConfigPath, (($Config -join "`r`n") + "`r`n"), [System.Text.Encoding]::ASCII)
}

Write-Host "Android 4.4 emulator is ready: $AvdName"
Write-Host "AVD_HOME=$env:ANDROID_AVD_HOME"
