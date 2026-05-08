$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1")

$AvdName = "fnostv_api19"
$Emulator = Join-Path $env:ANDROID_HOME "emulator\emulator.exe"

if (-not (Test-Path (Join-Path $env:ANDROID_AVD_HOME "$AvdName.avd"))) {
    & (Join-Path $PSScriptRoot "setup-emulator.ps1")
}

$Running = (& adb devices) -match "^emulator-\d+\s+device"
if ($Running) {
    Write-Host "An emulator is already running."
} else {
    Start-Process -FilePath $Emulator -ArgumentList @(
        "-avd", $AvdName,
        "-no-snapshot-load",
        "-gpu", "swiftshader_indirect",
        "-netdelay", "none",
        "-netspeed", "full"
    )
    Write-Host "Starting emulator: $AvdName"
}

$Booted = $false
for ($i = 0; $i -lt 90; $i++) {
    Start-Sleep -Seconds 2
    $Devices = (& adb devices) -join "`n"
    if ($Devices -match "emulator-\d+\s+device") {
        $BootComplete = (& adb shell getprop sys.boot_completed 2>$null).Trim()
        if ($BootComplete -eq "1") {
            $Booted = $true
            break
        }
    }
}

if (-not $Booted) {
    throw "Emulator did not finish booting in time. It may still be starting; run adb devices to check."
}

Write-Host "Emulator booted: $AvdName"
