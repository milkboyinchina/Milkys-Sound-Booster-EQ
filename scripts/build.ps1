# ==============================================================================
# Multi-Platform Build Script (PowerShell for Windows, macOS, Linux) with Logging
# ==============================================================================

param (
    [string]$Task = "assembleDebug"
)

# 1. Load .env file
if (-not (Test-Path ".env") -and (Test-Path ".env.example")) {
    Write-Host "[*] Creating .env from .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
}

$envVars = @{}
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $envVars[$parts[0].Trim()] = $parts[1].Trim().Trim('"')
        }
    }
}

$BuildLogsDir = if ($envVars["BUILD_LOGS_DIR"]) { $envVars["BUILD_LOGS_DIR"] } else { "logs" }
$BuildOutputDir = if ($envVars["BUILD_OUTPUT_DIR"]) { $envVars["BUILD_OUTPUT_DIR"] } else { ".build-outputs" }
$VersionCode = if ($envVars["VERSION_CODE"]) { $envVars["VERSION_CODE"] } else { "3" }
$VersionName = if ($envVars["VERSION_NAME"]) { $envVars["VERSION_NAME"] } else { "3.0" }

if (-not (Test-Path $BuildLogsDir)) { New-Item -ItemType Directory -Path $BuildLogsDir | Out-Null }
if (-not (Test-Path $BuildOutputDir)) { New-Item -ItemType Directory -Path $BuildOutputDir | Out-Null }

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$LogFile = Join-Path $BuildLogsDir "build_$Timestamp.log"
$LatestLogFile = Join-Path $BuildLogsDir "latest_build.log"

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "   Milkys Sound Booster & EQ - PowerShell Build" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "   Timestamp       : $Timestamp"
Write-Host "   Version Code    : $VersionCode"
Write-Host "   Version Name    : $VersionName"
Write-Host "   Logs Directory  : $BuildLogsDir"
Write-Host "   Output Directory: $BuildOutputDir"
Write-Host "=======================================================" -ForegroundColor Cyan

# 2. Requirements Check
if (Test-Path "scripts/check_requirements.ps1") {
    & "scripts/check_requirements.ps1"
}

# 3. Determine Gradle Command
$GradleCmd = "gradle"
if (-not (Get-Command "gradle" -ErrorAction SilentlyContinue)) {
    if (Test-Path "./gradlew") {
        $GradleCmd = "./gradlew"
    } elseif (Test-Path "./gradlew.bat") {
        $GradleCmd = ".\gradlew.bat"
    } else {
        Write-Host "[!] Error: No Gradle tool found." -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n[*] Executing: $GradleCmd $Task" -ForegroundColor Yellow

# Execute build with logging
$process = Start-Process -FilePath $GradleCmd -ArgumentList $Task -NoNewWindow -PassThru -RedirectStandardOutput $LogFile -RedirectStandardError "$LogFile.err"
$process.WaitForExit()

Get-Content $LogFile | Write-Host

if ($process.ExitCode -eq 0) {
    Write-Host "`n[+] Build Succeeded!" -ForegroundColor Green
    
    if (Test-Path "app/build/outputs/apk") {
        Copy-Item -Path "app/build/outputs/apk/*" -Destination $BuildOutputDir -Recurse -Force
        Write-Host "    [+] Copied build outputs to $BuildOutputDir" -ForegroundColor Green
    }
    
    Copy-Item $LogFile $LatestLogFile -Force
    exit 0
} else {
    Write-Host "`n[!] Build Failed with Exit Code $($process.ExitCode)" -ForegroundColor Red
    if (Test-Path "$LogFile.err") { Get-Content "$LogFile.err" | Write-Host -ForegroundColor Red }
    Copy-Item $LogFile $LatestLogFile -Force
    exit $process.ExitCode
}
