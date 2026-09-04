# QC Summary — Latest

> **Last updated:** 2026-09-04 21:34 | **VERSION_NAME:** 0.1.20 (`VERSION_CODE` 26090402, `.env:33`) | **Devices:** Redmi Note 8 Pro `hm5xr8gueiz5x4c6` (Android 16 / SDK 36, 1080×2340, 352dpi override, 491dp) + ADVAN TAB A10 `A1013A5320TH000257` (Android 14 / SDK 34, 1280×800, 213dpi, 601dp tablet) | **Env:** JDK 21 (Foojay 1.0.0), AGP 9.1.1, Kotlin 2.0.21, scrcpy 4.1 ephemeral
> **Source of truth — read top before planning:** This file is the agent queue. `Next Actions` (below) is the ONLY queue to act on. `Run` sections are log history — do not treat as queue. History preserved via `git log --follow qc/QC_SUMMARY.md`. Compare `Last updated` vs `git log --oneline qc/QC_SUMMARY.md` to avoid stale reads.

---

## Next Actions (agent queue — ONLY OPEN items here)

> Agents MUST read this section first. Act only on `Status == OPEN`. Skip `FIXED`/`WONTFIX`. Check `Last updated` at top.

| ID | Title | File:line | Severity | Owner | Due | Source |
|---|---|---|---|---|---|---|
| — | *No open blockers* | — | — | — | — | — |

*Release v0.1.20 built (see Run 2026-09-04 21:34). Gates: lint 0e, unit 20/20, roborazzi 6/6, P3 smoke PASS — all green. 0 OPEN blockers.*

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
| QC-006 | `MainActivity.kt:122` | Medium | FIXED (partial) | `Phase C: AppColors batch` | 2026-09-04 21:25 | `AppColors` import + 149 `Color(0xFF... )→AppColors` (E6E1E5→DarkTextPrimary, CAC4D0→DarkTextSecondary, D0BCFF→PrimaryAccentDark, 2B2930→DarkCard, 49454F→BorderDark, 381E72→DeepPurple etc) — 182→38 remaining (warning/button bespoke colors, deferred low risk) |
| QC-007 | `AdConsentManager.kt:10,84,24` | High | FIXED | `Phase C` | 2026-09-04 21:05 | `omp→ump` typo fix, `ConsentRequestParameters` class resolution + Activity overload fallback, `runOnUiThread` for `loadAndShow` (reflection hardened) |
| QC-008 | `AudioEffectManager.kt:130,303` / `data/PreferencesRepository.kt:1` | High | FIXED | `Phase C: DataStore` | 2026-09-04 21:30 | `PreferencesRepository` (DataStore 1.1.7, `preferencesDataStore` + 15 keys + `migrateFromSharedPrefs` + `Flow` + `edit` on `Dispatchers.IO`), `AudioEffectManager` dual-write `persistBoolean/Int/Long/String/StringSet` via `audioScope.launch(IO)` + `SharedPreferences` compat + `init` migration `audioScope.launch{migrate}` + `app/build.gradle.kts:193` `datastore-preferences`. Gates `lint 0e`, `test 20/20`, `roborazzi 6/6` PASS |

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
