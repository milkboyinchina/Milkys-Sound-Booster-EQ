#!/usr/bin/env bash
# DEBUG command helper — single action bridge (Q1-A)
# Usage: bash scripts/debug_cmd.sh booster_on
#        bash scripts/debug_cmd.sh level --ei level 30
#        bash scripts/debug_cmd.sh preset --es preset "Rock"
#        bash scripts/debug_cmd.sh set_eq --ez enabled true
#        bash scripts/debug_cmd.sh dump_state
# Requires debug build (BuildConfig.DEBUG) — no effect in release.
set -euo pipefail
SERIAL="${SERIAL:-$(adb devices | awk '$2=="device"{print $1}' | head -n 1)}"
CMD="${1:-dump_state}"; shift || true
EXTRA=("$@")
if [ -z "$SERIAL" ]; then echo "no device"; exit 1; fi
# Map short names to broadcast extras
case "$CMD" in
  booster_on|booster_off|booster_toggle|overlay_show|overlay_hide|overlay_toggle|overlay_power_on|overlay_power_off|dump_state) ;;
  level) EXTRA=(--ei level "${EXTRA[0]:-20}") ;;
  preset) EXTRA=(--es preset "${EXTRA[0]:-Rock}") ;;
  set_eq) EXTRA=(--ez enabled "${EXTRA[0]:-true}") ;;
  set_band) EXTRA=(--ei band "${EXTRA[0]:-0}" --ei level "${EXTRA[1]:-0}") ;;
esac
adb -s "$SERIAL" shell am broadcast -a com.milkys.soundbooster.DEBUG --es cmd "$CMD" "${EXTRA[@]}" >/dev/null
sleep 0.6
adb -s "$SERIAL" shell logcat -d 2>/dev/null | grep -a "V DebugCommand" | tail -n 4
