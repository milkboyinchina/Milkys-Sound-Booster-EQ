#!/usr/bin/env bash
# ==============================================================================
# Machine Requirements Check Script (Linux & macOS)
# ==============================================================================
# Verifies JDK, Android SDK, Gradle, and Environment configuration.

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

echo "======================================================="
echo "   Milkys Sound Booster & EQ - Environment Check"
echo "======================================================="

# 1. OS Detection
OS_TYPE="$(uname -s)"
echo -e "[*] Operating System: ${OS_TYPE}"
case "${OS_TYPE}" in
    Linux*)     echo -e "    ${GREEN}[PASS] Linux environment detected${NC}" ;;
    Darwin*)    echo -e "    ${GREEN}[PASS] macOS environment detected${NC}" ;;
    *)          echo -e "    ${YELLOW}[WARN] Unknown OS: ${OS_TYPE}${NC}"; WARNINGS=$((WARNINGS+1)) ;;
esac

# 2. Java / JDK Check
echo -e "\n[*] Checking Java Development Kit (JDK)..."
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo -e "    ${GREEN}[PASS] Java is installed: ${JAVA_VER}${NC}"
else
    echo -e "    ${RED}[FAIL] Java runtime (java) was not found in PATH${NC}"
    ERRORS=$((ERRORS+1))
fi

if command -v javac >/dev/null 2>&1; then
    JAVAC_VER=$(javac -version 2>&1)
    echo -e "    ${GREEN}[PASS] Java Compiler is available: ${JAVAC_VER}${NC}"
else
    echo -e "    ${YELLOW}[WARN] Java Compiler (javac) not found in PATH. Ensure JDK 17+ is configured.${NC}"
    WARNINGS=$((WARNINGS+1))
fi

# 3. Android SDK Check
echo -e "\n[*] Checking Android SDK environment variables..."
SDK_PATH="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
if [ -n "$SDK_PATH" ] && [ -d "$SDK_PATH" ]; then
    echo -e "    ${GREEN}[PASS] Android SDK found at: ${SDK_PATH}${NC}"
else
    echo -e "    ${YELLOW}[WARN] ANDROID_HOME or ANDROID_SDK_ROOT is not explicitly set or directory does not exist.${NC}"
    echo -e "           (If building in AI Studio environment, preconfigured SDK paths are managed automatically.)${NC}"
    WARNINGS=$((WARNINGS+1))
fi

# 4. Gradle Check
echo -e "\n[*] Checking Gradle build tools..."
if command -v gradle >/dev/null 2>&1; then
    GRADLE_VER=$(gradle -v | grep "Gradle " | head -n 1)
    echo -e "    ${GREEN}[PASS] Global Gradle available: ${GRADLE_VER}${NC}"
elif [ -f "./gradlew" ]; then
    echo -e "    ${GREEN}[PASS] Local Gradle Wrapper script (gradlew) detected.${NC}"
else
    echo -e "    ${RED}[FAIL] Neither global 'gradle' nor './gradlew' wrapper script was found.${NC}"
    ERRORS=$((ERRORS+1))
fi

# 5. Environment File (.env)
echo -e "\n[*] Checking Configuration Files..."
if [ -f ".env" ]; then
    echo -e "    ${GREEN}[PASS] .env file found.${NC}"
elif [ -f ".env.example" ]; then
    echo -e "    ${YELLOW}[WARN] .env not found. .env.example exists and will be auto-copied during build.${NC}"
    WARNINGS=$((WARNINGS+1))
else
    echo -e "    ${RED}[FAIL] Neither .env nor .env.example was found.${NC}"
    ERRORS=$((ERRORS+1))
fi

# 6. QC Tools Check (scrcpy ephemeral, gitleaks/actionlint/qc dirs — soft warnings only)
echo -e "\n[*] Checking QC Tools (scrcpy, gitleaks, actionlint, qc/ layout)..."

if command -v scrcpy >/dev/null 2>&1; then
    SCRCPY_VER=$(scrcpy --version 2>&1 | head -n 1)
    echo -e "    ${GREEN}[PASS] scrcpy available: ${SCRCPY_VER}${NC} (ephemeral manual QA, see qc_plan.md §5.5)"
else
    echo -e "    ${YELLOW}[WARN] scrcpy not found in PATH — manual QA will use adb screencap only (install: https://github.com/Genymobile/scrcpy)${NC}"
    WARNINGS=$((WARNINGS+1))
fi

if command -v gitleaks >/dev/null 2>&1; then
    GITLEAKS_VER=$(gitleaks version 2>&1 | head -n 1)
    echo -e "    ${GREEN}[PASS] gitleaks available: ${GITLEAKS_VER}${NC}"
else
    echo -e "    ${YELLOW}[WARN] gitleaks not found — secret scan will run in CI only${NC}"
    WARNINGS=$((WARNINGS+1))
fi

if command -v actionlint >/dev/null 2>&1; then
    ACTIONLINT_VER=$(actionlint --version 2>&1 | head -n 1)
    echo -e "    ${GREEN}[PASS] actionlint available: ${ACTIONLINT_VER}${NC}"
else
    echo -e "    ${YELLOW}[WARN] actionlint not found — workflow lint will run in CI only${NC}"
    WARNINGS=$((WARNINGS+1))
fi

if [ -d "qc" ] && [ -f "qc_plan.md" ] && [ -f "CHANGELOG.md" ]; then
    echo -e "    ${GREEN}[PASS] qc/ layout + qc_plan.md + CHANGELOG.md present${NC}"
else
    echo -e "    ${YELLOW}[WARN] qc/ or qc_plan.md/CHANGELOG.md missing — run scaffold per qc_plan.md §3${NC}"
    WARNINGS=$((WARNINGS+1))
fi

# Summary
echo -e "\n======================================================="
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}   Environment Check PASSED (${WARNINGS} warnings, 0 critical errors)${NC}"
    echo "======================================================="
    exit 0
else
    echo -e "${RED}   Environment Check FAILED (${ERRORS} errors, ${WARNINGS} warnings)${NC}"
    echo "======================================================="
    exit 1
fi
