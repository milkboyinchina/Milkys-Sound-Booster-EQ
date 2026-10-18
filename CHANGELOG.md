# Changelog

All notable changes to Milkys Sound Booster & EQ will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Versioning is driven by `VERSION_CODE`/`VERSION_NAME` in `.env` (bumped by `scripts/bump_version.py` on `scripts/build.sh`). Per-tag verbose QC notes live in `qc/changelogs/`.

## [1.0.7] - 2026-10-18 (release pipeline fail-closed)
### Fixed
- `scripts/build.sh` and `scripts/build_play_console_release.sh` enable `set -o pipefail`, so Gradle failures are no longer masked by `| tee`.
- Release workflow generates an ephemeral `debug.keystore` when `KEYSTORE_BASE64` is missing (with a warning), allowing a testable release build on a clean checkout.
- Release staging now fails if no APK/AAB files were produced, instead of silently publishing an empty release.

## [1.0.6] - 2026-10-18 (release asset packaging)
### Fixed
- Tag releases now stage `.build-outputs` APK/AAB files into the visible `release-assets/` directory before upload. The previous `v1.0.5` run created a release but attached no assets because hidden dot-directories are ignored by the artifact and release actions.

## [1.0.5] - 2026-10-18 (CI green, font-scale EQ, update checker)
### Added
- Settings → About update checker with per-channel sources: Play Store listing, GitHub Releases API, and F-Droid package API. Shows current version/channel, checking/up-to-date/available/not-found/error states, and opens the store/release page. Play Core native in-app updates remain deferred to the flavor work.
- `EqualizerFontScaleTest` 320dp matrix at 1.0x/1.5x/2.0x, light/dark, with 48dp and `+1 dB` callback assertions.
- Jules issue-label and weekly QC workflows with secret guards, concurrency, and pinned action SHA.

### Changed
- Screenshot tests render with `showDebugInfo = false`; matrix references moved under `app/src/test/screenshots/` and Play Console references tracked under `app/screenshots/`.
- EQ bands wrap responsively (5/3/2 columns) and preset actions use 2 columns at ≥1.5x; 48dp targets preserved.
- `BUILD_TARGET` / `INCLUDE_GOOGLE_ADS` now honour exported environment values over `.env`, fixing per-target builds.
- GitHub releases are tags-only; action majors bumped; CI pinned to `ubuntu-24.04`; Pages gated behind `ENABLE_PAGES`.
- Example developer/privacy URLs aligned with production (`milkboy.my.id`) so CI screenshots match local baselines.

### Fixed
- Per-commit Roborazzi failures caused by commit SHA/build time in the debug header.
- Missing Play Console reference images on clean CI checkouts.
- Invalid `google-labs-code/jules-action@v1` reference and non-existent synthetic `starting_branch`.
- Font-scale clipping in the equalizer header, band controls, and preset action row (issue #3).

### Verified
- Unit tests 72/72, `lintDebug` 0 errors, clean `verifyRoborazziDebug`, actionlint, Gitleaks, playstore+fdroid debug build, GitHub Actions run #44 green.

## [1.0.4] - 2026-10-12 (cold-start black-screen fix, API 28)
### Fixed
- Black screen on second launch on Android 9 and below (API ≤32) for users with a persisted per-app language (Play review: Vivo V1901K, Indonesian): unconditional `AppCompatDelegate.setApplicationLocales()` in `MainActivity.onCreate` re-triggered Activity recreation forever (377 `onCreate` loop observed on an API 28 emulator, no crash, `mCurrentFocus=null`). `AudioEffectManager.applyLanguageToApp` is now idempotent (skip when AppCompat already holds the list + re-entry guard) and the deprecated `resources.updateConfiguration()` block is removed. Full notes: `qc/changelogs/20261012-004500-v1.0.4.md`.
- Banner ad issued two `loadAd` calls per composition (first before the `AdListener` was registered); now a single `loadAd` after listener registration.
### Added
- `LocaleColdStartTest` (Robolectric, SDK 28, 4 tests) pinning locale-apply idempotence: 5 identical applies trigger at most one `setApplicationLocales`.
### Verified
- API 28 emulator (Vivo-like AVD, R8 release `1.0.4/26101202`): Indonesian cold start renders full dashboard, 0 fatals. Unit tests 48/48, `lintDebug` 0 errors.

## [1.0.2] - 2026-10-08 (Play-linked AdMob rotation)
### Changed
- AdMob IDs rotated to Play-linked app `~3500911599` (13 locales `strings.xml`), banner `/8968860697`, native `/5813406288`; unit IDs centralized in `AdUnits` object (`MainActivity.kt`).
- Staged production rollout target (10–20%); `app-ads.txt` verified live (`pub-6770897791730584 DIRECT`).

## [1.0.0] - 2026-10-03 (first production release)
### Added
- Planned Phase 3 — Optional Kover coverage (soft report-only, deferred from v1 gates).
- Onboarding page 0: Notification Bar Controls explainer (media-control purpose, never ads, Settings toggle note) with Allow Notifications / Skip; system permission now user-triggered, advances on grant or deny (Q1-A).
- Favorite slots #1–#4: ordered favorites with `#N ★` labels in Preset Manager and `#N` labels on overlay favorite boxes; untoggle compacts, retoggle fills lowest free slot (Q3-A).
- Overlay ⏻ power button (bold filled when enabled, greyed when disabled) replacing the Master Power switch (Q2-A).

### Changed
- EQ band columns top-anchored: `+` sits 5.dp under dB readout, track housing fitted so buttons no longer overlap the 80.dp bar (4a/4c/4d).
- Removed embedded Preset Manager section from Equalizer card (dedicated PresetManagerCard is the single owner; signatures + 3 layout call sites trimmed).
- DataStore: ordered favorites persist as `favorite_presets_ordered` string; legacy unordered set migrates sorted-alpha into slots, dual-written for rollback compat.

### Fixed
- EQ off→on volume drop + toggle compounding: `applyPreset`/`setEqEnabled(true)`/`setBandLevel` wrote hardware via a fire-and-forget `audioScope` queue landing 10–30s late (OFF was always sync), so rapid toggles stacked stale writes and `initEffects()` enabled EQ hardware from booster state while the switch showed OFF. All EQ hardware writes are now synchronous on the caller thread; `initEffects` + booster paths honor `_isEqEnabled`; bridge `dump_state` carries hardware readback (`AudioEffectManager.hardwareSnapshot()`).
- POWER on/off bounce (ON flashed then OFF, 2nd retry stuck): `release()` no longer nulls the persist context (writes were silently dropped) and `init()` seeds flows from disk only on first init — service-restart re-init never clobbers live state; `BoosterToggleBounceTest` pins both invariants.
- Crash loop `ForegroundServiceDidNotStartInTimeException` when overlay + booster ON with notifications denied: service now always calls `startForeground()` (denied just suppresses visuals); `onCreate` breaker skips silent auto-start, resets boost, and toasts toward Settings; pure `shouldAutoStartService()` + 4 unit tests.

### Changed
- Deferred `Color(0xFF...)` remaining bespoke warning colors — low risk, tracked in `qc/QC_SUMMARY.md`.

## [0.1.25] - 2026-09-05
### Added
- `isEqEnabled` StateFlow `false` default + `DataStore` `eq_enabled` + `PresetManagerCard` own card (`showText` icons-only in `EXPANDED` `Pane3` landscape, `true` in `COMPACT`/`MEDIUM` vertical below `Banner`).
- `EQ Enabled` `Switch` `48dp` in `EqualizerComponent` header (`VisualEqualizerCard` `onToggleEq`).

### Changed
- **Layout `Q7/Q8`:** `EXPANDED` `Pane1: QuickBoost` below `Decibel` (was `Pane2` below `EQ`), `Pane3: Banner + PresetManager(icons) + Battery` (was `Battery` only); `COMPACT` `LazyColumn` now `EQ → Banner → PresetManager → Battery` (was `Banner` before `EQ`); `MEDIUM` after `Row` adds `Banner + PresetManager + Battery` full-width below `Row` (was `Battery` in left column).

### Fixed
- **EQ `+/-` always tunable `Q1`:** `EqualizerComponent:3088` `IconButton enabled = isEnabled && level<15` where `isEnabled` is now `isEqEnabled` (was `isBoostEnabled`), so `EQ` tunable when `EQ` toggle `ON` even if booster `OFF`; `pointerInput(isEnabled)` + `Brush` + `background/tint` gated by `isEqEnabled`, `AudioEffectManager` `setBandLevel` `clone` + `audioScope` defer + `getBandLevelRange` clamp, `getPresetBands` `clone()`.
- **Power toggle `on>off>on(fail)>on(ok)`:** `AudioEffectManager.kt:342` `@Synchronized setBoostEnabled` with stale `audioTrack` check + retry, `DashboardScreen:205` `lastPowerToggleTime` `500ms` debounce + `try {onStartService} catch {setBoostEnabled(false)}`.

## [0.1.20] - 2026-09-04
### Added
- `qc_plan.md` v1 (506 lines → 580) — QA & QC master plan with `qc/` canon, device matrix (Redmi API36 1080×2340, ADVAN API34 1280×800), scrcpy 4.1 ephemeral (§5.5), Redmi 6-combo display/font matrix (§5.7), and changelog system.
- `qc/` folder structure: `qc/reports/{lint,tests,roborazzi,gitleaks}`, `qc/artifacts/{apks,screenshots/manual,recordings}`, `qc/traces/{heapsnapshots,perf}`, `qc/fixtures/{presets,locales}`, `qc/checklists/`, `qc/changelogs/`, `qc/QC_SUMMARY.md` (singleton queue vs log, 0 OPEN).
- `data/PreferencesRepository.kt` — DataStore 1.1.7 with 15 keys + `Flow` + `migrateFromSharedPrefs` + dual-write `persist*` on `Dispatchers.IO` (QC-008).
- `QcVisualMatrixTest.kt` — 6-combo Roborazzi matrix (compact 320/standard 411/expanded 600 × light/dark × landscape) + `roborazzi outputDir qc/reports/roborazzi` (QC-002/003) — 6/6 PASS, `matrix-*.png` 41K-302K.
- `scripts/qc_redmi_matrix.sh` — Redmi 6-combo helper (`wm density` + `font_scale`, 6 PNGs to `qc/artifacts/screenshots/manual/`).
- `CHANGELOG.md` + `qc/changelogs/_template.md` + `qc/changelogs/20260904-*.md` per tag (Keep-a-Changelog, linked).
- `strings.xml` 14 new keys `action_copy/share/pick_file/paste_clipboard/save_apply/dialog_*` (QC-005, 22 `Text("` →2).

### Changed
- `.gitignore` — added `/qc/reports/`, `/qc/artifacts/`, `/qc/artifacts/recordings/`, `*.mp4`, `/qc/traces/`, `*.heapsnapshot`, `.roborazzi/` ignores with `!qc/changelogs/*.md`, `!qc/fixtures/*`, `!qc/checklists/*.md` keep.
- `AGENTS.md` §3.1/3.4/4/7 — added Roborazzi, scrcpy, `qc/` canon, `qc_plan.md`/`CHANGELOG.md` links, `QC_SUMMARY.md` queue vs log.
- `.jules/config.yaml` — added `qc_path`, `reports_path`, `changelogs_path`, `roborazzi_command`, `coverage_command: null`.
- `.jules/rules.md` — added gates 4-6 (verifyRoborazzi, qc/reports artifacts, changelog linkage).
- `scripts/check_requirements.sh` — added QC tools soft check (scrcpy 4.1, gitleaks, actionlint, qc/ layout).
- `ui/theme/Theme.kt:39` — `dynamicColor true→false` to preserve `AppColors`; `MainActivity.kt:122` Scaffold `MaterialTheme`, 149 `Color(0xFF... )→AppColors` (149/182 fixed, 38 remaining bespoke warnings — QC-006 partial).

### Fixed
- `AudioEffectManager.kt:127,228,783` — B-002 `@Volatile` + `audioScope cancel/recreate + context=null` in `release()`, `stop()` `playState` guard, `initEffects` sessionId fallback + `enabled` try/catch (B-001 partial).
- `VolumeBoosterService.kt:89,163,170,193,210` — B-003 `START_STICKY→NOT_STICKY` null-intent guard + `store.clear()` after `cancel()`, B-004 `POST_NOTIFICATIONS areNotificationsEnabled()` guard.
- `HearingWarningCard.kt:43` — QC-001 `32dp→48dp` + `defaultMinSize(48.dp)` + `contentDescription stringResource` + `content_desc_hearing_warning` translatable false.
- `MainActivity.kt:3038,1761,3534,3596,3225,2243` — U-002 `height→heightIn(min=...)` (EQ 240, license 260, JSON 160/140), U-001 `40dp→48dp` buttons + `36dp→48dp` star + `widthIn 340→320dp`.
- `AdConsentManager.kt:10,24,84` — QC-007 `omp→ump` typo, `ConsentRequestParameters` class + Activity overload fallback, `runOnUiThread` for `loadAndShow`.

### Security
- No new secrets — Gitleaks 0 errors (verified via `gitleaks dir --redact`).

**Full QC report:** `qc/QC_SUMMARY.md` (0 OPEN, Run 2026-09-04 21:30 `lint 0e`, `test 20/20`, `roborazzi 6/6`, `P3 smoke PASS` 2 devices, `qc/reports/` + `qc/artifacts/screenshots/manual/` 8 files, `qc/changelogs/20260904-211000-v0.1.1-p3-smoke.md`).

## [0.1.0] - 2026-09-04
### Added
- Initial baseline (existing app: global boost up to 200%/1500mB, 5-band EQ, Tile/Overlay, AdMob, 13 locales).

