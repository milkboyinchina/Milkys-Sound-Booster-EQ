@echo off
REM ==============================================================================
REM Multi-Platform Build Script (Windows CMD) with Logging
REM ==============================================================================

setlocal enabledelayedexpansion

REM Ensure .env exists
if not exist .env (
    if exist .env.example (
        echo [*] Creating .env from .env.example...
        copy .env.example .env >nul
    )
)

REM Default variable values
set BUILD_LOGS_DIR=logs
set BUILD_OUTPUT_DIR=.build-outputs
set VERSION_CODE=26072401
set VERSION_NAME=0.1

REM Parse .env if available
if exist .env (
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        set "KEY=%%A"
        set "VAL=%%B"
        if "!KEY!"=="BUILD_LOGS_DIR" set "BUILD_LOGS_DIR=!VAL!"
        if "!KEY!"=="BUILD_OUTPUT_DIR" set "BUILD_OUTPUT_DIR=!VAL!"
        if "!KEY!"=="VERSION_CODE" set "VERSION_CODE=!VAL!"
        if "!KEY!"=="VERSION_NAME" set "VERSION_NAME=!VAL!"
    )
)

if not exist "%BUILD_LOGS_DIR%" mkdir "%BUILD_LOGS_DIR%"
if not exist "%BUILD_OUTPUT_DIR%" mkdir "%BUILD_OUTPUT_DIR%"

for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%
if "%TIMESTAMP%"=="" set TIMESTAMP=build_log

set LOG_FILE=%BUILD_LOGS_DIR%\build_%TIMESTAMP%.log
set LATEST_LOG=%BUILD_LOGS_DIR%\latest_build.log

echo ======================================================= > "%LOG_FILE%"
echo    Milkys Sound Booster ^& EQ - Windows Build Execution >> "%LOG_FILE%"
echo ======================================================= >> "%LOG_FILE%"
echo    Version Code    : %VERSION_CODE% >> "%LOG_FILE%"
echo    Version Name    : %VERSION_NAME% >> "%LOG_FILE%"
echo    Logs Directory  : %BUILD_LOGS_DIR% >> "%LOG_FILE%"
echo    Output Directory: %BUILD_OUTPUT_DIR% >> "%LOG_FILE%"
echo ======================================================= >> "%LOG_FILE%"

type "%LOG_FILE%"

REM Run Requirements Check
if exist scripts\check_requirements.bat (
    call scripts\check_requirements.bat >> "%LOG_FILE%" 2>&1
)

REM Determine Gradle command
set GRADLE_CMD=gradle
where gradle >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    if exist gradlew.bat (
        set GRADLE_CMD=gradlew.bat
    ) else (
        echo [!] Error: Neither 'gradle' nor 'gradlew.bat' found. >> "%LOG_FILE%"
        echo [!] Error: Neither 'gradle' nor 'gradlew.bat' found.
        exit /b 1
    )
)

set BUILD_TASK=%1
if "%BUILD_TASK%"=="" set BUILD_TASK=assembleDebug

echo.
echo [*] Executing: %GRADLE_CMD% %BUILD_TASK%...
echo [*] Executing: %GRADLE_CMD% %BUILD_TASK%... >> "%LOG_FILE%"

call %GRADLE_CMD% %BUILD_TASK% >> "%LOG_FILE%" 2>&1

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [+] Gradle build succeeded!
    echo [+] Gradle build succeeded! >> "%LOG_FILE%"
) else (
    echo.
    echo [!] Build FAILED. See details in log: %LOG_FILE%
    echo [!] Build FAILED. >> "%LOG_FILE%"
    copy /y "%LOG_FILE%" "%LATEST_LOG%" >nul
    exit /b 1
)

REM Copy APK output files
if exist app\build\outputs\apk (
    xcopy /s /e /y app\build\outputs\apk\* "%BUILD_OUTPUT_DIR%\" >nul
    echo [+] Copied build outputs to %BUILD_OUTPUT_DIR%
)

copy /y "%LOG_FILE%" "%LATEST_LOG%" >nul

echo.
echo =======================================================
echo    BUILD SUCCESSFUL
echo    Log File  : %LOG_FILE%
echo    APK Output: %BUILD_OUTPUT_DIR%
echo =======================================================
