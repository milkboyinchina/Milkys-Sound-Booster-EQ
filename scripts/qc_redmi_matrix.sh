#!/usr/bin/env bash
# ==============================================================================
# qc_redmi_matrix.sh — Redmi Note 8 Pro Display/Font spot-check (6 combos)
# ==============================================================================
# Toggles wm density (MIUI Display size) + font_scale (Font size) via adb,
# captures screencap per combo to qc/artifacts/screenshots/manual/, then
# restores baseline. Ephemeral — not CI-blocking. Manual via Settings UI is
# primary (catches MIUI quirks); this helper is for regression only.
#
# Usage:
#   bash scripts/qc_redmi_matrix.sh                  # 6-combo spot-check (default)
#   bash scripts/qc_redmi_matrix.sh --all            # full 16 combos (optional)
#   bash scripts/qc_redmi_matrix.sh --restore        # restore baseline only (352/1.0)
#   SERIAL=hm5xr8gueiz5x4c6 bash scripts/qc_redmi_matrix.sh
#
# Requires: adb 37.0.1+, device attached (hm5xr8gueiz5x4c6)
# Output: qc/artifacts/screenshots/manual/redmi-*.png (gitignored)
# See: qc_plan.md §5.7, AGENTS.md §3.2, qc/checklists/smoke.md

set -e

SERIAL="${SERIAL:-hm5xr8gueiz5x4c6}"
MODE="${1:-spot}"
OUT_DIR="qc/artifacts/screenshots/manual"
BASE_DENSITY=352
BASE_FONT=1.0

usage() {
  echo "Usage: $0 [--spot|--all|--restore] (SERIAL env overrides default $SERIAL)"
  exit 0
}

if [[ "$1" == "-h" || "$1" == "--help" ]]; then usage; fi

if ! command -v adb >/dev/null 2>&1; then
  echo "[!] adb not found in PATH"
  exit 1
fi

# Check device reachable
if ! adb -s "$SERIAL" get-state >/dev/null 2>&1; then
  echo "[!] Device $SERIAL not reachable. Available:"
  adb devices -l
  exit 1
fi

mkdir -p "$OUT_DIR"

# Current baseline for restore log
CUR_DENS=$(adb -s "$SERIAL" shell wm density 2>&1 | grep -oE '[0-9]+' | tail -n 1 || echo "$BASE_DENSITY")
CUR_FONT=$(adb -s "$SERIAL" shell settings get system font_scale 2>&1 | tr -d '\r' || echo "$BASE_FONT")
echo "[*] Device $SERIAL — current density: $CUR_DENS, font_scale: $CUR_FONT"
echo "[*] Baseline to restore: density $BASE_DENSITY, font_scale $BASE_FONT"

restore() {
  echo "[*] Restoring baseline density $BASE_DENSITY + font_scale $BASE_FONT..."
  adb -s "$SERIAL" shell wm density "$BASE_DENSITY" >/dev/null 2>&1 || true
  adb -s "$SERIAL" shell settings put system font_scale "$BASE_FONT" >/dev/null 2>&1 || true
  # Optional: clear override if needed (wm density reset would revert to physical 440, but we want 352 baseline)
  sleep 1
  adb -s "$SERIAL" shell wm density >/dev/null 2>&1 || true
  adb -s "$SERIAL" shell settings get system font_scale >/dev/null 2>&1 || true
  echo "[+] Restored."
}

if [[ "$MODE" == "--restore" ]]; then
  restore
  exit 0
fi

# combos: density|font_scale|label
if [[ "$MODE" == "--all" ]]; then
  COMBOS=(
    "440|0.85|small-0.85x"
    "440|1.0|small-1.0x"
    "440|1.3|small-1.3x"
    "440|2.0|small-2.0x"
    "352|0.85|default-0.85x"
    "352|1.0|default-1.0x"
    "352|1.3|default-1.3x"
    "352|2.0|default-2.0x"
    "300|0.85|largest-0.85x"
    "300|1.0|largest-1.0x"
    "300|1.3|largest-1.3x"
    "300|2.0|largest-2.0x"
    "350|1.0|large-1.0x"
    "350|1.3|large-1.3x"
    "320|1.3|mid-1.3x"
    "320|2.0|mid-2.0x"
  )
else
  # 6-combo spot-check per qc_plan.md §5.7
  COMBOS=(
    "352|1.0|default-1.0x"    # #1 baseline
    "440|2.0|small-2.0x"      # #2 worst-case compact + largest font
    "300|0.85|largest-0.85x"  # #3 expanded + small font
    "300|1.3|largest-1.3x"    # #4 tablet + accessibility
    "352|1.3|default-1.3x"    # #5 standard enlargement
    "440|0.85|small-0.85x"    # #6 compact + compact
  )
fi

echo "[*] Running ${#COMBOS[@]} combos → $OUT_DIR/redmi-<label>.png (gitignored)"
echo "[*] Tip: For MIUI-accurate checks, prefer manual Settings → Display size / Font size; this helper uses wm density + settings put."

for combo in "${COMBOS[@]}"; do
  IFS='|' read -r dens font label <<< "$combo"
  echo ""
  echo "--- Combo: $label (density $dens, font $font) ---"
  adb -s "$SERIAL" shell wm density "$dens" >/dev/null 2>&1 || echo "[WARN] wm density $dens failed"
  adb -s "$SERIAL" shell settings put system font_scale "$font" >/dev/null 2>&1 || echo "[WARN] font_scale $font failed"
  sleep 2  # allow UI to settle after density/font change

  # Optional: trigger a UI refresh by sending a dummy wm density query
  adb -s "$SERIAL" shell wm density >/dev/null 2>&1 || true

  out_file="$OUT_DIR/redmi-${label}.png"
  tmp_path="/sdcard/qc-redmi-${label}.png"
  if adb -s "$SERIAL" shell screencap -p "$tmp_path" >/dev/null 2>&1; then
    adb -s "$SERIAL" pull "$tmp_path" "$out_file" >/dev/null 2>&1 || echo "[WARN] pull failed for $label"
    adb -s "$SERIAL" shell rm "$tmp_path" >/dev/null 2>&1 || true
    if [[ -f "$out_file" ]]; then
      echo "[+] Saved $out_file ($(du -h "$out_file" 2>&1 | cut -f1))"
    else
      echo "[WARN] $out_file not created"
    fi
  else
    echo "[WARN] screencap failed for $label"
  fi
done

echo ""
restore
echo ""
echo "[*] Done. Check $OUT_DIR/redmi-*.png (gitignored, ephemeral)."
echo "[*] For video, use: scrcpy -s $SERIAL --no-control --record /tmp/qc-redmi-matrix-\$(date +%H%M%S).mp4"
