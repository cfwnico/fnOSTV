$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$ToolingRoot = Join-Path $ProjectRoot ".tooling"
$Downloads = Join-Path $ToolingRoot "downloads"
$AndroidSdkRoot = Join-Path $ToolingRoot "android-sdk"

$GradleVersion = "6.7.1"
$GradleZip = Join-Path $Downloads "gradle-$GradleVersion-bin.zip"
$CmdlineZip = Join-Path $Downloads "commandlinetools-win.zip"
$JdkZip = Join-Path $Downloads "temurin-jdk11.zip"

New-Item -ItemType Directory -Force $Downloads | Out-Null

if (-not (Test-Path $GradleZip)) {
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $GradleZip
}
if (-not (Test-Path $CmdlineZip)) {
    Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip" -OutFile $CmdlineZip
}
if (-not (Test-Path $JdkZip)) {
    Invoke-WebRequest -Uri "https://api.adoptium.net/v3/binary/latest/11/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" -OutFile $JdkZip
}

if (-not (Test-Path (Join-Path $ToolingRoot "gradle\gradle-$GradleVersion"))) {
    Expand-Archive -Force $GradleZip (Join-Path $ToolingRoot "gradle")
}
if (-not (Test-Path (Join-Path $ToolingRoot "jdk\jdk-11.0.31+11"))) {
    Expand-Archive -Force $JdkZip (Join-Path $ToolingRoot "jdk")
}
if (-not (Test-Path (Join-Path $AndroidSdkRoot "cmdline-tools\latest"))) {
    $TempCmdline = Join-Path $ToolingRoot "android-cmdline"
    Expand-Archive -Force $CmdlineZip $TempCmdline
    New-Item -ItemType Directory -Force (Join-Path $AndroidSdkRoot "cmdline-tools") | Out-Null
    Move-Item -Force (Join-Path $TempCmdline "cmdline-tools") (Join-Path $AndroidSdkRoot "cmdline-tools\latest")
}

. (Join-Path $PSScriptRoot "env.ps1")

1..20 | ForEach-Object { "y" } | & "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=$env:ANDROID_HOME --licenses
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=$env:ANDROID_HOME "platform-tools" "platforms;android-28" "build-tools;28.0.3"

Write-Host "Android build environment is ready."
Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
