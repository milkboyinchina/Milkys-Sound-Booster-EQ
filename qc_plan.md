# ✅ QC Plan — Milkys Sound Booster & EQ (v1, `qc/` canon)

> **Version:** 1.0 | **Date:** 2026-09-04 | **Target:** Android 7.0+ (Min SDK 24, Target SDK 36) | **Language:** Kotlin 2.0.21 | **Build:** Gradle 8.5+ Kotlin DSL, JDK 21 (Foojay 1.0.0), AGP 9.1.1
> **Devices verified via `adb devices -l` (adb 37.0.1):** Redmi Note 8 Pro `hm5xr8gueiz5x4c6` (Android 16 / SDK 36, 1080×2340, 352dpi override), ADVAN TAB A10 `A1013A5320TH000257` (Android 14 / SDK 34, 1280×800, 213dpi tablet, 601dp sw)
> **Source of truth:** This plan. See `AGENTS.md:§3` + `.jules/config.yaml` for CI references + `CHANGELOG.md` + `qc/changelogs/` for release notes.

---

## 1. Objectives & Scope

**Goal:** Establish a repeatable QA & QC system that gates releases without blocking iteration on `staging`, protects audio safety, guarantees UI integrity across form factors, and produces auditable artifacts per build.

**In-scope:**
- **Audio DSP safety:** `LoudnessEnhancer` gain capped at **200% / +15 dB (1500 mB)** (`AudioEffectManager.kt:280-283` `mapProgressToGain`), hearing-warning banner persistence (`HearingWarningCard.kt`), `AudioEffect` lifecycle `release()` in `onDestroy()`/`onCleared()` (`AudioEffectManager.kt:778-791`, `VolumeBoosterService.kt:169`).
- **UI integrity:** Jetpack Compose + Material 3, 3 window-size groups (`MainActivity.kt:83-87` COMPACT/MEDIUM/EXPANDED), 13 locales, light/dark themes, AdMob conditional builds (`BUILD_TARGET=playstore|fdroid|both`, `INCLUDE_GOOGLE_ADS`).
- **System integrations:** Foreground service `mediaPlayback` (`AndroidManifest.xml:46-51`), `TileService` (`VolumeBoosterTileService.kt`), overlay `SYSTEM_ALERT_WINDOW`, `POST_NOTIFICATIONS` (Android 13+).
- **Security/privacy:** Gitleaks + `actionlint`, no hardcoded secrets (`.env` → `BuildConfig` via `secrets` plugin `app/build.gradle.kts:168-173`).

**Out-of-scope v1:** Kover coverage gate (deferred to **Phase 3 — Optional**, §11), Play Console internal-track upload automation, performance benchmarking beyond traces.

**Quality philosophy:** Gates are **evidence-based** — every PR must produce `qc/reports/` artifacts; manual device smoke supplements (not replaces) automated JVM checks.

---

## 2. Device Matrix & ADB Inventory

### 2.1 Live ADB Inventory (probed 2026-09-04, `adb 37.0.1` at `/home/milkboy/Android/Sdk/platform-tools/adb`)

| # | `adb -s` Serial | Model (`ro.product.model`) | Android (`ro.build.version.release` / `ro.build.version.sdk`) | Physical (`wm size` / `wm density`) | Config sw | Form factor | Role in matrix |
|---|---|---|---|---|---|---|---|
| 1 | `hm5xr8gueiz5x4c6` | **Redmi Note 8 Pro** (`begonia`, `infinity_begonia`) | **16 / 36** (matches `targetSdk 36`) | 1080×2340, 440dpi physical / **352dpi override** (`1080x2340 rng 1080x1080-2340x2340`) | 491dp (`sw491dp w491dp h1064dp`) | Phone portrait (compact baseline 360-411dp) | Primary daily-driver, Android 16 edge, notification permission, Tile/Overlay on MIUI |
| 2 | `A1013A5320TH000257` | **ADVAN TAB A10** (`TAB_A10`) | **14 / 34** (matches Roborazzi `@Config(sdk=[34])`) | 1280×800, 213dpi | 601dp (`sw601dp w962dp h541dp land`) | Tablet expanded (≥600dp) | Tablet/landscape matrix, 2-col/3-col dashboard (`MainActivity.kt:348-749`), scrollability, split-pane |

Both `device` state, usable for `adb install`, `adb shell dumpsys audio`, `dumpsys window displays`, `screencap`. Battery: Redmi 85% USB powered — safe for instrumented runs.

### 2.2 Gap Analysis & Recommended Emulator Fill

Current physical fleet covers **2 extremes (16/phone + 14/tablet)** but leaves gaps:

| Gap | Recommended emulator | Purpose |
|---|---|---|
| No low-end API 24-28 | **Pixel 3a API 24** (320dp compact, 1GB RAM) | Verify `minSdk 24` audio fallback, small-screen wrapping |
| No mid API 30-33 | **Pixel 8 API 30** + **Pixel 8 API 33** | `POST_NOTIFICATIONS` introduction (33), scoped storage |
| No foldable | **Pixel Fold API 34** (unfolded 840dp) | Validate `WindowSizeGroup.EXPANDED` 3-pane (`MainActivity.kt:617`) |
| No `fdroid` build device | Use either device with `BUILD_TARGET=fdroid` APK | Confirm no AdMob symbols, no reflection crash |

Create via `avdmanager create avd -n <name> -k "system-images;android-<sdk>;google_apis;x86_64"` and launch with `emulator -avd <name> -no-snapshot`.

### 2.3 ADB Workflow per Device

```bash
adb devices -l                                          # inventory
adb -s hm5xr8gueiz5x4c6 shell getprop ro.build.version.release
adb -s hm5xr8gueiz5x4c6 shell wm size; adb -s hm5xr8gueiz5x4c6 shell wm density
adb -s <serial> install -r .build-outputs/app-playstore-debug.apk
adb -s <serial> shell pm list packages | grep milkys
adb -s <serial> shell dumpsys audio | grep -i loudness # leak check after service stop
adb -s <serial> shell dumpsys meminfo com.milkys.soundbooster
adb -s <serial> shell screencap -p /sdcard/qc_screenshot.png && adb -s <serial> pull /sdcard/qc_screenshot.png qc/artifacts/screenshots/manual/
```

---

## 3. Folder Structure for QA & QC (`qc/` canon — Q1 decision)

Canonical root: **`qc/`** (not `qa/`). `qc_plan.md` lives at workspace root; `qc/` holds all generated evidence. `CHANGELOG.md` lives at workspace root (Keep-a-Changelog), `qc/changelogs/` holds per-tag verbose notes linked from `CHANGELOG.md`.

```
Milkys-Sound-Booster-EQ/
├── qc_plan.md                    # ← this plan (workspace root)
├── CHANGELOG.md                  # Keep-a-Changelog (root, tracked)
├── qc/                           # QA & QC evidence root (canon)
│   ├── reports/
│   │   ├── lint/                 # lint-results-debug.html/.xml  (from ./gradlew lintDebug)
│   │   ├── tests/                # JUnit XML + html (from ./gradlew testDebugUnitTest)
│   │   ├── roborazzi/            # verifyRoborazzi outputs, diff images (24-combo matrix)
│   │   └── gitleaks/             # gitleaks report.json
│   ├── artifacts/
│   │   ├── apks/                 # copies/symlinks from .build-outputs/ (playstore/fdroid)
│   │   ├── screenshots/manual/   # adb screencap pulls per device (tracked PNGs)
│   │   └── recordings/           # scrcpy --record MP4s (ephemeral, gitignored)
│   ├── traces/
│   │   ├── heapsnapshots/        # *.heapsnapshot, *.hprof (chrome-devtools_take_heapsnapshot)
│   │   └── perf/                 # trace.json.gz (chrome-devtools_performance_start_trace)
│   ├── fixtures/
│   │   ├── presets/              # valid/invalid JSON for importPreset (include edge cases: dup name, 7-limit, band clamp)
│   │   └── locales/              # per-locale string snapshots (13 locales)
│   ├── checklists/
│   │   ├── smoke.md              # manual smoke (mirrors TODO_MANUAL_CHECK.md §6, device-specific steps)
│   │   └── accessibility.md      # 48dp, contentDescription, heightIn audit
│   └── changelogs/
│       ├── _template.md          # template for per-tag notes
│       └── YYYYMMDD-HHMMSS-vX.Y.Z.md  # one per release tag (e.g. 20260904-xyz-v0.1.1.md)
├── .build-outputs/               # APKs (existing, gitignored)
├── logs/                         # build logs (existing, gitignored)
└── app/src/test/screenshots/     # Roborazzi reference images (tracked reference, actual outputs → qc/reports/roborazzi)
```

**Conventions:**
- All `qc/reports/*`, `qc/artifacts/*`, `qc/traces/*` are **gitignored** (see §6); only `qc/changelogs/*.md`, `qc/fixtures/*`, `qc/checklists/*`, and Roborazzi **reference** images are tracked. `qc/artifacts/recordings/` is **ephemeral gitignored** (scrcpy MP4s, see §5.5).
- Roborazzi config: `roborazzi { outputDir = file("qc/reports/roborazzi") }` (add to `app/build.gradle.kts`), reference dir remains `app/src/test/screenshots` for `verifyRoborazziDebug` comparison.
- CI uploads `qc/reports/**` as artifact `qc-reports` and `qc/changelogs/*.md` as `qc-changelogs` (never uploads `qc/artifacts/recordings/`).

---

## 4. Toolchain & Versions

Pinned to `gradle/libs.versions.toml` + `settings.gradle.kts` + `gradle.properties`. No deviation without updating catalog.

| Tool | Version (pinned) | Config location | Purpose |
|---|---|---|---|
| JDK | **21** (Foojay `1.0.0` via `settings.gradle.kts:15`, `gradle.properties:21` toolchain) | `settings.gradle.kts:15`, `app/build.gradle.kts:154` | Compile, `testDebugUnitTest`, lint |
| AGP | **9.1.1** (`libs.versions.toml:2`) | `libs.versions.toml:2` | Build |
| Kotlin | **2.0.21** (`libs.versions.toml:12`) | `libs.versions.toml:12` | Language |
| Compose BOM | **2025.01.00** (`libs.versions.toml:13`) | `libs.versions.toml:13` | UI |
| Robolectric | **4.16.1** (`libs.versions.toml:26`) | `libs.versions.toml:26` | JVM UI tests (`@Config(sdk=[34])`) |
| Roborazzi | **1.59.0** (`libs.versions.toml:27`) | `libs.versions.toml:27`, `app/build.gradle.kts:7` | Screenshot matrix |
| Secrets Gradle Plugin | **2.0.1** (`libs.versions.toml:29`) | `app/build.gradle.kts:168-173` | `.env` → `BuildConfig` |
| Google Services | **4.5.0** (`libs.versions.toml:30`) | `libs.versions.toml:30` | AdMob passthrough |
| Gradle | 9.x wrapper (`gradlew`) | `gradlew`, `gradle.properties:10` | Build driver |
| ADB | **37.0.1** (`/home/milkboy/Android/Sdk/platform-tools/adb`) | host | Device install/dumpsys |
| scrcpy | **4.1** (`/usr/local/bin/scrcpy`, SDL 3.2.10, libavcodec 61.19) — **ephemeral manual only** | host (optional) | Live mirroring + ephemeral `--record` (not CI-blocking; see §5.5) |
| Gitleaks | v8+ (`gitleaks dir --redact`) | `.github/workflows/ci-cd.yml:54-57` | Secret scan |
| actionlint | `reviewdog/action-actionlint@v1` | `.github/workflows/ci-cd.yml:59-60` | Workflow YAML lint |
| Kover | **OUT of v1** — Phase 3 optional only (§11) | — | Coverage (deferred) |

**Add to `scripts/check_requirements.sh`:** check `actionlint` presence (`command -v actionlint`), `gitleaks` presence, `adb --version`, **scrcpy soft check** (`command -v scrcpy && scrcpy --version` → WARN if missing, not FAIL), and `qc/` directory existence.

---

## 5. Execution Workflows

### 5.1 JVM Unit + Compose Tests (blocking)

```bash
./gradlew testDebugUnitTest --info               # 20/20 expected (7 suites: EqualizerPresetManagerTest 8, AdConsentManagerTest 3, FloatingOverlayTest 5, etc.)
# artifacts: app/build/test-results/testDebugUnitTest/, app/build/reports/tests/
# copy to qc: cp -r app/build/reports/tests qc/reports/tests/ && cp -r app/build/test-results qc/reports/tests/
```
- Tests live at `app/src/test/java/com/milkys/soundbooster/*Test.kt` with Robolectric (`@RunWith(RobolectricTestRunner::class)`, `@Config(sdk=[34])`, `GraphicsMode.NATIVE`).
- Must use `collectAsStateWithLifecycle()` (AGENTS §5.1) — lint will enforce.

### 5.2 Roborazzi Visual Matrix (24 combos, blocking on PR/main per Q2)

Mandatory per `AGENTS.md:§3.2 A` — expand `GreetingScreenshotTest.kt:32-35` (single Pixel8) to full matrix. Recommended approach: parameterized test or multiple `@Config(qualifiers=...)` + fontScale + theme.

| Dimension | Values | Qualifiers / config |
|---|---|---|
| Screen width | **320dp** (compact small), **411dp** (standard phone), **600dp+** (tablet), **landscape** | `RobolectricDeviceQualifiers.Pixel8` + custom `w320dp-h640dp`, `w600dp-h900dp`, `w962dp-h541dp-land` |
| Font scale | **0.85x**, **1.0x**, **1.3x**, **2.0x** | `Configuration.fontScale` via `@Config(fontScale=...)` or `ApplicationProvider` override |
| Theme | **Light**, **Dark** | `MyApplicationTheme { }` with `isDarkTheme` toggle |

Total **4 × 4 × 2 = 32** theoretical, **24** after deduplicating compact+2.0x extremes per `AGENTS.md`. Command:

```bash
./gradlew verifyRoborazziDebug --info    # compares qc/reports/roborazzi vs app/src/test/screenshots reference
# on first run: ./gradlew recordRoborazziDebug
```

Threshold: **<5% pixel diff** per image; **0 failures** to pass gate. Store diffs in `qc/reports/roborazzi/diffs/`.

### 5.3 Android Lint & Accessibility (blocking)

```bash
./gradlew lintDebug --info
# report: app/build/reports/lint-results-debug.html → cp to qc/reports/lint/
```

Mandatory rules (`AGENTS.md:§3.2 B`):
- `contentDescription = stringResource(R.string.*)` for interactive icons, `null` for decorative.
- `Modifier.defaultMinSize(48.dp)` for clickables (audit `HearingWarningCard.kt:43` 32dp close button — fix to 48dp).
- No `Modifier.height(...)` on text containers; use `heightIn`/`wrapContent`.
- Target: **0 errors**, warnings triaged (130 warnings currently in `TODO_MANUAL_CHECK.md:13` — `Divider→HorizontalDivider` expected).

### 5.4 Instrumented Tests (manual, on-device)

```bash
./gradlew connectedDebugAndroidTest   # requires device via adb
# or per-device: ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.milkys.soundbooster.ExampleInstrumentedTest
```

Validate `VolumeBoosterService` foreground notification, `TileService` toggle, `SYSTEM_ALERT_WINDOW` bubble drag/snap.

### 5.5 Manual Smoke (per `TODO_MANUAL_CHECK.md:§6` + `qc/checklists/smoke.md`)

Execute on **both** physical devices after `adb install`:

1. Install `adb -s <serial> install -r .build-outputs/app-playstore-debug.apk`, grant `POST_NOTIFICATIONS`, enable booster → notification `Milkys Sound Booster & EQ Active (+XX%)` with `-10%/+10%/OFF` when `isNotifControlsEnabled`.
2. Toggle `Overlay Control` → grant `SYSTEM_ALERT_WINDOW` → bubble draggable, snap to edge, expand to `Booster Overlay` with 4 favorites.
3. EQ presets: `Flat`, `Bass Booster`, save custom `MyPreset` (1-10 chars, max 7), favorite max 4, export/import JSON (`qc/fixtures/presets/`).
4. `HearingWarningCard` dismiss → hidden 7 days (`hideHearingWarningFor7Days` `AudioEffectManager.kt:423`), reappears after clearing prefs.
5. Language switch via Settings → `AppCompatDelegate.setApplicationLocales` + `config.setLocale` — verify 13 locales.
6. Theme toggle → no hardcoded `Color(0xFF...` ) regressions (audit `MainActivity.kt:244` `AppColors` usage).
7. Audio leak: `adb -s <serial> shell dumpsys audio | grep -i enhancer` before/after service stop — 0 leaked handles.

Capture `adb shell screencap` to `qc/artifacts/screenshots/manual/<device>-<step>.png`.

> **Optional ephemeral scrcpy (manual only, not CI):** scrcpy is installed (`scrcpy 4.1` at `/usr/local/bin/scrcpy`) and available for live QA. Use side-by-side mirroring for precise drag checks and bug-repro video — recordings are **ephemeral (gitignored)** under `qc/artifacts/recordings/` or `/tmp/` and are **not** linked in `qc/changelogs/` (only PNGs + Roborazzi diffs are permanent evidence).
> ```bash
> scrcpy -s hm5xr8gueiz5x4c6 --window-title "Redmi-API36" &
> scrcpy -s A1013A5320TH000257 --window-title "ADVAN-API34" &
> # ephemeral record for bug repro only (delete after review — gitignored)
> scrcpy -s <serial> --no-control --record /tmp/qc-<device>-smoke-$(date +%H%M%S).mp4
> # or: scrcpy -s <serial> --record qc/artifacts/recordings/<device>.mp4  # also gitignored
> ```
> Retention: `/tmp/qc-*.mp4` or `qc/artifacts/recordings/*.mp4` until manual delete; CI never stores them.

### 5.6 Performance & Memory (traces)

- **Heap snapshot:** `chrome-devtools_take_heapsnapshot` → `qc/traces/heapsnapshots/<timestamp>.heapsnapshot` (check `AudioEffectManager` `audioScope`/`silenceJob` leaks).
- **Perf trace:** `chrome-devtools_performance_start_trace` (reload) → `qc/traces/perf/trace.json.gz` — analyze `LCP`/`CLS` for Compose.

### 5.7 Manual Display / Font Spot-Check — Redmi Note 8 Pro (6 combos, ephemeral)

**Purpose:** Complement JVM Roborazzi matrix (§5.2, 24 combos headless) with real-device MIUI rendering. Redmi Settings → Display size (controls `wm density`) + Font size (controls `font_scale`) map 1:1 to matrix dimensions but catch MIUI-specific layout bugs (threshold glitches, `SYSTEM_ALERT_WINDOW` bubble clipping, `TileService` labels).

**Current Redmi baseline (probed `adb 37.0.1`):** `font_scale=1.0`, `wm density` Physical 440 / Override 352 → `491dp` width. Small ≈440 (~360dp compact), Large/Largest → ~300dpi (~600dp expanded).

**Spot-check matrix — 6 critical combos (not full 16):**

| # | Display size (MIUI) | `wm density` | Effective sw | Font size | `font_scale` | Covers |
|---|---|---|---|---|---|---|
| 1 | **Default** (baseline) | 352 | 491dp | Default | 1.0x | Baseline (`AGENTS.md:§3.2` standard) |
| 2 | **Small** | 440 | ~360dp | **Largest** | 2.0x | Worst-case compact + largest font (tight wrapping, button stacking) |
| 3 | **Largest** | ~300 | ~600dp | **Small** | 0.85x | Expanded + small font (alignment, max-width bounds) |
| 4 | **Largest** | ~300 | ~600dp | **Large** | 1.3x | Tablet + accessibility (height 240dp EQ fix) |
| 5 | Default | 352 | 491dp | **Large** | 1.3x | Standard enlargement |
| 6 | Small | 440 | ~360dp | **Small** | 0.85x | Compact + compact (touch target min 48dp) |

Add **Light/Dark** toggle on #2 and #4 to cover theme (total 8 screenshots if needed).

**Manual execution (primary — via Settings UI to catch MIUI quirks):**

```bash
# On Redmi: Settings → Display → Display size → slide Small/Default/Large/Largest
#           Settings → Display → Font size → slide Small/Default/Large/Largest
# Check per combo: DashboardScreen scrollability, 5-band sliders visible (height 240dp fix), HearingWarningCard close, no truncation, 48dp touch targets
adb -s hm5xr8gueiz5x4c6 shell screencap -p /sdcard/qc-redmi-$combo.png && adb -s hm5xr8gueiz5x4c6 pull /sdcard/qc-redmi-$combo.png qc/artifacts/screenshots/manual/redmi-$combo.png
# Optional live mirror + ephemeral video (gitignored, see §5.5):
scrcpy -s hm5xr8gueiz5x4c6 --window-title "Redmi-Matrix" &
scrcpy -s hm5xr8gueiz5x4c6 --no-control --record /tmp/qc-redmi-matrix-$(date +%H%M%S).mp4
```

**Automated helper (optional, for regression — not CI):** `bash scripts/qc_redmi_matrix.sh` toggles `wm density 440/352/300` + `font_scale 0.85/1.0/1.3/2.0` via `adb shell wm density` / `settings put system font_scale`, pulls `screencap` per combo to `qc/artifacts/screenshots/manual/`, then restores baseline `wm density 352 && font_scale 1.0`. See `scripts/qc_redmi_matrix.sh:1`.

**Evidence & retention:** PNGs in `qc/artifacts/screenshots/manual/redmi-<display>-<font>.png` are **tracked PNGs? No — gitignored per `.gitignore:30` (`/qc/artifacts/`), only reviewed locally and optionally attached to `qc/changelogs/*.md` as links, not committed.** MP4s ephemeral `/tmp/qc-redmi-matrix-*.mp4` or `qc/artifacts/recordings/redmi-*.mp4` gitignored, delete after review.

**ADVAN tablet note:** ADVAN already covers 601dp expanded baseline — no matrix needed there; just one screencap per baseline for comparison.

---

## 6. `.gitignore` Deltas

Current `.gitignore:1-30` covers `/.build-outputs/`, `/logs/`, `/screenshots/`, `.env`, `.gradle`, `.idea/*`, `debug.keystore`, `assets/logo/`, `assets/icon/`. **Additions:**

```gitignore
# --- QC outputs (generated, do not track) ---
/qc/reports/
/qc/artifacts/
# /qc/artifacts/recordings/ is ephemeral scrcpy MP4s (gitignored, see §5.5)
/qc/artifacts/recordings/
*.mp4
/qc/traces/
*.hprof
*.heapsnapshot
*.trace
*.trace.gz
.roborazzi/
app/build/
.gradle/
captures/
.env.*
*.jks
*.p12
release.keystore

# --- Roborazzi reference vs outputs ---
# Track reference images, ignore generated diffs/outputs (outputs go to qc/reports/roborazzi)
!app/src/test/screenshots/
!qc/changelogs/*.md
!qc/fixtures/**/*
!qc/checklists/*.md

# Keep changelogs tracked (exception to qc/ ignore)
!qc/changelogs/
```

**Migration:** Existing `/screenshots/` (root) → deprecated, move reference images to `app/src/test/screenshots/` (tracked), generated outputs to `qc/reports/roborazzi/` (ignored). Existing `screenshots-bug/` remains ignored.

Apply via edit to `/.gitignore` after line 30.

---

## 7. Rules / Skill & `AGENTS.md` Optimizations

### 7.1 `AGENTS.md` edits (242 lines)

- **§3.1 Standard Verification:** Add `./gradlew verifyRoborazziDebug` alongside `testDebugUnitTest` + `lintDebug`. Clarify output dirs `qc/reports/*`.
- **New §3.4 QA/QC Folder Convention:** Insert after §3.3, describe `qc/` tree (§3), reference `qc_plan.md` as source of truth, list `qc/changelogs/` linkage to `CHANGELOG.md`, mandate `collectAsStateWithLifecycle` + `roborazzi` config.
- **§5.2 Safety Boundaries:** Sync wording to 200% (+15 dB / 1500 mB) matching `AudioEffectManager.kt:281` comment.
- **§7 Documentation:** Add bullet `📋 qc_plan.md — QA & QC master plan (qc/ canon, device matrix, gates, changelogs)` + `📝 CHANGELOG.md + qc/changelogs/`.
- **§2 Build Commands:** Add `bash scripts/generate_screenshots.sh` for Play Console (if exists) and `adb` smoke reference.

### 7.2 `.jules/config.yaml` + `.jules/rules.md` edits

`config.yaml:1-29` — add:
```yaml
verification:
  roborazzi_command: "./gradlew verifyRoborazziDebug"
  coverage_command: null  # Phase 3 optional, see §11
artifacts:
  qc_path: "qc/"
  reports_path: "qc/reports/"
  changelogs_path: "qc/changelogs/"
protected_paths:
  - "qc/changelogs/*"   # do not auto-clean
```

`rules.md:26-40` — add rule under §3:
```
4. Run ./gradlew verifyRoborazziDebug and ensure 0 failures before PR.
5. All qc/reports artifacts must be generated and linked in PR description.
```

### 7.3 `.gemini/rules/` → `.opencode/` migration (via `customize-opencode` skill)

Repo currently has `.gemini/rules/model_delegation.md` + `rules.md`, no `opencode.json` / `.opencode/` (probed: `opencode.json: not found`, `.opencode: no such file`). **Plan:**

- Create `opencode.json` at root (port `model_delegation.md:81` portability rule).
- Create `.opencode/skills/qa-automation/SKILL.md` (port shortcuts `/quick`, `/standard`, `/hard-fix` to opencode agents).
- Create `.opencode/skills/qc-device-farm/SKILL.md` — auto `adb devices -l` → matrix table generation (uses probes from §2).
- Keep `.gemini/` for backward compat until migration complete; new skill `customize-opencode` governs edits to `opencode.json`.
- Replace `scripts/setup_project_rules.sh` delegation with `scripts/setup_opencode_rules.sh` wrapper (preserves zero-delay shortcuts, maps to opencode agent specs).

All `.opencode/` edits must be done via `skill customize-opencode` (per system prompt).

---

## 8. CI/CD Integration (`.github/workflows/ci-cd.yml:1-164`)

Current `verify` job (`ci-cd.yml:23-61`) runs `testDebugUnitTest` + `lintDebug` + `gitleaks` + `actionlint` on `push: main,staging` + `pull_request: main,staging`. `build-and-release` on `workflow_dispatch` only.

**Recommended delta (Q2 best practice — blocking on PR/main, soft on staging):**

```yaml
jobs:
  verify:
    name: Verify (blocking on PR/main)
    if: github.event_name == 'pull_request' || github.ref == 'refs/heads/main'
    # runs: check_requirements, testDebugUnitTest, lintDebug, verifyRoborazziDebug (NEW, blocking), gitleaks, actionlint
    steps:
      - name: Run Roborazzi Matrix
        run: ./gradlew verifyRoborazziDebug
      - name: Upload QC Reports
        uses: actions/upload-artifact@v4
        with: { name: qc-reports, path: qc/reports/ }

  verify-staging:
    name: Verify Staging (soft, allow failure)
    if: github.ref == 'refs/heads/staging'
    continue-on-error: true
    # same steps but continue-on-error, no gate
```

- Add `roborazzi` setup (no extra SDK needed, JVM only).
- Upload `qc/reports/**` + `app/build/reports/**` as artifacts.
- `build-and-release:65-133` — add `cp -r qc/changelogs .build-outputs/` to release files.
- Keep `jules-fix:138-164` auto-fix on `verify` failure (not on `verify-staging` soft fail).

Version in this plan aligns with `AGENTS.md:§3.3 B` workflow rules (runner `ubuntu-latest`, pinned `actions/checkout@v4`, `setup-java@v4`, `setup-gradle@v4`).

---

## 9. Reporting & Quality Gates (v1 — no Kover)

### 9.1 Gate definitions (blocking)

| Gate | Command | Threshold | Artifact | Blocking on |
|---|---|---|---|---|
| Unit tests | `./gradlew testDebugUnitTest` | **100% pass** (20/20, 7 suites) | `qc/reports/tests/` | PR/main |
| Lint | `./gradlew lintDebug` | **0 errors** (warnings triaged, no `ContentDescription`/`ClickableViewAccessibility`) | `qc/reports/lint/lint-results-debug.html` | PR/main |
| Roborazzi | `./gradlew verifyRoborazziDebug` | **0 failures**, **<5% diff** per image | `qc/reports/roborazzi/` | PR/main |
| Secrets | `gitleaks dir --redact` | **0 leaks** | `qc/reports/gitleaks/report.json` | PR/main |
| Workflow | `actionlint .github/workflows/*.yml` | **0 errors** | stdout | PR/main |

No coverage gate in v1 (see §11). `staging` soft gate: same commands, `continue-on-error: true`, report only.

### 9.2 Manual gates (checklist in `qc/checklists/smoke.md`)

- All 7 smoke steps (§5.5) pass on **both** physical devices + one emulator (API 24).
- No leaked `LoudnessEnhancer`/`Equalizer` handles after `onDestroy` (dumpsys).
- No hardcoded color/strings regression (grep `Color(0xFF` in `MainActivity.kt`).

### 9.3 Release gate

- `CHANGELOG.md` updated (see §10), `qc/changelogs/YYYYMMDD-HHMMSS-vX.Y.Z.md` exists and linked.
- APKs in `.build-outputs/` copied to `qc/artifacts/apks/` and uploaded as `android-release-outputs` (`ci-cd.yml:104-108`).

---

## 10. Changelogs & Versioning (added per your request)

No `CHANGELOG.md` exists yet (probed `CHANGELOG*: not found`). **Create both:**

### 10.1 `CHANGELOG.md` at workspace root (Keep-a-Changelog format)

- Source: https://keepachangelog.com/ — sections `Added`, `Changed`, `Fixed`, `Security` per version.
- Versioning: `VERSION_CODE`/`VERSION_NAME` from `.env:33-35` (currently `26072401` / `0.1`), bumped by `scripts/bump_version.py` on every `scripts/build.sh:20-21` run. `CHANGELOG.md` entry per bump.
- Template:

```markdown
# Changelog

All notable changes to Milkys Sound Booster & EQ will be documented in this file.

## [Unreleased]
### Added
- ...

## [0.1.1] - 2026-09-04
### Changed
- ...
### Fixed
- ...
### Security
- ...
```

### 10.2 `qc/changelogs/` per-tag verbose notes

- One markdown per release tag (tag generated `ci-cd.yml:110-115` `v0.1-build.YYYYMMDD-HHMMSS-<sha>`).
- Named `qc/changelogs/YYYYMMDD-HHMMSS-vX.Y.Z.md` (e.g., `20260904-143000-v0.1.1.md`).
- Contents: changelog excerpt, APK list (`qc/artifacts/apks/`), lint/test/roborazzi report links (`qc/reports/`), device matrix result (pass/fail per device), known issues.
- CI `Generate Tag Name` → write `qc/changelogs/<tag>.md` from `qc/changelogs/_template.md`, then `CHANGELOG.md` append via script.
- Template `qc/changelogs/_template.md`:

```markdown
# Release vX.Y.Z — YYYY-MM-DD

## Summary
<!-- one-line summary -->

## Artifacts
- APK: `qc/artifacts/apks/app-playstore-debug.apk`
- Reports: `qc/reports/tests/`, `qc/reports/lint/`, `qc/reports/roborazzi/`

## QA Results
| Device | Android | Result | Notes |
|---|---|---|---|
| Redmi Note 8 Pro | 16 | PASS/FAIL | |
| ADVAN TAB A10 | 14 | PASS/FAIL | |

## Changelog excerpt
<!-- copy from CHANGELOG.md ## [X.Y.Z] -->
```

- **Linkage:** Every `qc/changelogs/*.md` must reference its `CHANGELOG.md` section, and `CHANGELOG.md` links to `qc/changelogs/<tag>.md` for full QC evidence.

---

## 11. Timeline & Phases

| Phase | Scope | Effort | Exit criteria |
|---|---|---|---|
| **P0 — Scaffold** | Create `qc/` dirs (§3), `qc_plan.md` (this file), `CHANGELOG.md` + `qc/changelogs/_template.md`, update `.gitignore` (§6) | 1h | `ls qc/` shows 6 subdirs, `cat CHANGELOG.md` exists, `git status` clean except new files |
| **P1 — Rules & CI** | Patch `AGENTS.md:§3.4`, `.jules/config.yaml`, create `opencode.json` + skills (§7), extend `ci-cd.yml` with `verifyRoborazziDebug` blocking/soft split (§8) | 2h | `./scripts/check_requirements.sh` PASS, `actionlint` 0 errors, CI dry-run green |
| **P2 — Matrix & Reports** | Expand `GreetingScreenshotTest.kt` to 24-combo matrix (or new `QcMatrixScreenshotTest.kt`), add `roborazzi { outputDir }` to `app/build.gradle.kts`, implement `qc/checklists/smoke.md` + `accessibility.md`, `qc/fixtures/presets/` | 3h | `./gradlew verifyRoborazziDebug` PASS (or record baseline), `qc/reports/roborazzi/` populated |
| **P3 — Device Run** | `scripts/build.sh assembleDebug` → `adb -s <serial> install` both devices (§5.5), capture `qc/artifacts/screenshots/manual/`, `heapsnapshot`/`perf` traces (§5.6), write first `qc/changelogs/YYYYMMDD-v0.1.1.md` | 2h | Manual checklist all PASS, traces in `qc/traces/`, release tag with `CHANGELOG.md` updated |

### Phase 3 — Optional Coverage (Kover) — deferred per your choice

**Not in v1 — no gate.** Reserved for after `AudioEffectManager.kt:24` DI refactor (`object` → `PreferencesRepository` + `BoosterViewModel`/`EqViewModel` per `TODO_MANUAL_CHECK.md:96-102`).

When ready:
- Add `id("org.jetbrains.kotlinx.kover") version "0.8.+"` to `app/build.gradle.kts:3` plugins + `kover { }` config.
- Output to `qc/reports/coverage/` (gitignored, like other reports).
- Gate **soft only**: `report` mode, threshold **40% initial** → raise to **60-70%** after ViewModels stabilize. Never blocking in Phase 3 initial — `continue-on-error: true` in CI.
- This section documents intent without implementing — prevents premature PR blocking (current coverage likely 15-30% on `MainActivity.kt` 3800 LOC singleton).

---

## 12. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Gain exceeds 200%/1500mB, hearing damage | Low (guard `mapProgressToGain` `AudioEffectManager.kt:283`) | High | Gate: grep `1500` in CI, manual slider 100% check, warning banner e2e |
| `AudioEffect` leak (system audio server crash) | Medium | High | `release()` audit, `dumpsys audio` after smoke, heapsnapshot in `qc/traces/heapsnapshots/` |
| Overlay permission denied on MIUI (Redmi) | Medium | Medium | Smoke step 2 on Redmi specifically, fallback UX check |
| Battery optimization kills service | High | Medium | `isBatteryOptimized` check `AudioEffectManager.kt:220-226`, `SystemBatteryDiagnosticCard` e2e |
| Roborazzi flakiness (fontScale/theme) | Medium | Low | Pin `sdk 34`, `GraphicsMode.NATIVE`, threshold 5%, store reference in `app/src/test/screenshots/` |
| Secret leak via `.env` | Low | High | Gitleaks blocking gate, `.gitignore` `.env.*`, `protected_paths` in `config.yaml:26` |
| Changelog drift (forgot to update) | High | Low | CI check: `git diff --name-only | grep -q CHANGELOG.md` on tag builds, template in `qc/changelogs/_template.md` |

---

## Appendix A: Commands Cheatsheet

```bash
# --- Environment ---
cp .env.example .env && chmod +x scripts/*.sh
./scripts/check_requirements.sh
adb devices -l && adb --version

# --- Build ---
bash scripts/build.sh assembleDebug          # → .build-outputs/ + logs/
./gradlew assembleDebug
./gradlew testDebugUnitTest --info           # → qc/reports/tests/
./gradlew lintDebug --info                   # → qc/reports/lint/
./gradlew verifyRoborazziDebug --info        # → qc/reports/roborazzi/
./gradlew recordRoborazziDebug               # baseline first run
gitleaks dir --redact --report-path qc/reports/gitleaks/report.json
actionlint .github/workflows/*.yml

# --- Device ---
adb -s hm5xr8gueiz5x4c6 install -r .build-outputs/app-playstore-debug.apk
adb -s A1013A5320TH000257 install -r .build-outputs/app-playstore-debug.apk
adb -s <serial> shell pm list packages | grep milkys
adb -s <serial> shell dumpsys audio | head -n 50
adb -s <serial> shell screencap -p /sdcard/qc.png && adb -s <serial> pull /sdcard/qc.png qc/artifacts/screenshots/manual/
adb -s <serial> shell dumpsys meminfo com.milkys.soundbooster
adb -s <serial> logcat -d | grep -i AudioEffectManager

# --- Changelog ---
# bump is automatic via scripts/bump_version.py on build.sh
cat CHANGELOG.md
ls qc/changelogs/
```

## Appendix B: Changelog Template (`qc/changelogs/_template.md`)

See §10.2 — copy to new tag file on release:

```markdown
# Release vX.Y.Z — YYYY-MM-DD

## Summary
<!-- one-line summary of release intent -->

## Artifacts
- APK: `qc/artifacts/apks/app-playstore-debug.apk`
- Reports: `qc/reports/tests/`, `qc/reports/lint/`, `qc/reports/roborazzi/`

## QA Results
| Device | Android | Result | Notes |
|---|---|---|---|
| Redmi Note 8 Pro (hm5xr8gueiz5x4c6) | 16 | PASS/FAIL | |
| ADVAN TAB A10 (A1013A5320TH000257) | 14 | PASS/FAIL | |

## Changelog excerpt
<!-- copy from CHANGELOG.md ## [X.Y.Z] -->

## Known issues
<!-- list or "None" -->
```

---

> **Maintained by:** QC plan v1, 2026-09-04. Update this file when device fleet, toolchain (`libs.versions.toml`), or gates change. Keep `qc/` evidence per build; do not delete `qc/changelogs/` history.
