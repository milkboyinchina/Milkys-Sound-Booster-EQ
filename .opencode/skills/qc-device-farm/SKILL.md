# Skill: qc-device-farm

> Generates device matrix from `adb devices -l` for P3 smoke and Redmi 6-combo matrix (`qc_plan.md:§5.7`).

## Inventory (live)

```bash
adb devices -l            # 2 devices: hm5xr8gueiz5x4c6 (Redmi Note 8 Pro, API36, 1080×2340, 352dpi, 491dp) + A1013A5320TH000257 (ADVAN TAB A10, API34, 1280×800, 601dp)
adb -s <serial> shell wm size; wm density; getprop ro.build.version.release
```

## Helpers

- `scripts/device_prep.sh <serial> [apk]` — Q17 `pm grant POST_NOTIFICATIONS` silent + Q18 `run-as has_seen_onboarding=true` (fallback tap `GET STARTED`) — **must run before any dump/screencap/swipe/click after pm clear/install -r** (prevents `Allow`/`GET STARTED` blocking `uiautomator dump`/`screencap`), see `AGENTS.md:§3.2 D`.
- `scripts/qc_redmi_matrix.sh` — 6-combo spot-check (default-1.0x, small-2.0x, largest-0.85x, largest-1.3x, default-1.3x, small-0.85x) → `qc/artifacts/screenshots/manual/redmi-*.png` (gitignored) + restore `352/1.0`. Manual via Settings UI is primary (MIUI quirks). Call `device_prep.sh` first.
- `scrcpy -s <serial> --window-title "Redmi-API36" &` + `ADVAN-API34` — side-by-side live QA; ephemeral `--record /tmp/qc-*.mp4` or `qc/artifacts/recordings/*.mp4` (gitignored, never CI).

## Matrix table generation

Use `adb` probes + `qc_plan.md:§2` template to emit:

| # | Serial | Model | Android | Size/density | Role |
|---|---|---|---|---|---|

Keep 2 physical + 3 emulators gap-fill (Pixel3a API24 320dp, Pixel8 API30/33, Fold API34 840dp).

## Evidence

- Screenshots: `qc/artifacts/screenshots/manual/<device>-<step>.png` (gitignored, ephemeral)
- Reports: `qc/reports/tests/`, `qc/reports/lint/`, `qc/reports/roborazzi/`
- Summary: `qc/QC_SUMMARY.md` (add `Run` log, update `Bug Status`)
