# Handoff — Milkys Sound Booster & EQ (read this to continue)

> **Goal:** debug build `0.1.27 (26090505)` on Redmi `25040RP0AG` + ADVAN `A1013A5320TH000257`, all 6 quick fixes + persistence verified. Next agent: `bash scripts/device_prep.sh <serial> .build-outputs/app-playstore-debug-<sha4>.apk` before any dump/tap.

## Snapshot
- **Tag:** `pre-opencode-v2-ef9c2d5` → HEAD `ef9c2d5` (feat debug bridge + overlay EQ + ⇲)
- **Branch:** `main`, 3 recent fix commits: `f3828ec` FGS crash, `f6bba5a` bubble+eq+debug bridge, `ef9c2d5` command bridge+EQ+X->⇲
- **Version held:** `VERSION_NAME=0.1.27`, `VERSION_CODE=26090505` (SKIP_VERSION_BUMP=1)
- **Build output:** `.build-outputs/app-playstore-debug-c2d5.apk` (last-4 `c2d5` from `ef9c2d5`), pushed to `/sdcard/Download/milkys-debug-c2d5.apk`; header `DEBUG 0.1.27 (26090505) • ef9c2d5`

## Devices
- `192.168.0.129:41161` → `25040RP0AG` (Redmi Pad 2, Android 16 SDK36, 1600x2560 360dpi EXPANDED, Wireless debugging, IP ephemeral)
- `A1013A5320TH000257` (ADVAN TAB A10, Android 14 SDK34, USB 3-1, Q4 smoke target)
- Serial resolve: `adb devices | awk '$2=="device"{print $1}' | head -n1` ; MIUI `INSTALL_FAILED_USER_RESTRICTED` → push + Files tap

## What’s built + verified
1. **FGS crash** fixed: `VolumeBoosterService.startForegroundService()` always foreground, `MainActivity.onCreate` breaker `shouldAutoStartService()` + Toast, 4 tests
2. **POWER bounce** fixed: `persistContext` never cleared + `init` first-only seeding, `BoosterToggleBounceTest` 3 cases, `persistBefore/AfterRestart/AfterUpgrade` screenshots green
3. **File naming:** `scripts/build.sh` now appends last-4 sha for debug APKs (your request) → `app-playstore-debug-<sha4>.apk`
4. **Debug bridge** (`BuildConfig.DEBUG` only): single action `com.milkys.soundbooster.DEBUG` in `ui/DebugCommandReceiver.kt` (manifest `exported=false`). Extras: `booster_on/off/toggle`, `level`, `preset`, `set_eq`, `set_band`, `overlay_show/hide/toggle/power_*`, `dump_state` → `logcat -s DebugCommand:V`. Helper `scripts/debug_cmd.sh` + `scripts/log_tail.sh` (qc/traces)
5. **Q1-A EQ coupling:** `AudioEffectManager.setEqEnabled(true)` rejected when booster OFF, `setBoostEnabled(false)` auto-turns EQ OFF, Row clickable shows toast `eq_requires_power`, Switch `enabled=boosterOn`
6. **Overlay EQ:** `FloatingDashboard` now has `EQ Enabled` Switch + `EQ : preset` row (below Boost Amplification, same coupling/toast)
7. **Overlay tap:** `FloatingBubble` `clickable + pointerInput drag` so tap expands, drag snaps; header `>`→`⇲` (X stays disable) per your note
8. **Quick presets:** `MUTE→+0%` with `+` prefix (`+30%…MAX`) — no locale change

## Gates (must stay green)
- `SKIP_VERSION_BUMP=1 bash scripts/build.sh assembleDebug` → `.build-outputs/`
- `./gradlew testDebugUnitTest` (37 tests) → `qc/reports/tests/`
- `./gradlew lintDebug` 0 errors, `./gradlew verifyRoborazziDebug` (matrix references `app/matrix-*.png` tracked, `app/src/test/screenshots/`)
- `EqPowerCouplingTest` pinned

## Debug control (no screencap needed)
```bash
bash scripts/debug_cmd.sh dump_state
bash scripts/debug_cmd.sh booster_on
bash scripts/debug_cmd.sh set_eq --ez enabled true
bash scripts/debug_cmd.sh preset --es preset Rock
bash scripts/debug_cmd.sh overlay_toggle
adb -s $SERIAL shell logcat -d 2>/dev/null | grep -a "V DebugCommand" | tail
```

## File map
- `app/src/main/java/com/milkys/soundbooster/ui/DebugCommandReceiver.kt` — bridge
- `VolumeBoosterService.kt` — overlay + FloatingDashboard EQ + ⇲
- `AudioEffectManager.kt` — persistContext, initialized guard, eq coupling, DebugLogBridge V
- `MainActivity.kt` — DEBUG header, EQ row clickable + toast
- `app/build.gradle.kts` — GIT_SHA field + version from .env
- `scripts/build.sh` — sha4 suffix, `scripts/debug_cmd.sh`, `scripts/log_tail.sh`
- `qc/artifacts/screenshots/manual/25040RP0AG-persist-*.png` + `overlay-*.png` + `qc/traces/log-*.log` (gitignored)

## Pending / next
- Header `⇲` verified visually (`Booster Overlay` now shows ⇲/X); one more device tap to confirm close vs minimize semantics on `A1013A5320TH000257` (Q4 smoke)
- Opencode v2 migration: `.opencode/package.json` plugin `1.18.27→1.18.32`, binary `~/.opencode/bin/opencode 1.18.32`, git tag preserved, repo-only scope (your Q2-A)
- `AGENTS.md:§8` always-commit already landed (local commits, push on `push`)

## How to resume
```bash
git log --oneline -5; git status --short
bash scripts/check_requirements.sh
bash scripts/device_prep.sh A1013A5320TH000257 .build-outputs/app-playstore-debug-c2d5.apk
bash scripts/debug_cmd.sh dump_state
./gradlew testDebugUnitTest lintDebug verifyRoborazziDebug
```

## Opencode v2 note
- Source `Q1-A npm i -g` → package is `@opencode-ai/plugin` (not `opencode`), binary at `~/.opencode/bin/opencode 1.18.32`, upgrade is `cd .opencode && npm i @opencode-ai/plugin@latest`
- Repo scope only, tag `pre-opencode-v2-ef9c2d5` for rollback, `AGENTS.md` shortcuts `quick/standard/hard-fix` + `Q#` labeling unchanged
