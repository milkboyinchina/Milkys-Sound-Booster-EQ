# ==============================================================================
# Machine Requirements Check Script (PowerShell for Windows, macOS, Linux)
# ==============================================================================

$ErrorsCount = 0
$WarningsCount = 0

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "   Milkys Sound Booster & EQ - Environment Check" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

# 1. OS Check
$OS = [System.Runtime.InteropServices.RuntimeInformation]::OSDescription
Write-Host "`n[*] OS: $OS" -ForegroundColor Yellow

# 2. Java Check
Write-Host "`n[*] Checking Java Development Kit (JDK)..." -ForegroundColor Yellow
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    Write-Host "    [PASS] Java runtime found: $($javaCmd.Source)" -ForegroundColor Green
} else {
    Write-Host "    [FAIL] Java runtime (java) not found in PATH" -ForegroundColor Red
    $ErrorsCount++
}

$javacCmd = Get-Command javac -ErrorAction SilentlyContinue
if ($javacCmd) {
    Write-Host "    [PASS] Java compiler (javac) found: $($javacCmd.Source)" -ForegroundColor Green
} else {
    Write-Host "    [WARN] javac not found in PATH" -ForegroundColor Yellow
    $WarningsCount++
}

# 3. Android SDK Check
Write-Host "`n[*] Checking Android SDK..." -ForegroundColor Yellow
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }

if ($sdkPath -and (Test-Path $sdkPath)) {
    Write-Host "    [PASS] Android SDK found at: $sdkPath" -ForegroundColor Green
} else {
    Write-Host "    [WARN] ANDROID_HOME or ANDROID_SDK_ROOT environment variable not set" -ForegroundColor Yellow
    $WarningsCount++
}

# 4. Gradle Check
Write-Host "`n[*] Checking Gradle..." -ForegroundColor Yellow
$gradleCmd = Get-Command gradle -ErrorAction SilentlyContinue
if ($gradleCmd) {
    Write-Host "    [PASS] Global Gradle found: $($gradleCmd.Source)" -ForegroundColor Green
} elseif (Test-Path "./gradlew") {
    Write-Host "    [PASS] Local Gradle Wrapper (gradlew) found" -ForegroundColor Green
} elseif (Test-Path "./gradlew.bat") {
    Write-Host "    [PASS] Local Gradle Wrapper (gradlew.bat) found" -ForegroundColor Green
} else {
    Write-Host "    [FAIL] No Gradle or gradlew script found" -ForegroundColor Red
    $ErrorsCount++
}

# 5. .env Check
Write-Host "`n[*] Checking Environment Configuration (.env)..." -ForegroundColor Yellow
if (Test-Path ".env") {
    Write-Host "    [PASS] .env configuration file present" -ForegroundColor Green
} elseif (Test-Path ".env.example") {
    Write-Host "    [WARN] .env missing (.env.example will be copied during build)" -ForegroundColor Yellow
    $WarningsCount++
} else {
    Write-Host "    [FAIL] Missing .env and .env.example" -ForegroundColor Red
    $ErrorsCount++
}

Write-Host "`n=======================================================" -ForegroundColor Cyan
if ($ErrorsCount -eq 0) {
    Write-Host "   Environment Check PASSED ($WarningsCount warnings)" -ForegroundColor Green
    Write-Host "=======================================================" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "   Environment Check FAILED ($ErrorsCount errors, $WarningsCount warnings)" -ForegroundColor Red
    Write-Host "=======================================================" -ForegroundColor Cyan
    exit 1
}
