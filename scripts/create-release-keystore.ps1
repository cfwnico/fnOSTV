$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1")

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$SigningDir = Join-Path $ProjectRoot "signing"
$KeystorePath = Join-Path $SigningDir "fnostv-release.jks"
$KeystoreProperties = Join-Path $ProjectRoot "keystore.properties"

New-Item -ItemType Directory -Force $SigningDir | Out-Null

if ((Test-Path $KeystorePath) -and (Test-Path $KeystoreProperties)) {
    Write-Host "Release keystore already exists: $KeystorePath"
    Write-Host "Signing properties already exist: $KeystoreProperties"
    exit 0
}

$StorePassword = ([Guid]::NewGuid().ToString("N") + [Guid]::NewGuid().ToString("N")).Substring(0, 48)
$KeyPassword = ([Guid]::NewGuid().ToString("N") + [Guid]::NewGuid().ToString("N")).Substring(0, 48)
$Alias = "fnostv"

& "$env:JAVA_HOME\bin\keytool.exe" `
    -genkeypair `
    -v `
    -keystore $KeystorePath `
    -storetype JKS `
    -storepass $StorePassword `
    -keypass $KeyPassword `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -dname "CN=fnOSTV, OU=fnOSTV, O=XDORG-N1, L=Unknown, S=Unknown, C=CN"

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$RelativeKeystore = "signing/fnostv-release.jks"
$Content = @(
    "storeFile=$RelativeKeystore"
    "storePassword=$StorePassword"
    "keyAlias=$Alias"
    "keyPassword=$KeyPassword"
) -join "`r`n"

[System.IO.File]::WriteAllText($KeystoreProperties, "$Content`r`n", [System.Text.Encoding]::ASCII)

Write-Host "Created release keystore: $KeystorePath"
Write-Host "Created signing properties: $KeystoreProperties"
Write-Host "Keep both files safe. Losing them prevents signing updates with the same key."
