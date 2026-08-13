# Build and install ZhAPI test app (normal install)
# Usage: .\scripts\install_debug.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Apk = Join-Path $Root "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $Apk)) {
    Write-Host "APK not found, building..."
    Push-Location $Root
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Pop-Location
}

Write-Host "Installing..."
adb install -r $Apk
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Done. Launch: adb shell am start -n com.example.zhapitest/.MainActivity"
