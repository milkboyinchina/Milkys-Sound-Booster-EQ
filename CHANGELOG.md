# Changelog

All notable changes to Milkys Sound Booster & EQ will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Versioning is driven by `VERSION_CODE`/`VERSION_NAME` in `.env` (bumped by `scripts/bump_version.py` on `scripts/build.sh`). Per-tag verbose QC notes live in `qc/changelogs/`.

## [Unreleased]
### Added
- Planned Phase 3 — Optional Kover coverage (soft report-only, deferred from v1 gates).

### Changed
- Deferred `Color(0xFF...)` remaining 38 bespoke warning colors — low risk, tracked in `qc/QC_SUMMARY.md`.

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

