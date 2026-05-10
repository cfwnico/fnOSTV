$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1")

adb logcat | Select-String -Pattern "FnOSTV|fnostv|com.fnostv.android4|chromium|WebView|AndroidRuntime"
