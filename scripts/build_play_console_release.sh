#!/usr/bin/env bash
# ==============================================================================
# Google Play Console Release Build Script (AAB & APK)
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}"

# 1. Load .env file or copy from .env.example
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
    echo "[*] Creating .env from .env.example..."
    cp .env.example .env
fi

# Automatically bump VERSION_CODE and VERSION_NAME in .env for every build (skip on tag source of truth)
if [ "${SKIP_VERSION_BUMP:-0}" = "1" ]; then
    echo "[*] SKIP_VERSION_BUMP=1 — skipping bump_version.py (tag is source of truth)"
elif [ -f "${SCRIPT_DIR}/bump_version.py" ]; then
    python3 "${SCRIPT_DIR}/bump_version.py"
fi

get_env_var() {
    local var_name="$1"
    local default_val="$2"
    if [ -f ".env" ]; then
        local val
        val=$(grep -E "^${var_name}=" .env | cut -d '=' -f2- | tr -d '\r' | tr -d '"')
        if [ -n "$val" ]; then
            echo "$val"
            return
        fi
    fi
    echo "$default_val"
}

APPLICATION_ID=$(get_env_var "APPLICATION_ID" "com.milkys.soundbooster")
APP_NAME=$(get_env_var "APP_NAME" "Milkys Sound Booster & EQ")
VERSION_CODE=$(get_env_var "VERSION_CODE" "26072401")
VERSION_NAME=$(get_env_var "VERSION_NAME" "0.1")
BUILD_LOGS_DIR=$(get_env_var "BUILD_LOGS_DIR" "logs")
BUILD_OUTPUT_DIR=$(get_env_var "BUILD_OUTPUT_DIR" ".build-outputs")

mkdir -p "${BUILD_LOGS_DIR}"
mkdir -p "${BUILD_OUTPUT_DIR}/playstore"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${BUILD_LOGS_DIR}/build_play_console_release_${TIMESTAMP}.log"
LATEST_LOG_FILE="${BUILD_LOGS_DIR}/latest_release_build.log"

echo "=======================================================" | tee "${LOG_FILE}"
echo "   Google Play Console Release Build - Execution Log" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"
echo "   App Name       : ${APP_NAME}" | tee -a "${LOG_FILE}"
echo "   Application ID : ${APPLICATION_ID}" | tee -a "${LOG_FILE}"
echo "   Version Code   : ${VERSION_CODE}" | tee -a "${LOG_FILE}"
echo "   Version Name   : ${VERSION_NAME}" | tee -a "${LOG_FILE}"
echo "   Output Dir     : ${BUILD_OUTPUT_DIR}/playstore" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"

# 2. Check Requirements
echo -e "\n[*] Checking system requirements..." | tee -a "${LOG_FILE}"
if [ -f "${SCRIPT_DIR}/check_requirements.sh" ]; then
    bash "${SCRIPT_DIR}/check_requirements.sh" 2>&1 | tee -a "${LOG_FILE}"
fi

# 3. Determine Gradle command
GRADLE_CMD="gradle"
if ! command -v gradle >/dev/null 2>&1; then
    if [ -f "./gradlew" ]; then
        chmod +x ./gradlew
        GRADLE_CMD="./gradlew"
    else
        echo "[!] Error: Neither 'gradle' nor './gradlew' is available." | tee -a "${LOG_FILE}"
        exit 1
    fi
fi

# 4. Execute Release Build Tasks (AAB App Bundle & Release APK)
export BUILD_TARGET="playstore"
export INCLUDE_GOOGLE_ADS="true"
export APPLICATION_ID="${APPLICATION_ID}"

echo -e "\n[*] Building Google Play Release App Bundle (.aab) & Release APK..." | tee -a "${LOG_FILE}"

if ${GRADLE_CMD} bundleRelease assembleRelease 2>&1 | tee -a "${LOG_FILE}"; then
    echo -e "\n[+] Gradle release compilation succeeded!" | tee -a "${LOG_FILE}"

    # Copy Android App Bundle (.aab)
    if [ -f "app/build/outputs/bundle/release/app-release.aab" ]; then
        cp app/build/outputs/bundle/release/app-release.aab "${BUILD_OUTPUT_DIR}/playstore/app-release.aab"
        cp app/build/outputs/bundle/release/app-release.aab "${BUILD_OUTPUT_DIR}/playstore/${APPLICATION_ID}-v${VERSION_NAME}-release.aab"
        cp app/build/outputs/bundle/release/app-release.aab "${BUILD_OUTPUT_DIR}/${APPLICATION_ID}-v${VERSION_NAME}-release.aab"
        echo "    [+] App Bundle ready: ${BUILD_OUTPUT_DIR}/playstore/${APPLICATION_ID}-v${VERSION_NAME}-release.aab" | tee -a "${LOG_FILE}"
    fi

    # Copy Release APK
    for apk in app/build/outputs/apk/release/*.apk; do
        if [ -f "$apk" ]; then
            cp "$apk" "${BUILD_OUTPUT_DIR}/playstore/app-release.apk"
            cp "$apk" "${BUILD_OUTPUT_DIR}/playstore/${APPLICATION_ID}-v${VERSION_NAME}-release.apk"
            cp "$apk" "${BUILD_OUTPUT_DIR}/${APPLICATION_ID}-v${VERSION_NAME}-release.apk"
            echo "    [+] Release APK ready: ${BUILD_OUTPUT_DIR}/playstore/${APPLICATION_ID}-v${VERSION_NAME}-release.apk" | tee -a "${LOG_FILE}"
        fi
    done
else
    echo -e "\n[!] Release build FAILED. Check log file at: ${LOG_FILE}" | tee -a "${LOG_FILE}"
    cp "${LOG_FILE}" "${LATEST_LOG_FILE}"
    exit 1
fi

cp "${LOG_FILE}" "${LATEST_LOG_FILE}"

echo -e "\n=======================================================" | tee -a "${LOG_FILE}"
echo "   PLAY CONSOLE RELEASE BUILD SUCCESSFUL" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"
echo "   Application ID : ${APPLICATION_ID}" | tee -a "${LOG_FILE}"
echo "   AAB File Path  : ${BUILD_OUTPUT_DIR}/playstore/app-release.aab" | tee -a "${LOG_FILE}"
echo "   APK File Path  : ${BUILD_OUTPUT_DIR}/playstore/app-release.apk" | tee -a "${LOG_FILE}"
echo "   Log File       : ${LOG_FILE}" | tee -a "${LOG_FILE}"
echo "" | tee -a "${LOG_FILE}"
echo "   To upload to Google Play Console:" | tee -a "${LOG_FILE}"
echo "   1. Go to Google Play Console (https://play.google.com/console)." | tee -a "${LOG_FILE}"
echo "   2. Select app package: ${APPLICATION_ID}" | tee -a "${LOG_FILE}"
echo "   3. Create a new release under Testing or Production." | tee -a "${LOG_FILE}"
echo "   4. Upload ${BUILD_OUTPUT_DIR}/playstore/app-release.aab." | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"
