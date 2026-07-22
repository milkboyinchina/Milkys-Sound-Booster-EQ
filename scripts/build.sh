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

mkdir -p "${BUILD_LOGS_DIR}"
mkdir -p "${BUILD_OUTPUT_DIR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${BUILD_LOGS_DIR}/build_${TIMESTAMP}.log"
LATEST_LOG_FILE="${BUILD_LOGS_DIR}/latest_build.log"

echo "=======================================================" | tee "${LOG_FILE}"
echo "   Milkys Sound Booster & EQ - Build Execution" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"
echo "   Build Timestamp : ${TIMESTAMP}" | tee -a "${LOG_FILE}"
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

# 4. Execute Build
BUILD_TASK="${1:-assembleDebug}"
echo -e "\n[*] Executing Gradle Task: ${GRADLE_CMD} ${BUILD_TASK}..." | tee -a "${LOG_FILE}"

if ${GRADLE_CMD} ${BUILD_TASK} 2>&1 | tee -a "${LOG_FILE}"; then
    echo -e "\n[+] Gradle build succeeded!" | tee -a "${LOG_FILE}"
else
    echo -e "\n[!] Build FAILED. Check log file at: ${LOG_FILE}" | tee -a "${LOG_FILE}"
    cp "${LOG_FILE}" "${LATEST_LOG_FILE}"
    exit 1
fi

# 5. Copy Build Outputs
echo -e "\n[*] Copying built APKs to ${BUILD_OUTPUT_DIR}..." | tee -a "${LOG_FILE}"
if [ -d "app/build/outputs/apk" ]; then
    cp -r app/build/outputs/apk/* "${BUILD_OUTPUT_DIR}/"
    echo "    [+] APKs successfully copied to ${BUILD_OUTPUT_DIR}:" | tee -a "${LOG_FILE}"
    find "${BUILD_OUTPUT_DIR}" -name "*.apk" | while read -r apk; do
        echo "        - ${apk}" | tee -a "${LOG_FILE}"
    done
fi

cp "${LOG_FILE}" "${LATEST_LOG_FILE}"

echo -e "\n=======================================================" | tee -a "${LOG_FILE}"
echo "   BUILD SUCCESSFUL" | tee -a "${LOG_FILE}"
echo "   Log File  : ${LOG_FILE}" | tee -a "${LOG_FILE}"
echo "   Latest Log: ${LATEST_LOG_FILE}" | tee -a "${LOG_FILE}"
echo "   APK Output: ${BUILD_OUTPUT_DIR}" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"
