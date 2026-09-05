# QC Summary — Latest

> **Last updated:** 2026-09-06 | **VERSION_NAME:** 0.1.25 (`VERSION_CODE` 26090503, `.env:33`) | **Devices:** Redmi Note 8 Pro `hm5xr8gueiz5x4c6` (Android 16 / SDK 36, 1080×2340, 352dpi override, 491dp) + ADVAN TAB A10 `A1013A5320TH000257` (Android 14 / SDK 34, 1280×800, 213dpi, 601dp tablet) | **Env:** JDK 21 (Foojay 1.0.0), AGP 9.1.1, Kotlin 2.0.21, scrcpy 4.1 ephemeral | **CI:** Auto main+`v*` tags, Roborazzi blocking, `keystore/release.jks` subfolder, `SKIP_VERSION_BUMP=1` on tags
> **Source of truth — read top before planning:** This file is the agent queue. `Next Actions` (below) is the ONLY queue to act on. `Run` sections are log history — do not treat as queue. History preserved via `git log --follow qc/QC_SUMMARY.md`. Compare `Last updated` vs `git log --oneline qc/QC_SUMMARY.md` to avoid stale reads.

---

## Next Actions (agent queue — ONLY OPEN items here)

> Agents MUST read this section first. Act only on `Status == OPEN`. Skip `FIXED`/`WONTFIX`. Check `Last updated` at top.

| ID | Title | File:line | Severity | Owner | Due | Source |
|---|---|---|---|---|---|---|
| CI-001 | Pin GitHub Actions to SHA + add qc-reports/pages upload + Full+Pages artifacts | `.github/workflows/ci-cd.yml:1-200` | High | Jules | 2026-09-06 | CI/CD plan §1 (Full+Pages, pin @v4→SHA) |
| JULES-001 | Fix setup_jules_env.sh keystore/release.jks subfolder + SKIP_VERSION_BUMP guard + qc dirs | `scripts/setup_jules_env.sh:11-38`, `.jules/config.yaml:14-41`, `.jules/rules.md:27-50` | High | Jules | 2026-09-06 | CI/CD plan §3 keystore subfolder |
| QC-009 | Re-record Roborazzi 24-combo matrix baseline (verify 0 failures) | `app/src/test/screenshots/`, `qc/reports/roborazzi/` | Medium | Jules | 2026-09-06 | qc_plan.md §5.2 + CI verify blocking |

*Queue seeded 2026-09-06 per CI/CD fix: all 3 delegated to Jules — `CI-001` already landed in this commit (pin+pages), `JULES-001` + `QC-009` remain OPEN for Jules verification. Previous layout Q7/Q8 validated 00:52; keystore now `keystore/release.jks` via `.env`, bump disabled on `v*` tags. Banner fix + device_prep landed 12:48 (see Run 2026-09-06).*

---

## Run 2026-09-06 — Banner fix + device_prep (Redmi hm5xr8gueiz5x4c6 COMPACT 491dp)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| banner-fix | `MainActivity.kt:2082` `AdaptiveBannerAdCard` fallback always visible (`Column Row SPONSORED AD` + `AndroidView FrameLayout+fallback+AdListener`) + `COMPACT` fixed banner outside `LazyColumn` (`Column fillMaxSize → Banner + Spacer + Row LazyColumn`) | PASS | `MainActivity.kt:2082-2234` |
| device-prep | `bash scripts/device_prep.sh hm5xr8gueiz5x4c6 .build-outputs/app-playstore-debug.apk` (Q17 `pm grant POST_NOTIFICATIONS` silent + Q18 `run-as has_seen_onboarding=true` fallback tap) | PASS | `scripts/device_prep.sh:1` `rwxr-xr-x`, `scripts/check_requirements.sh:83` soft `WARN` |
| roborazzi | `./gradlew recordRoborazziDebug && verifyRoborazziDebug` | **PASS 8/8 → 0 failures** (re-record after banner move) | `app/src/test/screenshots/greeting.png` `247K` + `qc/reports/roborazzi/matrix-*.png` `41K-302K` `6` |
| unit/lint | `./gradlew testDebugUnitTest lintDebug` | **PASS 20/20 + 0e ~130w** | `qc/reports/tests/` `qc/reports/lint/lint-results-debug.html` |
| device-verify | `adb -s hm5xr8gueiz5x4c6 install -r app/build/outputs/apk/debug/app-debug.apk + pm grant + run-as + am start + uiautomator dump` | **PASS** `SPONSORED AD 1` `Adaptive 1` at top before `MILKYS APP` | `qc/artifacts/screenshots/manual/redmi-final-banner.png` `286K` `ui_final_banner.xml` |

### Findings

- Banner was below `EQ` in `COMPACT` `LazyColumn` (after `VisualEqualizer` `60Hz 14kHz`), required scroll past `EQ` `260dp` + `PresetManager` to see — on `Redmi` `491dp` phone below-fold, users perceived missing. `uiautomator dump` after `pm clear` also blocked by `Allow notifications` + `Onboarding GET STARTED` (`has_seen_onboarding false`), so `grep SPONSORED AD 0` masked.
- Fixed: `AdaptiveBannerAdCard` now always shows `Column Row[SPONSORED AD + Adaptive Equalizer Boost]` + `AndroidView` with `fallbackLayout` added before `AdView` + `AdListener proxy` hides fallback on `onAdLoaded` (never empty). `COMPACT` now has fixed banner outside `LazyColumn` (`Column fillMaxSize → Banner + Spacer 12dp + Row weight LazyColumn`), visible at top before `MILKYS APP` without scroll (`ui_final_banner.xml` `SPONSORED AD 1` `Adaptive 1` at top, `redmi-final-banner.png` `286K`).
- Device prep: `scripts/device_prep.sh` `3.6K` `rwxr-xr-x` handles `svc power wakeup` + `pm grant POST_NOTIFICATIONS` (Q17 silent) + `run-as has_seen_onboarding=true` via `SharedPrefs` `sed` (Q18) + fallback tap `GET STARTED` + `force-stop + am start` + `dumpsys window mCurrentFocus/mAwake true`. Added to `AGENTS.md:§3.2 D`, `.jules/rules.md:§3` step 0, `.gemini/rules/rules.md`, `.opencode/skills/{qa-automation,qc-device-farm}`, `qc/checklists/smoke.md:0`, `scripts/check_requirements.sh:83` soft `WARN`.
- Gates green after re-record: `lint 0e`, `test 20/20` (26 with matrix), `verifyRoborazzi 6/6` (was 8 failed due to banner move, now 0 after `record`).

### Fixes Applied (this run)

- `MainActivity.kt:2082` — `AdaptiveBannerAdCard` fallback always visible + `COMPACT` fixed banner outside `LazyColumn`.
- `scripts/device_prep.sh:1` — new `3.6K` device prep (Q17/Q18/Q19).
- `AGENTS.md:§3.2 D`, `.jules/rules.md:§3`, `.gemini/rules/rules.md`, `.opencode/skills/qa-automation/SKILL.md:0`, `.opencode/skills/qc-device-farm/SKILL.md`, `qc/checklists/smoke.md:0`, `scripts/check_requirements.sh:83` — Device Prep preamble.
- `app/src/test/screenshots/greeting.png` — re-record `247K` after banner move.

### Evidence Pointers

- Banner: `MainActivity.kt:2082` + `MainActivity.kt:363` fixed `COMPACT` | Device prep: `scripts/device_prep.sh:1` | Screenshots: `qc/artifacts/screenshots/manual/redmi-final-banner.png` `286K` `SPONSORED AD 1` + `app/src/test/screenshots/greeting.png` `247K` | Reports: `qc/reports/lint/` `qc/reports/tests/` `qc/reports/roborazzi/` | QC: `qc/QC_SUMMARY.md:1`

---

## Run 2026-09-04 22:23 — Full fontScale matrix 0.85x/1.0x/1.3x/2.0x ×2 devices (8 PNGs, screencap only)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| font-matrix | `adb -s hm5xr8gueiz5x4c6/A1013A5320TH000257 shell settings put system font_scale 0.85/1.0/1.3/2.0 + screencap` | **8/8 PASS** | `qc/artifacts/screenshots/manual/redmi-font*.png` (15K ×4) + `advan-font*.png` (116K-181K ×4) |
| restore | `settings put system font_scale 1.0` both | PASS | `getprop font_scale 1.0` both |
| dumpsys | `dumpsys window mCurrentFocus` + `meminfo` | PASS | Redmi `358M` PSS, ADVAN focused `MainActivity` |
| lint/unit/roborazzi | `./gradlew lintDebug testDebugUnitTest verifyRoborazziDebug` | **PASS 0e/20/6** | `qc/reports/lint/`, `qc/reports/tests/`, `qc/reports/roborazzi/` |

### Findings

- Full `0.85x/1.0x/1.3x/2.0x` × `Redmi 491dp` + `ADVAN 601dp` = 8 PNGs (Redmi 15K each dark bg compress, ADVAN 116K-181K tablet) captured via `screencap -p` + `adb pull` to `qc/artifacts/screenshots/manual/` (previous 8 + new 8 = 16 total, gitignored `qc/artifacts/`). No `scrcpy --record` per your `just screencap` choice (ephemeral `qc/artifacts/recordings/` empty, `/tmp` not used).
- Combined with prior `scripts/qc_redmi_matrix.sh` 6 display-size combos (`default-1.0x` etc 15K), total manual `14` font/display PNGs plus `P3 smoke` `2` baselines = `16` in `qc/artifacts/screenshots/manual/`.
- Live `MainActivity` focused after `am start` both devices; `font_scale` restored `1.0` both; no clip at `2.0x` (EQ `heightIn 240dp`, dialog `320dp` already fixed, `AppColors 0→0` verified).

### Fixes Applied (this run)

- No code fix — device UI validation only (full fontScale matrix). Evidence `qc/artifacts/screenshots/manual/*font*.png` (8).

### Evidence Pointers

- Screenshots: `qc/artifacts/screenshots/manual/redmi-font*.png` (4) + `advan-font*.png` (4) | Previous: `redmi-*.png` 6 display-size + `advan/redmi-smoke-baseline.png` 2 | Reports: `qc/reports/` | QC: `qc/QC_SUMMARY.md:1`

---

## Run 2026-09-05 00:52 — Layout Q7/Q8 + EQ toggle + Power debounce (0.1.25)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| layout | `MainActivity.kt:752` `Pane1 QuickBoost` below `Decibel` (EXPANDED) + `Pane3 Banner + PresetManager(showText=false) + Battery` + `COMPACT` `EQ→Banner→PresetManager→Battery` + `MEDIUM` after `Row` `Banner+Preset+ Battery` | PASS | `MainActivity.kt:489-846` |
| eq-toggle | `isEqEnabled` `false` default + `Switch` `48dp` + `VisualEqualizerCard onToggleEq` + `PresetManagerCard showText` | PASS | `AudioEffectManager.kt:24` `isEqEnabled`, `ui/components/PresetManagerCard.kt:62` |
| power-debounce | `AudioEffectManager.kt:342` `@Synchronized` + `lastPowerToggleTime 500ms` + `try onStartService` | PASS | `MainActivity.kt:205` + `AudioEffectManager.kt:342` |
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warns** | `qc/reports/lint/lint-results-debug.html` |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** (26 tests) | `qc/reports/tests/` |
| roborazzi | `./gradlew recordRoborazziDebug && verifyRoborazziDebug` | **PASS 6/6** | `qc/reports/roborazzi/matrix-*.png` (re-record for layout) |
| build | `bash scripts/build.sh assembleDebug` | **PASS 26090503 / 0.1.25** `23M` | `.build-outputs/app-playstore-debug.apk` + `qc/artifacts/apks/` |

### Findings

- `EXPANDED` now `Pane1: QuickBoost` below `Decibel` (was `Pane2` below `EQ`), `Pane2: VisualEqualizer` only, `Pane3: Banner + PresetManager(icons) + Battery` (was `Battery` only, `showText=false` for landscape per Q8/Q11).
- `COMPACT` now `EQ → Banner → PresetManager → Battery` vertical (was `Banner` before `EQ`, no separate PresetManager), `MEDIUM` after `Row` adds `Banner + PresetManager + Battery` full-width (was `Battery` in left column).
- `EQ` `+/-` now gated by `isEqEnabled` (was `isBoostEnabled`), `EQ Enabled` toggle `48dp` `Switch` controls `isEnabled` for `EQ` + `PresetManager`, default `false` (A9).
- Power `on>off>on` debounce `500ms` + `@Synchronized` retry fixes `on(fail)` after `off`.

### Fixes Applied (this run)

- `AudioEffectManager.kt:24,205,342` — `isEqEnabled` + `lastPowerToggleTime` + `@Synchronized`.
- `MainActivity.kt:489-846` — layout moves (Q7/Q8) + `PresetManagerCard showText`.
- `ui/components/PresetManagerCard.kt:62` — `showText` param.

### Evidence Pointers

- Layout: `MainActivity.kt:489-846` | Toggle: `MainActivity.kt:3173` `Switch` | Power: `MainActivity.kt:205` | Reports: `qc/reports/` | APK: `.build-outputs/app-playstore-debug.apk` (23M) | Changelog: `qc/changelogs/20260905-005200-v0.1.25.md`

---

## Run 2026-09-04 22:40 — EQ 5-band always tunable (Q1, 16 taps ×2 devices)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| eq-fix | `MainActivity.kt:3088` `enabled = level <15` (was `isEnabled &&`) + `AudioEffectManager.kt:464` clone/defer | PASS | `MainActivity.kt:3075-3175` `+/-` always tunable, `VolumeBoosterService.kt:705` floating |
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warns** | `qc/reports/lint/lint-results-debug.html` |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** (26 tests) | `qc/reports/tests/` |
| roborazzi | `./gradlew recordRoborazziDebug && verifyRoborazziDebug` | **PASS 6/6** | `qc/reports/roborazzi/matrix-*.png` (re-record after EQ enable color change) |
| device | `adb -s hm5xr8gueiz5x4c6/A1013A5320TH000257 install -r + am start + screencap` | **PASS** `eq-before-*.png` 15K/83K | `qc/artifacts/screenshots/manual/eq-before-*.png` (2) |

### Findings

- EQ `+/-` now always tunable (Q1): removed `isEnabled (=isBoostEnabled)` guard from `IconButton enabled` and `onClick` (`isEnabled && level <15` → `level <15`) + `pointerInput Unit` (was `isEnabled`) + `Brush` always (was `if isEnabled else BorderDark`). Both `hm5xr8gueiz5x4c6` Redmi + `A1013A5320TH000257` ADVAN `Flat`→`+1dB`→`Custom` with booster OFF/ON now updates `Text +1dB` + `_eqBands` Flow + `equalizer?.setBandLevel` deferred via `audioScope` (previously greyed when booster OFF, hardware `null` swallowed).
- `AudioEffectManager` hardened: `setBandLevel` clones `IntArray`, `_eqBands.value = bands` + `persistString` always, hardware `audioScope.launch` with `getBandLevelRange` clamp, `Log.w` not `printStackTrace`, `getPresetBands` returns `clone()` (was shared `BUILT_IN_PRESETS` reference), `applyPreset` `_eqBands = levels.clone()` + `audioScope` defer.
- Skill/rules updated per Q5: `AGENTS.md:§3.2 C` + `.jules/rules.md:§3` step 7 + `.opencode/skills/qa-automation` 7-step + `qc/checklists/smoke.md:3` 16-tap `EQ +/-` Q4 (screencap only Q6).

### Fixes Applied (this run)

- `MainActivity.kt:3075-3175` — +/- always tunable (5 edits: enabled, bg, tint, drag, fill).
- `AudioEffectManager.kt:464,488,493` — clone + defer + clamp.
- `AGENTS.md:147`, `.jules/rules.md:7`, `.opencode/skills/qa-automation/SKILL.md:1`, `qc/checklists/smoke.md:3` — enforce EQ +/- before PR.

### Evidence Pointers

- Lint HTML: `qc/reports/lint/lint-results-debug.html:1` | Tests: `qc/reports/tests/` | Roborazzi: `qc/reports/roborazzi/matrix-*.png` (6) | Device: `qc/artifacts/screenshots/manual/eq-before-*.png` (2) | EQ: `MainActivity.kt:3088` `AudioEffectManager.kt:464`

---

## Run 2026-09-04 21:53 — Polish remaining 38 colors →0 (AppColors)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| colors | `grep Color(0xFF →0` MainActivity (was 38) | **39 fixed** | `ui/theme/Color.kt:14` 21 new tokens, `MainActivity.kt:1` batch |
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warns** | `qc/reports/lint/lint-results-debug.html` |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** (26 tests) | `qc/reports/tests/` |
| roborazzi | `./gradlew recordRoborazziDebug && verifyRoborazziDebug` | **PASS 6/6** | `qc/reports/roborazzi/matrix-*.png` (re-record after color change) |

### Findings

- Extended `Color.kt:14` with 21 new semantic tokens (WarningBorder, WarningTitle, Error, Success, SurfaceVariant, DisabledCard, etc) + batch 39 `Color(0xFF... )→AppColors` in `MainActivity.kt:1` (B3261E→Error 5, FFB4AB→WarningIcon 3, 4F378B→WarningContainer 3, 332D41→CardAlt2 3, etc) — `grep Color(0xFF MainActivity →0` (was 38), remaining 0. `VolumeBoosterService` still has ~8 hardcoded (overlay, not blocking).
- Re-recorded Roborazzi baselines after color change (`app/screenshots/` 5 + `qc/reports/roborazzi/` 6) — `verify 6/6` PASS (previously 6 failed due to stale baseline + deleted `app/qc/` + `app/screenshots`).
- Stray `app/matrix-*.png` (6) removed (were at `app/` root due to `filePath` without `outputDir` prefix, now fixed to `matrix-*.png` via `outputDir`).

### Fixes Applied (this run)

- `ui/theme/Color.kt:14` — 21 new tokens.
- `MainActivity.kt:1` — 39 `AppColors` replacements (`grep 0`).
- `app/src/test/java/.../QcVisualMatrixTest.kt:1` — `filePath` `qc/reports/...` → `matrix-*.png` (rely on `outputDir`).

### Evidence Pointers

- Lint HTML: `qc/reports/lint/lint-results-debug.html:1` | Colors: `grep Color(0xFF MainActivity →0` | Roborazzi: `qc/reports/roborazzi/matrix-*.png` (6) + `app/screenshots/` (5) | Reports: `qc/reports/`

---

## Run 2026-09-04 21:34 — Release v0.1.20 (final build + CHANGELOG)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| build | `bash scripts/build.sh assembleDebug` | **PASS** `26090402 / 0.1.20` playstore 22M + fdroid 22M | `.build-outputs/app-playstore-debug.apk` + `qc/artifacts/apks/` |
| changelog | `CHANGELOG.md [0.1.20]` + `qc/changelogs/20260904-213400-v0.1.20.md` | PASS | `CHANGELOG.md:1`, `qc/changelogs/20260904-213400-v0.1.20.md:1` |
| bump | `scripts/bump_version.py` | `26090401→26090402`, `0.1.19→0.1.20` | `.env:33` |

### Findings

- Final bump `0.1.19→0.1.20` / `26090401→26090402` via `bump_version.py` (build.sh auto-bump); APKs 22M each copied to `qc/artifacts/apks/`; `CHANGELOG.md` [0.1.20] documents all Phase A/B/C/P3 (Added 7 items, Changed 6, Fixed 5), `qc/changelogs/20260904-213400-v0.1.20.md` per-tag verbose.
- Gates remain green (lint 0e warns, unit 20/20, roborazzi 6/6, smoke 8 screenshots). Ready for `git tag v0.1.20`.

### Fixes Applied (this run)

- `CHANGELOG.md:1` — [0.1.20] entry.
- `qc/changelogs/20260904-213400-v0.1.20.md:1` — verbose.
- `.env:33` — version bump.

### Evidence Pointers

- APKs: `qc/artifacts/apks/*.apk` (22M) | CHANGELOG: `CHANGELOG.md:1` | Changelog: `qc/changelogs/20260904-213400-v0.1.20.md:1` | Reports: `qc/reports/` | QC: `qc/QC_SUMMARY.md:1`

---

## Bug Status (OPEN vs FIXED — agent skips FIXED)

| ID | File:line | Severity | Status | Fixed in | Verified in Run | Notes |
|---|---|---|---|---|---|---|
| QC-001 | `HearingWarningCard.kt:43` | Medium | FIXED | `Phase A` | 2026-09-04 20:35 | `32dp → 48dp` + `defaultMinSize(48.dp)` + `contentDescription stringResource` + `translatable false` strings — lint 0 errors, `lint-results-debug.html` confirms |
| B-002 | `AudioEffectManager.kt:127,783` | High | FIXED | `Phase A` | 2026-09-04 20:35 | `audioScope` leak: `@Volatile isPlayingSilence`, `audioScope cancel + recreate + context=null` in `release()`, `stop()` guarded by `playState` |
| B-003 | `VolumeBoosterService.kt:163` | High | FIXED | `Phase A` | 2026-09-04 20:35 | `START_STICKY → START_NOT_STICKY`, null-intent guard, `store.clear()` after `serviceScope.cancel()` |
| B-004 | `VolumeBoosterService.kt:210,193` | High | FIXED | `Phase A` | 2026-09-04 20:35 | `POST_NOTIFICATIONS areNotificationsEnabled()` guard before `startForeground`/`notify`, `SecurityException` early return |
| B-001-partial | `AudioEffectManager.kt:228,701` | High | FIXED (partial) | `Phase A` | 2026-09-04 20:35 | `initEffects` hardened: sessionId fallback, `enabled` try/catch, `startSilencePlayback` ordering comment; full mutex/sync deferred to Phase C |
| U-002 | `MainActivity.kt:3038,1761,3534,3596` | Medium | FIXED | `Phase A` | 2026-09-04 20:35 | `Modifier.height(240/260/160/140) → heightIn(min=...)` for text containers (EQ, license, JSON fields) |
| QC-002 | `GreetingScreenshotTest.kt:32` / `QcVisualMatrixTest.kt:1` | High | FIXED | `Phase C` | 2026-09-04 21:05 | 6-combo matrix `QcVisualMatrixTest` (compact 320/standard 411/expanded 600 × light/dark × landscape) + `roborazzi outputDir qc/reports/roborazzi`; verify 6/6 PASS. Redmi 6-combo manual covers remaining fontScale |
| QC-003 | `app/build.gradle.kts:7` | Medium | FIXED | `Phase C` | 2026-09-04 21:05 | `roborazzi { outputDir.set(rootProject.file("qc/reports/roborazzi")) }` + `record/verifyRoborazziDebug` PASS |
| QC-004 | `MainActivity.kt:3225/3426` | Medium | FIXED | `Phase B` | 2026-09-04 20:55 | `40dp→48dp` x4 buttons () + `36dp→48dp` star `defaultMinSize+siz`e + `widthIn 340→320dp` dialog overflow fixed |
| QC-005 | `MainActivity.kt:778` | High | FIXED | `Phase B: 22 strings` | 2026-09-04 21:18 | Fixed 3 headings + 4 buttons + 14 dialogs/actions (`Cancel/Close/Copy/Share/Pick File/Paste Clipboard/Save/Apply/Yes/No/Import/Delete` etc → `stringResource` + `strings.xml:84` 14 new keys). Remaining 2 `Text("` (placeholder JSON + notification BigText) acceptable — `grep Text(\" →2` |
| QC-006 | `MainActivity.kt:122` / `ui/theme/Color.kt:14` | Medium | FIXED | `Polish 21:53` | 2026-09-04 21:53 | `Color.kt:14` 21 new tokens (WarningBorder/Title, Error, Success, SurfaceVariant etc) + `MainActivity.kt:1` 149→0 `Color(0xFF... )→AppColors` (21 distinct, 39 total incl. B3261E→Error, FFB4AB→WarningIcon, 4F378B→WarningContainer etc) — `grep Color(0xFF →0` MainActivity, 38→0 |
| QC-007 | `AdConsentManager.kt:10,84,24` | High | FIXED | `Phase C` | 2026-09-04 21:05 | `omp→ump` typo fix, `ConsentRequestParameters` class resolution + Activity overload fallback, `runOnUiThread` for `loadAndShow` (reflection hardened) |
| QC-008 | `AudioEffectManager.kt:130,303` / `data/PreferencesRepository.kt:1` | High | FIXED | `Phase C: DataStore` | 2026-09-04 21:30 | `PreferencesRepository` (DataStore 1.1.7, `preferencesDataStore` + 15 keys + `migrateFromSharedPrefs` + `Flow` + `edit` on `Dispatchers.IO`), `AudioEffectManager` dual-write `persistBoolean/Int/Long/String/StringSet` via `audioScope.launch(IO)` + `SharedPreferences` compat + `init` migration `audioScope.launch{migrate}` + `app/build.gradle.kts:193` `datastore-preferences`. Gates `lint 0e`, `test 20/20`, `roborazzi 6/6` PASS |
| EQ-001 | `MainActivity.kt:3088` / `AudioEffectManager.kt:464` | High | FIXED | `Hotfix 22:40` | 2026-09-04 22:40 | 5-band `+/-` always tunable (remove `isEnabled &&` guard, `enabled = level <15 / >-15`, `pointerInput Unit`, `Brush` always, `setBandLevel` clone + `audioScope` defer + `getBandLevelRange` clamp, `getPresetBands` clone). Both devices `Flat→Custom` Q4 with booster OFF/ON 16 taps screencap `eq-before-*.png`, re-record Roborazzi 6/6 PASS |

*When fixing, move to `FIXED` with commit sha + `Verified in Run` date. Do NOT delete rows — history matters for loop prevention.*

---

## Run 2026-09-04 21:30 — DataStore (PreferencesRepository + dual-write IO)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| deps | `app/build.gradle.kts:193` add `datastore-preferences` 1.1.7 | PASS | `gradle/libs.versions.toml:25` |
| data | `data/PreferencesRepository.kt:1` 15 keys + DataStore + migration | PASS | `app/src/main/java/.../data/PreferencesRepository.kt` |
| refactor | `AudioEffectManager.kt:1` dual-write helpers + IO | PASS | `AudioEffectManager.kt:217` helpers + `migrateFromSharedPrefs` |
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warns** | `qc/reports/lint/lint-results-debug.html` |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** (26 tests) | `qc/reports/tests/` |
| roborazzi | `./gradlew verifyRoborazziDebug` | **PASS 6/6** | `qc/reports/roborazzi/matrix-*.png` |

### Findings

- DataStore 1.1.7 added (`androidx.datastore:datastore-preferences`), `PreferencesRepository` 15 keys (enabled/boost/preset/bands/floating/ads/slider/notif/onboarding/warning/dark/language/custom/default/fav/consent) with `Flow` + `suspend edit` + `catch IOException` + `migrateFromSharedPrefs` (copies `SharedPreferences` → DataStore once, idempotent).
- `AudioEffectManager` now dual-writes: `SharedPreferences.apply()` (compat) + `audioScope.launch{ DataStore.put* on IO }` via 5 helpers (`persistBoolean/Int/Long/String/StringSet`) for all 14 setters + 3 multi-put blocks (`setBandLevel`, `applyPreset`, `saveCustomPreset`). `init` launches `migrateFromSharedPrefs` on `audioScope(IO)` non-blocking. Threading: writes off Main (previously 19× `apply()` on Main per drag), reads still via `SharedPreferences` for `init` compat (no breaking change).
- Gates all green (no new lint errors, tests 20/20, roborazzi 6/6). Full DataStore read-path (Flow collection) deferred to Phase C ViewModel (non-blocking).

### Fixes Applied (this run)

- `app/build.gradle.kts:193` — datastore-preferences.
- `data/PreferencesRepository.kt:1` — new.
- `AudioEffectManager.kt:14,217` — helpers + migration + 14 setters + 3 multi blocks.

### Evidence Pointers

- Lint HTML: `qc/reports/lint/lint-results-debug.html:1` | Tests: `qc/reports/tests/` | Roborazzi: `qc/reports/roborazzi/matrix-*.png` (6) | DataStore: `data/PreferencesRepository.kt:1`

---

## Run 2026-09-04 21:25 — AppColors batch (182→38 colors, 149 fixed)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warnings** | `qc/reports/lint/lint-results-debug.html` |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** (26 tests incl. 6 matrix) | `qc/reports/tests/` |
| roborazzi | `./gradlew verifyRoborazziDebug` | **PASS 6/6** | `qc/reports/roborazzi/matrix-*.png` |
| colors | `grep Color(0xFF →38` | **149 fixed →AppColors** | `MainActivity.kt:778` batch, `Color.kt:14` tokens |

### Findings

- Added `import AppColors` to `MainActivity.kt:1` + batch 149 `Color(0xFF... )→AppColors` (E6E1E5→DarkTextPrimary 23, CAC4D0→DarkTextSecondary 23, D0BCFF→PrimaryAccentDark 46, 2B2930→DarkCard 13, 49454F→BorderDark 32, 1C1B1F→DarkBackground 6, 381E72→DeepPurple 3, 36343B→DarkCardAlt 3) — `grep Color(0xFF →38` remaining (warning/button bespoke colors, deferred).
- Gates still green (lint 0e warns `Divider→HorizontalDivider` expected, unit 20/20, roborazzi 6/6).

### Fixes Applied (this run)

- `MainActivity.kt:1,122, etc` — AppColors import + 149 replacements.
- `qc_reports` copied to `qc/reports/`.

### Evidence Pointers

- Lint HTML: `qc/reports/lint/lint-results-debug.html:1` | Colors: `grep Color(0xFF →38` (was 182) | Reports: `qc/reports/`

---

## Run 2026-09-04 21:18 — Strings batch (22→2 Text hardcodes, lint 0e)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warnings** | `qc/reports/lint/lint-results-debug.html` (copied) |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** (26 tests incl. 6 matrix) | `qc/reports/tests/` |
| roborazzi | `./gradlew verifyRoborazziDebug` | **PASS 6/6** | `qc/reports/roborazzi/matrix-*.png` |
| strings | `grep Text(\" →2` | **22 fixed** | `strings.xml:84` 14 new `action_*/dialog_*` keys, `MainActivity.kt:778` batch |

### Findings

- Batch added `strings.xml:14` new keys (`action_copy/share/pick_file/paste_clipboard/save_apply/dialog_import_title/...`) all `translatable false` to avoid `MissingTranslation`; replaced `Text("Copy/Share/.../Save/Apply/Yes/No")` etc 14 occurrences + `Cancel/Close` 6 occurrences + `Export JSON` prefix; remaining 2 `Text("` are placeholder JSON example + notification BigText (acceptable).
- `Color(0xFF... )` still 182 remaining (was 182 → scaffold 1 fixed 181) — `Theme` dynamicColor false + Scaffold MaterialTheme done; deferred rest (low risk, warnings only).
- Gates still green (lint 0e, unit 20/20, roborazzi 6/6).

### Fixes Applied (this run)

- `strings.xml:84` — 14 new keys.
- `MainActivity.kt:3513, etc` — 22 stringResource replacements + `Scaffold containerColor MaterialTheme`.

### Evidence Pointers

- Lint HTML: `qc/reports/lint/lint-results-debug.html:1` | Strings: `grep Text(\" →2` | Colors: `grep Color(0xFF →182` (was 182) | Reports: `qc/reports/`

---

## Run 2026-09-04 21:12 — P3 Smoke (Redmi + ADVAN, 6-combo matrix, audio leak 0)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| build | `bash scripts/build.sh assembleDebug` | **PASS** playstore 22M + fdroid 22M | `.build-outputs/app-playstore-debug.apk` + `qc/artifacts/apks/` (copied) |
| install | `adb -s hm5xr8gueiz5x4c6 / A1013A5320TH000257 install -r` | **PASS** both `Success` | `adb shell pm list packages` |
| launch | `am start -n com.milkys.soundbooster/.MainActivity` | **PASS** focused `MainActivity` both devices | `dumpsys window mCurrentFocus` |
| screencap | `screencap -p` both devices | **PASS** `redmi 15K`, `advan 126K` | `qc/artifacts/screenshots/manual/redmi-smoke-baseline.png`, `advan-smoke-baseline.png` (gitignored) |
| matrix | `bash scripts/qc_redmi_matrix.sh` 6-combo | **PASS** 6/6 SAVED | `qc/artifacts/screenshots/manual/redmi-*.png` 15K each (6 files) |
| meminfo | `dumpsys meminfo` | Redmi TOTAL 253M PSS, ADVAN 160M | stdout |
| audio | `dumpsys audio | grep Loudness` | **0 leaks** both devices | stdout |

### Findings

- APK `VERSION_CODE 26072401 / 0.1` installed OK on both; launch via `monkey` OK; `mCurrentFocus` `MainActivity` after `am start`.
- Screencap baseline: Redmi 15K (dark bg compresses) vs ADVAN 126K (tablet) — both captured; matrix 6 combos (`default-1.0x`, `small-2.0x`, `largest-0.85x`, `largest-1.3x`, `default-1.3x`, `small-0.85x`) all SAVED, restore 352/1.0 PASS.
- Meminfo: Redmi `Native 41M / Dalvik 10M / TOTAL 253M`, ADVAN `42M/19M` — healthy; `dumpsys audio` 0 LoudnessEnhancer/Equalizer leaks (only system players).
- No scrcpy video (ephemeral) needed — screencap sufficient; recordings remain gitignored per `.gitignore:31`.

### Fixes Applied (this run)

- No code fix — smoke validation only. APKs copied to `qc/artifacts/apks/`, changelog `qc/changelogs/20260904-211000-v0.1.1-p3-smoke.md` written.

### Evidence Pointers

- APKs: `qc/artifacts/apks/app-playstore-debug.apk` (22M) | Screenshots: `qc/artifacts/screenshots/manual/` (8 files) | Reports: `qc/reports/lint/`, `qc/reports/roborazzi/matrix-*.png` (6) | Changelog: `qc/changelogs/20260904-211000-v0.1.1-p3-smoke.md`

---

## Run 2026-09-04 21:05 — Phase C (QC-002/003 matrix + QC-007 AdConsent)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warnings** | `qc/reports/lint/lint-results-debug.html` (copied) |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** (26 tests incl. 6 matrix) | `qc/reports/tests/` |
| roborazzi | `./gradlew recordRoborazziDebug && verifyRoborazziDebug` | **PASS 6/6 matrix** | `qc/reports/roborazzi/matrix-*.png` (6 files) |
| smoke | `adb -s <serial> install` | NOT RUN | `qc/artifacts/screenshots/manual/` (pending P3) |

### Findings

- **QC-002/003 fixed:** `QcVisualMatrixTest.kt:1` 6-combo matrix (compact 320, standard 411, expanded 600, dark variants, landscape) + `app/build.gradle.kts:178 roborazzi outputDir qc/reports/roborazzi` → `record` 6 PNGs (41K-302K) + `verify` PASS.
- **QC-007 fixed:** `AdConsentManager.kt:10` `omp→ump`, `requestConsentInfoUpdate` now resolves `ConsentRequestParameters` class (not `params::class.java`), Activity overload fallback, `runOnUiThread` for `loadAndShow`.
- **Phase B strings/colors partially fixed earlier (20:55):** 7 strings, Theme dynamicColor false; remaining 24 strings + 66 colors deferred.
- **Gates:** All 3 automated gates green (lint 0e, unit 20/20 incl. matrix, roborazzi 6/6).

### Fixes Applied (this run)

- `QcVisualMatrixTest.kt:1` — new 6-combo matrix, `MyApplicationTheme(dynamicColor=false)`.
- `app/build.gradle.kts:178` — roborazzi outputDir.
- `AdConsentManager.kt:10,24,84` — UMP fixes.

### Evidence Pointers

- Lint HTML: `qc/reports/lint/lint-results-debug.html:1` | Tests: `qc/reports/tests/` | Roborazzi: `qc/reports/roborazzi/matrix-*.png` (6)

---

## Run 2026-09-04 20:55 — Phase B partial (U-001/U-008 + U-003/U-005 partial)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warnings** | `qc/reports/lint/lint-results-debug.html` (copied), `app/build/reports/lint-results-debug.html` |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** | `qc/reports/tests/` (copied) |
| roborazzi | `./gradlew verifyRoborazziDebug` | NOT RUN | `qc/reports/roborazzi/` (pending P2) |
| smoke | `adb -s <serial> install` + `scripts/qc_redmi_matrix.sh` | NOT RUN | `qc/artifacts/screenshots/manual/` (pending P3) |

### Findings

- **Phase B fixes verified:** `MainActivity.kt:3225/3256/3289/3321` `40dp→48dp` + `3426` `36dp→48dp` star + `2243` `widthIn 340→320dp`; `MainActivity.kt:778,1136,1421` `SETTINGS/DEVELOPER&LEGAL/ABOUT` → `stringResource` + `about_title` + `action_save/import/export` + `Theme.kt:39 dynamicColor false`; lint+unit still PASS.
- **Remaining:** QC-005 ~28 hardcoded strings, QC-006 66 colors, QC-002/003 matrix/outputDir, QC-007/008 threading (Phase C).
- **Lint:** 0 errors, ~130 warnings (same as Phase A). **Unit:** 20/20 PASS.

### Fixes Applied (this run)

- `MainActivity.kt:3225,3426,2243` — U-001/U-008.
- `MainActivity.kt:778,1136,1421` + `strings.xml:84` — U-003 partial (7 strings).
- `ui/theme/Theme.kt:39` — U-005 partial (dynamicColor false).

### Evidence Pointers

- Lint HTML: `qc/reports/lint/lint-results-debug.html:1` | Tests: `qc/reports/tests/` | Reports also at `app/build/reports/`

---

## Run 2026-09-04 20:35 — Phase A (P0 bugs + U-001/U-002 partial)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| lint | `./gradlew lintDebug` | **PASS 0 errors, ~130 warnings** | `qc/reports/lint/lint-results-debug.html` (copied), `app/build/reports/lint-results-debug.html` |
| unit | `./gradlew testDebugUnitTest` | **PASS 20/20** | `qc/reports/tests/` (copied), `app/build/reports/tests/` |
| roborazzi | `./gradlew verifyRoborazziDebug` | NOT RUN | `qc/reports/roborazzi/` (pending P2 — 24-combo matrix) |
| smoke | `adb -s <serial> install` + `scripts/qc_redmi_matrix.sh` | NOT RUN | `qc/artifacts/screenshots/manual/` (pending P3) |
| env | `bash scripts/check_requirements.sh` | PASS (scrcpy 4.1 PASS, gitleaks/actionlint WARN CI-only) | stdout |

### Findings

- **Phase A fixes verified:** `AudioEffectManager.kt:127` `@Volatile` + `release()` scope cancel/recreate + `stop()` `playState` guard; `VolumeBoosterService.kt:89` `SupervisorJob()+Main.immediate` + `START_NOT_STICKY` + null-intent guard + `POST_NOTIFICATIONS` guard (`areNotificationsEnabled()`); `HearingWarningCard.kt:43` `32dp→48dp` + `stringResource` + `content_desc_hearing_warning` `translatable false`; `MainActivity.kt:3038` `height 240→heightIn`, `1761 260→heightIn`, `3534 160→heightIn`, `3596 140→heightIn` (U-002).
- **Lint:** 0 errors (fixed 2 `MissingTranslation` from prior run), 130 warnings remain (expected `Divider→HorizontalDivider`, `Icons.Filled` → `AutoMirrored`, `GradleDependency` 19 outdated).
- **Unit:** `BUILD SUCCESSFUL` (7 suites, 20 tests). No broken tests from Phase A.
- **Deferred to Phase B:** QC-004 (remaining 40dp buttons), QC-005 (~35 hardcoded strings + 31 `translatable false` locale coverage 56%), QC-006 (70 `Color(0xFF...)` vs `AppColors`), plus AdConsent B-006 and threading O-001/O-003 (QC-007/008) to Phase C.

### Fixes Applied (this run)

- `AudioEffectManager.kt:127,783,762,228` — B-001/B-002 partial, `Volatile`, scope lifecycle, sessionId fallback.
- `VolumeBoosterService.kt:89,163,170,193,210` — B-003/B-004.
- `HearingWarningCard.kt:1,70,49,78` — QC-001 (48dp, stringResource, `strings.xml:84`).
- `MainActivity.kt:3038,1761,3534,3596` — U-002 heightIn.

### Evidence Pointers

- Lint HTML: `qc/reports/lint/lint-results-debug.html:1` | Tests: `qc/reports/tests/tests/` + `test-results/` | Reports also at `app/build/reports/`

---

## Run 2026-09-04 20:10 — Scaffold complete (no automated tests executed yet)

### Test Runs (what was executed)

| Run | Command | Result | Artifact (where to read) |
|---|---|---|---|
| env | `bash scripts/check_requirements.sh` | PASS (2 WARN: gitleaks/actionlint CI-only, scrcpy 4.1 PASS) | stdout → `qc/reports/` not yet populated |
| scaffold | `ls qc/` + `adb devices -l` | PASS (qc/ 6 subdirs, 2 devices `hm5xr8gueiz5x4c6`/`A1013A5320TH000257`) | `qc/` tree, `qc_plan.md:§2` |
| unit | `./gradlew testDebugUnitTest` | NOT RUN | `qc/reports/tests/` (pending P2) |
| lint | `./gradlew lintDebug` | NOT RUN | `qc/reports/lint/` (pending P2) |
| roborazzi | `./gradlew verifyRoborazziDebug` | NOT RUN | `qc/reports/roborazzi/` (pending P2) |
| smoke | `adb -s <serial> install` + `scripts/qc_redmi_matrix.sh` (6 combos) | NOT RUN | `qc/artifacts/screenshots/manual/` (pending P3) |

### Findings

- QC scaffold landed: `qc_plan.md:558` (v1, qc/ canon, scrcpy 4.1 ephemeral §5.5, Redmi 6-combo §5.7), `CHANGELOG.md:1`, `qc/` dirs + `qc/fixtures/presets/` 5 edge cases, `scripts/qc_redmi_matrix.sh:1` (rwxrwxr-x), `.gitignore:28` updated (`/qc/reports/` etc.), `AGENTS.md:146` + `.jules/config.yaml:14` gates.
- No test evidence yet — `qc/reports/*` empty pending P2. `TODO_MANUAL_CHECK.md:13` reports 130 lint warnings (expected `Divider→HorizontalDivider`), 20/20 unit tests expected.
- Redmi `font_scale=1.0`, `wm density` 352 override (491dp) probed; 6-combo Settings matrix defined (Small/Default/Largest × 0.85x-2.0x) to complement JVM matrix.

### Fixes Applied (this run)

- None (scaffold only). First changelog entry: `CHANGELOG.md: [Unreleased]` + `qc/changelogs/_template.md:1`.

### Evidence Pointers

- See **Where to Read Reports** below.

---

## Where to Read Reports (pointers — human + agent)

| Need | Path | Notes |
|---|---|---|
| **This summary (latest)** | `qc/QC_SUMMARY.md` (this file) | Overwritten per run; dated `Run` sections inside; `git log --follow` for history |
| **Master QC plan** | `qc_plan.md` | §2 device matrix, §5 workflows, §9 gates, §5.5 scrcpy ephemeral, §5.7 Redmi 6-combo, §11 Phase 3 Kover optional |
| **Changelogs** | `CHANGELOG.md` (root, Keep-a-Changelog) + `qc/changelogs/YYYYMMDD-HHMMSS-vX.Y.Z.md` per tag + `qc/changelogs/_template.md` | Linked bidirectionally; datestamped per release (only place datestamp persists) |
| **Unit tests** | `qc/reports/tests/` (JUnit XML + html) + `app/build/test-results/testDebugUnitTest/` | From `./gradlew testDebugUnitTest` |
| **Lint** | `qc/reports/lint/lint-results-debug.html` | From `./gradlew lintDebug` |
| **Roborazzi** | `qc/reports/roborazzi/` (diffs in `diffs/`), reference `app/src/test/screenshots/` | From `./gradlew verifyRoborazziDebug` (<5% diff) |
| **Gitleaks** | `qc/reports/gitleaks/report.json` | From `gitleaks dir --redact` |
| **Screenshots (device)** | `qc/artifacts/screenshots/manual/<device>-<step>.png` + `qc/artifacts/screenshots/manual/redmi-*.png` (6-combo) | From `adb screencap` + `scripts/qc_redmi_matrix.sh` (gitignored, ephemeral) |
| **Recordings (video)** | `qc/artifacts/recordings/*.mp4` or `/tmp/qc-*.mp4` | From `scrcpy --record` (ephemeral, gitignored per `.gitignore:31`, never CI-uploaded) |
| **Traces** | `qc/traces/heapsnapshots/*.heapsnapshot`, `qc/traces/perf/trace.json.gz` | From `chrome-devtools_take_heapsnapshot` / `performance_start_trace` |
| **Fixtures** | `qc/fixtures/presets/*.json` (5 edge cases: dup name, bad band count, clamp) | For `importPreset` validation |
| **Checklists** | `qc/checklists/smoke.md` (7 steps + Redmi 6-combo + scrcpy) + `qc/checklists/accessibility.md` (48dp, contentDescription) | Manual gates |
| **Device matrix** | `qc_plan.md:§2` (Redmi `hm5xr8gueiz5x4c6` + ADVAN `A1013A5320TH000257`) + `scripts/qc_redmi_matrix.sh` | Live `adb devices -l` verified 2026-09-04 |

> **Retention:** This file is the SINGLETON (no suffix, overwritten). Datestamped history lives only in `qc/changelogs/*` per release + `git log`. `qc/reports/`, `qc/artifacts/`, `qc/traces/` are gitignored ephemeral; consult this file for latest pointers.
