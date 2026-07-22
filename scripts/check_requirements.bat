@echo off
REM ==============================================================================
REM Machine Requirements Check Script (Windows CMD)
REM ==============================================================================

set ERRORS=0
set WARNINGS=0

echo =======================================================
echo    Milkys Sound Booster ^& EQ - Windows Environment Check
echo =======================================================

echo.
echo [*] Checking Java Development Kit (JDK)...
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo     [PASS] Java runtime is installed.
    java -version 2>&1 | findstr /I "version"
) else (
    echo     [FAIL] Java runtime (java) was not found in PATH.
    set /A ERRORS+=1
)

where javac >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo     [PASS] Java Compiler (javac) is available.
) else (
    echo     [WARN] Java Compiler (javac) not found in PATH.
    set /A WARNINGS+=1
)

echo.
echo [*] Checking Android SDK Environment...
if defined ANDROID_HOME (
    echo     [PASS] ANDROID_HOME is set to %ANDROID_HOME%
) else if defined ANDROID_SDK_ROOT (
    echo     [PASS] ANDROID_SDK_ROOT is set to %ANDROID_SDK_ROOT%
) else (
    echo     [WARN] Neither ANDROID_HOME nor ANDROID_SDK_ROOT is set.
    set /A WARNINGS+=1
)

echo.
echo [*] Checking Gradle...
where gradle >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo     [PASS] Global Gradle is available.
) else if exist gradlew.bat (
    echo     [PASS] Local gradlew.bat wrapper found.
) else (
    echo     [FAIL] Neither 'gradle' nor 'gradlew.bat' was found.
    set /A ERRORS+=1
)

echo.
echo [*] Checking .env configuration...
if exist .env (
    echo     [PASS] .env file found.
) else if exist .env.example (
    echo     [WARN] .env not found (.env.example present, will copy on build).
    set /A WARNINGS+=1
) else (
    echo     [FAIL] Neither .env nor .env.example found.
    set /A ERRORS+=1
)

echo.
echo =======================================================
if %ERRORS% EQU 0 (
    echo    Environment Check PASSED (%WARNINGS% warnings)
    echo =======================================================
    exit /b 0
) else (
    echo    Environment Check FAILED (%ERRORS% errors, %WARNINGS% warnings)
    echo =======================================================
    exit /b 1
)
