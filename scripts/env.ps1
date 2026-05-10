$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$ToolingRoot = Join-Path $ProjectRoot ".tooling"
$JdkRoot = Join-Path $ToolingRoot "jdk\jdk-11.0.31+11"
$GradleRoot = Join-Path $ToolingRoot "gradle\gradle-6.7.1"
$AndroidSdkRoot = Join-Path $ToolingRoot "android-sdk"

if (-not (Test-Path $JdkRoot)) {
    throw "Missing JDK at $JdkRoot. Run scripts\setup-android-env.ps1 first."
}
if (-not (Test-Path $GradleRoot)) {
    throw "Missing Gradle at $GradleRoot. Run scripts\setup-android-env.ps1 first."
}
if (-not (Test-Path $AndroidSdkRoot)) {
    throw "Missing Android SDK at $AndroidSdkRoot. Run scripts\setup-android-env.ps1 first."
}

$env:JAVA_HOME = (Resolve-Path $JdkRoot).Path
$env:ANDROID_HOME = (Resolve-Path $AndroidSdkRoot).Path
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$GradleRoot\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

$localProperties = Join-Path $ProjectRoot "local.properties"
$escapedSdk = $env:ANDROID_HOME.Replace("\", "\\").Replace(":", "\:")
[System.IO.File]::WriteAllText($localProperties, "sdk.dir=$escapedSdk`r`n", [System.Text.Encoding]::ASCII)
