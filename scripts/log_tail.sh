#!/usr/bin/env bash
# Tail DEBUG bounce-hunt logs from the connected device into qc/traces/.
# Usage: bash scripts/log_tail.sh [serial]  (resolves first device if omitted)
# Output: qc/traces/log-bounce-HHMMSS.log (gitignored, ephemeral per qc_plan.md)
set -euo pipefail
SERIAL="${1:-$(adb devices | awk '$2=="device"{print $1}' | head -n 1)}"
if [ -z "$SERIAL" ]; then echo "no device"; exit 1; fi
mkdir -p qc/traces
OUT="qc/traces/log-bounce-$(date +%H%M%S).log"
echo "tailing $SERIAL -> $OUT (Ctrl+C to stop)"
adb -s "$SERIAL" logcat -v time -s AudioEffectManager:V DashboardScreen:V VolumeBoosterService:V | tee "$OUT"
