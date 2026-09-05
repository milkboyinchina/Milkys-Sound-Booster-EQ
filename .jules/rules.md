# 📜 Google Jules Agent Execution Rules (.jules/rules.md)

These guidelines govern how **Google Jules** must analyze, edit, build, verify, and submit code changes to this repository.

---

## 🔒 1. Audio DSP Engine & Safety Guardrails

1. **Volume Boost Ceiling**: Maximum gain amplifications must **NEVER exceed 200% (+15dB)** (`AudioEffectManager.kt:281` 1500 mB). EQ is always tunable — do not gate `+/-` with `isBoostEnabled`.
2. **Hearing Protection Warning**: Do not modify, hide, or alter the high-contrast safety warning banner for speaker damage and hearing impairment.
3. **Audio Effect Lifecycle**: All `AudioEffect`, `Equalizer`, `LoudnessEnhancer`, and `PresetReverb` instances **MUST** be explicitly released in `onDestroy()` / `onCleared()`.
4. **Session ID Binding**: Always pass the correct global system audio session ID (`0`) or active media player audio session ID to the audio service.

---

## 🎨 2. Android Kotlin & Jetpack Compose Rules

1. **No Hardcoded UI Strings**: Every user-facing UI text string must be referenced via `stringResource(R.string.<id>)` and defined in `res/values/strings.xml`.
2. **Compose Performance**: Use `remember`, `derivedStateOf`, and stateless composable parameters to avoid unnecessary recomposition loops.
3. **Async & Thread Safety**: Never invoke `AudioEffect` mutations or database IO operations directly on `Dispatchers.Main`. Always wrap in `withContext(Dispatchers.IO)`.
4. **Localization**: Preserve multilingual capability across all 13 supported languages.

---

## 🧪 3. Mandatory Build & Test Verification

Before submitting a Pull Request, Jules **MUST**:
0. **Device prep (before any dump/screencap/swipe/click after pm clear/install -r):** `bash scripts/device_prep.sh hm5xr8gueiz5x4c6 .build-outputs/app-playstore-debug.apk` + `bash scripts/device_prep.sh A1013A5320TH000257 .build-outputs/app-playstore-debug.apk` — Q17 silent `pm grant POST_NOTIFICATIONS` + Q18 `run-as has_seen_onboarding=true` (fallback tap `GET STARTED` if `run-as` fails) — prevents `Allow`/`GET STARTED` from blocking `uiautomator dump`/`screencap`/`input swipe` (see `AGENTS.md:§3.2 D`).
1. Run `./scripts/setup_jules_env.sh` to ensure the environment dependencies are synced.
2. Run `./gradlew testDebugUnitTest` to ensure all unit tests pass without failure (reports → `qc/reports/tests/`) — must include `EqualizerComponent +/-` `performClick` → `Text +1dB` + `_eqBands` Flow (booster OFF/ON, Flat→Custom, both devices).
3. Run `./gradlew lintDebug` — must show 0 errors (report → `qc/reports/lint/`).
4. Run `./gradlew verifyRoborazziDebug` and ensure 0 failures before PR (outputs → `qc/reports/roborazzi/`, reference `app/src/test/screenshots/`).
5. Run `./scripts/build.sh assembleDebug` to confirm that the Android Debug APK builds successfully and outputs to `.build-outputs/` (copy to `qc/artifacts/apks/`).
6. Ensure `qc_plan.md` §9 gates and `CHANGELOG.md` + `qc/changelogs/` linkage are satisfied (see `qc/checklists/smoke.md`).
7. Verify 5-band EQ `+/-` always tunable on both `hm5xr8gueiz5x4c6` + `A1013A5320TH000257` (screencap `qc/artifacts/screenshots/manual/eq-*.png`, Flat→Custom and Custom, booster OFF/ON) — EQ is always tunable per `MainActivity.kt:3088` + `AudioEffectManager.kt:464`, Q1.
8. **Version bump guard**: If `GITHUB_REF` starts with `refs/tags/v*`, set `SKIP_VERSION_BUMP=1` and DO NOT edit `VERSION_CODE/NAME` in `.env` — tag is source of truth (`scripts/build.sh:19-22`, `scripts/build_play_console_release.sh:19-22`).
9. **Keystore subfolder**: `KEYSTORE_PATH=keystore/release.jks` via `.env` (`.jules/config.yaml:environment.keystore_path`); never commit `keystore/*` or `*.jks`; `verify` may use `debug.keystore` fallback `app/build.gradle.kts:116` but `release` needs real keystore via `KEYSTORE_BASE64` secret decoded to that path.

---

## 🔀 4. Git Commit & Pull Request Guidelines

1. **Commit Messages**: Write imperative, descriptive commit titles (e.g. `feat(audio): add custom equalizer preset persistence`).
2. **PR Format**: Include:
   - **Summary**: Concise overview of what was changed and why.
   - **Verification**: Output log confirmation of `./gradlew test` and APK compilation.
   - **Ref**: Link to the original GitHub issue.

---

## 🤖 5. Jules Automation & Delegation (CI hook)

1. **Trigger**: `jules-fix` runs only when `verify` or `build-and-release` fails (`ci-cd.yml:142`, guard `secrets.JULES_API_KEY != ''`), not cron.
2. **Branch**: `starting_branch` must be `github.head_ref || fix/jules-${run_id}` — never a tag name `v*`.
3. **Scope**: Good for `quick` (lint 48dp/contentDescription, strings, AppColors) + `standard` (Roberazzi re-record, unit quench) — keep `hard-fix` (audio DSP 1500 mB, foreground service) human.
4. **Recommendation queue** (`qc/QC_SUMMARY.md:Next Actions`, all 3 queued): `1) pin actions to SHA + qc-reports/pages 2) setup_jules_env subfolder+bump 3) re-record 24-combo matrix`.
5. **Protected**: Never edit `keystore/*`, `*.jks`, `.env`, `qc/changelogs/*` — respect `.jules/config.yaml:protected_paths`.
