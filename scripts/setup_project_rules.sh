#!/usr/bin/env bash
# ==============================================================================
# Universal Project Rule Deployer for Antigravity Model Delegation
# Deploys .gemini/rules/model_delegation.md to any specified project workspace.
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

SOURCE_RULE="${ROOT_DIR}/.gemini/rules/model_delegation.md"
TARGET_DIR="${1:-${ROOT_DIR}}"

if [ ! -f "${SOURCE_RULE}" ]; then
    echo "[!] Error: Source rule file not found at ${SOURCE_RULE}"
    exit 1
fi

DEST_RULES_DIR="$(cd "${TARGET_DIR}" && pwd)/.gemini/rules"
mkdir -p "${DEST_RULES_DIR}"

DEST_FILE="${DEST_RULES_DIR}/model_delegation.md"

if [ "$(readlink -f "${SOURCE_RULE}")" != "$(readlink -f "${DEST_FILE}" 2>/dev/null)" ]; then
    cp "${SOURCE_RULE}" "${DEST_FILE}"
    ACTION_MSG="Deployed Rule   : ${DEST_FILE}"
else
    ACTION_MSG="Rule File Status: ${DEST_FILE} (Up to date)"
fi

echo "======================================================="
echo "   ANTIGRAVITY MODEL DELEGATION RULES DEPLOYED"
echo "======================================================="
echo "   Target Workspace: $(cd "${TARGET_DIR}" && pwd)"
echo "   ${ACTION_MSG}"
echo "======================================================="
echo "   Shortcuts Enabled:"
echo "     /quick <task>    -> @quick-task (flash_lite)"
echo "     /standard <task> -> @standard-dev (flash)"
echo "     /hard-fix <task> -> @complex-architect (pro)"
echo "======================================================="
