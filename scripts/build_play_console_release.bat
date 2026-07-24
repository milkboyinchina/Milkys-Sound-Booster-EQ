@echo off
REM ==============================================================================
REM Google Play Console Release Build Script (Windows CMD)
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
set APPLICATION_ID=com.milkys.soundbooster
set APP_NAME=Milkys Sound Booster & EQ
set VERSION_CODE=26072301
set VERSION_NAME=0.1
set BUILD_LOGS_DIR=logs
set BUILD_OUTPUT_DIR=.build-outputs

REM Parse .env if available
if exist .env (
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        set "KEY=%%A"
        set "VAL=%%B"
        if "!KEY!"=="APPLICATION_ID" set "APPLICATION_ID=!VAL!"
        if "!KEY!"=="APP_NAME" set "APP_NAME=!VAL!"
        if "!KEY!"=="VERSION_CODE" set "VERSION_CODE=!VAL!"
        if "!KEY!"=="VERSION_NAME" set "VERSION_NAME=!VAL!"
        if "!KEY!"=="BUILD_LOGS_DIR" set "BUILD_LOGS_DIR=!VAL!"
        if "!KEY!"=="BUILD_OUTPUT_DIR" set "BUILD_OUTPUT_DIR=!VAL!"
    )
)

if not exist "%BUILD_LOGS_DIR%" mkdir "%BUILD_LOGS_DIR%"
if not exist "%BUILD_OUTPUT_DIR%\playstore" mkdir "%BUILD_OUTPUT_DIR%\playstore"

for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value 2>nul') do set datetime=%%I
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%
if "%TIMESTAMP%"=="" set TIMESTAMP=release_build_log

set LOG_FILE=%BUILD_LOGS_DIR%\build_play_console_release_%TIMESTAMP%.log
set LATEST_LOG=%BUILD_LOGS_DIR%\latest_release_build.log

echo ======================================================= > "%LOG_FILE%"
echo    Google Play Console Release Build - Windows Log >> "%LOG_FILE%"
echo ======================================================= >> "%LOG_FILE%"
echo    Application ID : %APPLICATION_ID% >> "%LOG_FILE%"
echo    Version Code   : %VERSION_CODE% >> "%LOG_FILE%"
echo    Version Name   : %VERSION_NAME% >> "%LOG_FILE%"
echo    Output Dir     : %BUILD_OUTPUT_DIR%\playstore >> "%LOG_FILE%"
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

echo.
echo [*] Executing: %GRADLE_CMD% bundleRelease assembleRelease...
echo [*] Executing: %GRADLE_CMD% bundleRelease assembleRelease... >> "%LOG_FILE%"

call %GRADLE_CMD% bundleRelease assembleRelease >> "%LOG_FILE%" 2>&1

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [+] Play Console release compilation succeeded!
    echo [+] Play Console release compilation succeeded! >> "%LOG_FILE%"

    if exist app\build\outputs\bundle\release\app-release.aab (
        copy /y app\build\outputs\bundle\release\app-release.aab "%BUILD_OUTPUT_DIR%\playstore\app-release.aab" >nul
        copy /y app\build\outputs\bundle\release\app-release.aab "%BUILD_OUTPUT_DIR%\playstore\%APPLICATION_ID%-v%VERSION_NAME%-release.aab" >nul
        echo [+] App Bundle (.aab) created in %BUILD_OUTPUT_DIR%\playstore\
    )

    if exist app\build\outputs\apk\release\app-release.apk (
        copy /y app\build\outputs\apk\release\app-release.apk "%BUILD_OUTPUT_DIR%\playstore\app-release.apk" >nul
        copy /y app\build\outputs\apk\release\app-release.apk "%BUILD_OUTPUT_DIR%\playstore\%APPLICATION_ID%-v%VERSION_NAME%-release.apk" >nul
        echo [+] Release APK created in %BUILD_OUTPUT_DIR%\playstore\
    )
) else (
    echo.
    echo [!] Release build FAILED. See log for details: %LOG_FILE%
    echo [!] Release build FAILED. >> "%LOG_FILE%"
    copy /y "%LOG_FILE%" "%LATEST_LOG%" >nul
    exit /b 1
)

copy /y "%LOG_FILE%" "%LATEST_LOG%" >nul

echo.
echo =======================================================
echo    PLAY CONSOLE RELEASE BUILD SUCCESSFUL
echo    Application ID : %APPLICATION_ID%
echo    AAB Output     : %BUILD_OUTPUT_DIR%\playstore\app-release.aab
echo    APK Output     : %BUILD_OUTPUT_DIR%\playstore\app-release.apk
echo =======================================================
