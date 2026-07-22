#!/usr/bin/env bash
# ==============================================================================
# Multi-Platform Build Script (Linux & macOS) with Environment Logging
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

# Function to read variable from .env or fallback
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

BUILD_LOGS_DIR=$(get_env_var "BUILD_LOGS_DIR" "logs")
BUILD_OUTPUT_DIR=$(get_env_var "BUILD_OUTPUT_DIR" ".build-outputs")
VERSION_CODE=$(get_env_var "VERSION_CODE" "3")
VERSION_NAME=$(get_env_var "VERSION_NAME" "3.0")
BUILD_TARGET=$(get_env_var "BUILD_TARGET" "playstore" | tr '[:upper:]' '[:lower:]')

mkdir -p "${BUILD_LOGS_DIR}"
mkdir -p "${BUILD_OUTPUT_DIR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${BUILD_LOGS_DIR}/build_${TIMESTAMP}.log"
LATEST_LOG_FILE="${BUILD_LOGS_DIR}/latest_build.log"

echo "=======================================================" | tee "${LOG_FILE}"
echo "   Milkys Sound Booster & EQ - Build Execution" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"
echo "   Build Timestamp : ${TIMESTAMP}" | tee -a "${LOG_FILE}"
echo "   Build Target    : ${BUILD_TARGET}" | tee -a "${LOG_FILE}"
echo "   Version Code    : ${VERSION_CODE}" | tee -a "${LOG_FILE}"
echo "   Version Name    : ${VERSION_NAME}" | tee -a "${LOG_FILE}"
echo "   Logs Directory  : ${BUILD_LOGS_DIR}" | tee -a "${LOG_FILE}"
echo "   Output Directory: ${BUILD_OUTPUT_DIR}" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"

# 2. Run Requirements Check
echo -e "\n[*] Running Machine Requirements Check..." | tee -a "${LOG_FILE}"
if [ -f "${SCRIPT_DIR}/check_requirements.sh" ]; then
    bash "${SCRIPT_DIR}/check_requirements.sh" 2>&1 | tee -a "${LOG_FILE}"
fi

# 3. Choose Gradle Command
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

# 4. Execute Build according to BUILD_TARGET
BUILD_TASK="${1:-assembleDebug}"

run_gradle_build() {
    local target_name="$1"
    local include_ads="$2"
    echo -e "\n[*] Building for Distribution Target: [${target_name}] (INCLUDE_GOOGLE_ADS=${include_ads})..." | tee -a "${LOG_FILE}"
    
    export BUILD_TARGET="${target_name}"
    export INCLUDE_GOOGLE_ADS="${include_ads}"

    if ${GRADLE_CMD} ${BUILD_TASK} 2>&1 | tee -a "${LOG_FILE}"; then
        echo "    [+] Gradle build for [${target_name}] succeeded!" | tee -a "${LOG_FILE}"
        if [ -d "app/build/outputs/apk/debug" ]; then
            mkdir -p "${BUILD_OUTPUT_DIR}/${target_name}"
            cp app/build/outputs/apk/debug/*.apk "${BUILD_OUTPUT_DIR}/${target_name}/" 2>/dev/null || true
            cp app/build/outputs/apk/debug/app-debug.apk "${BUILD_OUTPUT_DIR}/app-${target_name}-debug.apk" 2>/dev/null || true
        elif [ -d "app/build/outputs/apk" ]; then
            mkdir -p "${BUILD_OUTPUT_DIR}/${target_name}"
            cp -r app/build/outputs/apk/* "${BUILD_OUTPUT_DIR}/${target_name}/" 2>/dev/null || true
            find app/build/outputs/apk -name "*.apk" -exec cp {} "${BUILD_OUTPUT_DIR}/app-${target_name}-debug.apk" \; 2>/dev/null || true
        fi
    else
        echo "[!] Build for [${target_name}] FAILED. Check log file at: ${LOG_FILE}" | tee -a "${LOG_FILE}"
        cp "${LOG_FILE}" "${LATEST_LOG_FILE}"
        exit 1
    fi
}

case "${BUILD_TARGET}" in
    "fdroid")
        run_gradle_build "fdroid" "false"
        ;;
    "both")
        echo -e "\n[*] 'both' target specified: Building Play Store and F-Droid packages sequentially..." | tee -a "${LOG_FILE}"
        run_gradle_build "playstore" "true"
        run_gradle_build "fdroid" "false"
        ;;
    *)
        run_gradle_build "playstore" "true"
        ;;
esac

cp "${LOG_FILE}" "${LATEST_LOG_FILE}"

echo -e "\n=======================================================" | tee -a "${LOG_FILE}"
echo "   BUILD SUCCESSFUL" | tee -a "${LOG_FILE}"
echo "   Build Target: ${BUILD_TARGET}" | tee -a "${LOG_FILE}"
echo "   Log File    : ${LOG_FILE}" | tee -a "${LOG_FILE}"
echo "   Latest Log  : ${LATEST_LOG_FILE}" | tee -a "${LOG_FILE}"
echo "   APK Output  : ${BUILD_OUTPUT_DIR}" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"
