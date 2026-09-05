# Smoke Checklist — Manual Device Run

> Copy from `TODO_MANUAL_CHECK.md` §6 and `qc_plan.md` §5.5. Execute on both physical devices after `adb install`.

## Devices
- [ ] Redmi Note 8 Pro `hm5xr8gueiz5x4c6` (Android 16, 1080×2340)
- [ ] ADVAN TAB A10 `A1013A5320TH000257` (Android 14, 1280×800 tablet)

## Steps
0. [ ] **Device prep (Q17/Q18, before any dump/screencap/swipe/click after pm clear/install -r):** `bash scripts/device_prep.sh hm5xr8gueiz5x4c6 .build-outputs/app-playstore-debug.apk` + `bash scripts/device_prep.sh A1013A5320TH000257 .build-outputs/app-playstore-debug.apk` — silent `pm grant POST_NOTIFICATIONS` + `run-as has_seen_onboarding=true` (fallback tap `GET STARTED`) — prevents `Allow`/`GET STARTED` blocking `uiautomator dump`/`screencap` (see `AGENTS.md:§3.2 D`).
1. [ ] Install: `adb -s <serial> install -r .build-outputs/app-playstore-debug.apk`, grant `POST_NOTIFICATIONS`, enable booster → notification `Milkys Sound Booster & EQ Active (+XX%)` with `-10%/+10%/OFF` when `isNotifControlsEnabled`.
2. [ ] Overlay: Toggle `Overlay Control` → grant `SYSTEM_ALERT_WINDOW` → bubble draggable, snap to edge, expand to `Booster Overlay` with 4 favorites.
3. [ ] EQ presets: `Flat`, `Bass Booster`, save custom `MyPreset` (1-10 chars, max 7), favorite max 4, export/import JSON (`qc/fixtures/presets/`).
   - [ ] **EQ 5-band `+/-` always tunable (Q1, Q4):** With booster **OFF and ON**, tap `60Hz` `+`/`-` on **both** `hm5xr8gueiz5x4c6` + `A1013A5320TH000257` — verify `Text +1dB` + `_eqBands` Flow + `equalizer?.setBandLevel` (`Flat`→`Custom` Q4 and `Custom` presets, booster OFF/ON, 16 taps total, 4 fontScales per device if needed). `EQ is always tunable` — `enabled = level <15 / >-15` (not `isBoostEnabled`), see `MainActivity.kt:3088` + `AudioEffectManager.kt:464` (screencap `qc/artifacts/screenshots/manual/eq-*.png`, `screencap` only per Q6).
4. [ ] Warning: `HearingWarningCard` dismiss → hidden 7 days (`hideHearingWarningFor7Days`), reappears after clearing prefs.
5. [ ] Language: Settings → `AppCompatDelegate.setApplicationLocales` + `config.setLocale` — verify 13 locales.
6. [ ] Theme: Toggle light/dark → no hardcoded `Color(0xFF...)` regressions.
7. [ ] Leak: `adb -s <serial> shell dumpsys audio | grep -i enhancer` before/after service stop — 0 leaked handles.

## Redmi Display / Font Spot-Check (Settings — 6 combos, ephemeral, see qc_plan.md §5.7)

> Primary via **Settings UI** (MIUI-accurate) — helper `bash scripts/qc_redmi_matrix.sh` for regression only.

- [ ] #1 Default/Default: Display size **Default** (352dpi) + Font **Default** 1.0x — baseline
- [ ] #2 Small/Largest: Display **Small** (440dpi) + Font **Largest** 2.0x — compact + max font (wrapping, button stacking)
- [ ] #3 Largest/Small: Display **Largest** (~300dpi) + Font **Small** 0.85x — expanded alignment
- [ ] #4 Largest/Large: Display **Largest** (~300dpi) + Font **Large** 1.3x — EQ height 240dp fix
- [ ] #5 Default/Large: Display **Default** (352dpi) + Font **Large** 1.3x — standard enlargement
- [ ] #6 Small/Small: Display **Small** (440dpi) + Font **Small** 0.85x — 48dp touch target min
- [ ] Light/Dark toggle on #2 and #4 (→ 8 PNGs if needed)
- [ ] For each combo: `adb -s hm5xr8gueiz5x4c6 shell screencap -p /sdcard/qc-redmi-<label>.png && adb pull` → `qc/artifacts/screenshots/manual/redmi-<label>.png` (gitignored) + helper: `bash scripts/qc_redmi_matrix.sh`

## Optional — Ephemeral scrcpy (manual only, not CI)
- [ ] Mirror side-by-side for precise drag checks: `scrcpy -s hm5xr8gueiz5x4c6 --window-title "Redmi-API36" &` + `scrcpy -s A1013A5320TH000257 --window-title "ADVAN-API34" &`
- [ ] If bug repro needed, ephemeral record: `scrcpy -s <serial> --no-control --record /tmp/qc-<device>-$(date +%H%M%S).mp4` (gitignored, delete after review; see `qc_plan.md:§5.5`)

## Evidence
- [ ] `adb screencap` saved to `qc/artifacts/screenshots/manual/<device>-<step>.png`
- [ ] Logs / dumpsys saved to `qc/reports/` if needed
- [ ] (If scrcpy used) ephemeral MP4 at `/tmp/qc-*.mp4` or `qc/artifacts/recordings/*.mp4` — **not** tracked, not linked in `qc/changelogs/`
