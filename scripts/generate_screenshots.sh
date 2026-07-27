#!/usr/bin/env bash
# ==============================================================================
# Play Console & App Screenshots Generation Script
# ==============================================================================
# Generates high-resolution screenshots for all app screens using Roborazzi/Robolectric
# and exports them to SCREENSHOT_OUTPUT_DIR defined in .env.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${ROOT_DIR}"

# Load .env or fallback to .env.example
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
    echo "[*] Creating .env from .env.example..."
    cp .env.example .env
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

SCREENSHOT_OUTPUT_DIR=$(get_env_var "SCREENSHOT_OUTPUT_DIR" "screenshots")
BUILD_LOGS_DIR=$(get_env_var "BUILD_LOGS_DIR" "logs")

mkdir -p "${SCREENSHOT_OUTPUT_DIR}"
mkdir -p "${BUILD_LOGS_DIR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${BUILD_LOGS_DIR}/screenshots_${TIMESTAMP}.log"

echo "=======================================================" | tee "${LOG_FILE}"
echo "   Milkys Sound Booster & EQ - Screenshot Generation" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"
echo "   Output Directory : ${SCREENSHOT_OUTPUT_DIR}" | tee -a "${LOG_FILE}"
echo "   Log File         : ${LOG_FILE}" | tee -a "${LOG_FILE}"
echo "=======================================================" | tee -a "${LOG_FILE}"

# Determine Gradle Command
if command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
elif [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    echo "[!] Error: Neither global gradle nor gradlew found." | tee -a "${LOG_FILE}"
    exit 1
fi

echo -e "\n[*] Executing Roborazzi Screenshot Capture Task..." | tee -a "${LOG_FILE}"
export SCREENSHOT_OUTPUT_DIR="${SCREENSHOT_OUTPUT_DIR}"

if ${GRADLE_CMD} :app:recordRoborazziDebug 2>&1 | tee -a "${LOG_FILE}"; then
    echo -e "\n[+] Screenshot generation completed successfully!" | tee -a "${LOG_FILE}"
else
    echo -e "\n[!] Roborazzi recording task failed. Running test suite fallback..." | tee -a "${LOG_FILE}"
    ${GRADLE_CMD} :app:testDebugUnitTest 2>&1 | tee -a "${LOG_FILE}"
fi

# Consolidate screenshots from default Roborazzi folders into SCREENSHOT_OUTPUT_DIR if needed
if [ -d "app/src/test/screenshots" ]; then
    cp app/src/test/screenshots/*.png "${SCREENSHOT_OUTPUT_DIR}/" 2>/dev/null || true
fi
if [ -d "app/build/outputs/roborazzi" ]; then
    cp app/build/outputs/roborazzi/*.png "${SCREENSHOT_OUTPUT_DIR}/" 2>/dev/null || true
fi

# Generate Play Store Presentation Mockup Screenshots
if command -v python3 >/dev/null 2>&1 && [ -f "scripts/generate_playstore_presentation_screenshots.py" ]; then
    echo -e "\n[*] Generating Play Store Presentation Graphics..." | tee -a "${LOG_FILE}"
    python3 scripts/generate_playstore_presentation_screenshots.py 2>&1 | tee -a "${LOG_FILE}"
fi

echo -e "\n=======================================================" | tee -a "${LOG_FILE}"
echo "   SCREENSHOT GENERATION SUCCESSFUL" | tee -a "${LOG_FILE}"
echo "   Output Folder: ${SCREENSHOT_OUTPUT_DIR}" | tee -a "${LOG_FILE}"
echo "   Generated Screenshot Files:" | tee -a "${LOG_FILE}"
find "${SCREENSHOT_OUTPUT_DIR}" -maxdepth 2 -name "*.png" | while read -r img; do
    echo "    - ${img}" | tee -a "${LOG_FILE}"
done
echo "=======================================================" | tee -a "${LOG_FILE}"
