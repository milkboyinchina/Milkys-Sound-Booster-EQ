# 🤖 AGENTS.md — Repository Instructions for Autonomous Coding Agents (Google Jules)

Welcome to **Milkys Sound Booster & EQ**! This document provides context, setup instructions, architecture principles, build commands, and coding guidelines for **Google Jules** and other AI agents operating within this repository.

---

## 📌 1. Project Overview & Tech Stack

**Milkys Sound Booster & EQ** is a high-fidelity global audio booster and 5-band graphic equalizer application for Android.

* **Target Operating System**: Android 7.0+ (Min SDK `24`, Target SDK `36`)
* **Primary Language**: Kotlin 2.0+
* **UI Framework**: Jetpack Compose with Material 3 Design System
* **Build System**: Gradle 8.5+ with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`)
* **Java Runtime**: JDK 21 (JDK 17 supported)
* **Audio DSP Engine**: Android `AudioEffect`, `Equalizer`, `LoudnessEnhancer`, `PresetReverb`
* **Background Processing**: Android Foreground Service, Quick Settings Tile (`TileService`), System Floating Overlay Widget (`WindowManager`)
* **Database & Persistence**: AndroidX Room Database & Encrypted Key-Value Storage
* **Monetization & Ads**: Google Mobile Ads SDK (AdMob) with `.env` build toggles

---

## ⚡ 2. Quick Environment Setup & Build Commands

Before compiling or modifying code, initialize environment configuration and verify machine prerequisites.

### Step 1: Environment Initialization
```bash
# Copy baseline environment properties if missing
cp .env.example .env

# Make custom automation scripts executable
chmod +x scripts/*.sh
```

### Step 2: Prerequisites Verification
```bash
bash scripts/check_requirements.sh
```

### Step 3: Standard Build Commands
* **Debug APK Compilation**:
  ```bash
  bash scripts/build.sh assembleDebug
  # Output APK saved to: .build-outputs/
  ```
* **Release APK Compilation**:
  ```bash
  bash scripts/build.sh assembleRelease
  ```
* **Direct Gradle Build**:
  ```bash
  ./gradlew assembleDebug
  ```

---

## 🧪 3. Testing & Code Quality Verification

All AI agents **must run automated verification** after making code changes before declaring tasks complete.

### 3.1 Standard Verification Commands
* **Run Unit & JVM UI Tests:**
  ```bash
  ./gradlew testDebugUnitTest
  # reports → qc/reports/tests/ (JUnit XML + html)
  ```
* **Run Roborazzi Visual Matrix (24 combos, blocking on PR/main):**
  ```bash
  ./gradlew verifyRoborazziDebug
  # outputs → qc/reports/roborazzi/ (reference: app/src/test/screenshots/)
  # baseline: ./gradlew recordRoborazziDebug
  ```



* **Run Android Lint & Static Analysis:**
```bash
./gradlew lintDebug

```


* **Run Environment Pre-checks:**
```bash
./scripts/check_requirements.sh
# also checks scrcpy 4.1 (ephemeral manual QA, soft WARN if missing)
```

* **Optional Manual QA (ephemeral scrcpy 4.1, not CI-blocking):**
```bash
scrcpy -s hm5xr8gueiz5x4c6 --window-title "Redmi-API36" &
scrcpy -s A1013A5320TH000257 --window-title "ADVAN-API34" &
# ephemeral record (gitignored: qc/artifacts/recordings/*.mp4 or /tmp/qc-*.mp4)
scrcpy -s <serial> --no-control --record /tmp/qc-<device>-$(date +%H%M%S).mp4
```

---

### 3.2 Detailed UI & Accessibility Testing Guidelines

When creating or modifying Jetpack Compose screens, agents must ensure visual integrity, state handling, and accessibility compliance.

#### 📸 A. Layout Overflow, Font Scaling & Clipping (Visual Screenshot Testing)

* **Objective:** Prevent UI clipping, text truncation, broken wraps, and view overlaps across diverse display sizes and accessibility scale settings.
* **Execution Task:**
```bash
./gradlew verifyRoborazziDebug

```


* **Mandatory Test Matrix Variations:** Every major UI screen or critical component screenshot test must evaluate rendering across a matrix of screen dimensions, orientations, font scales, and theme modes:
1. **Multi-Screen Dimensions & Orientations:**
* **Compact / Small Phone (`320dp` width):** Check for tight vertical constraints, extreme text wrapping, and button stacking issues.
* **Standard Phone (`360dp` – `411dp` width):** Baseline standard display dimensions.
* **Large / Expanded / Tablet (`600dp+` width):** Verify layout responsiveness, max-width container bounds, and proper alignment.
* **Landscape Mode:** Ensure vertical scrollability works and fixed-height components do not clip off-screen.


2. **Multiple Font Scaling Levels:**
* **Small / Compact Font (`0.85x`):** Ensure text alignment and minimum touch targets remain intact.
* **Standard Font (`1.0x`):** Baseline UI text layout metrics.
* **Large Font (`1.3x`):** Standard user accessibility enlargement.
* **Maximum / Accessibility Font (`1.5x` – `2.0x`):** Extreme font scale to test multi-line text wrapping, container height expansion, and lack of text truncation (`Ellipsis`) bugs.


3. **Theme Configurations:**
* Both **Light Mode** and **Dark Mode** render passes using standard `MaterialTheme` tokens.





#### ♿ B. Accessibility & Improper UI Attributes (Linting & Rules)

* **Objective:** Guarantee full accessibility support and compliance with Material Design guidelines.
* **Execution Task:** Run `./gradlew lintDebug` and resolve all reported UI/accessibility warnings.
* **Mandatory Rules:**
* **Content Descriptions:** Interactive images/icons must include explicit `contentDescription = stringResource(...)`. Decorative images must explicitly set `contentDescription = null`.
* **Touch Targets:** All clickable UI elements must adhere to the minimum **48x48dp** touch target recommendation (`Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)`).
* **No Fixed Heights on Text Containers:** Avoid using hardcoded `Modifier.height(...)` on text containers; use dynamic wrapping or `Modifier.heightIn(...)` so scaled fonts do not get clipped.



#### 🧪 C. Functional UI Interaction & State Tests

* **Objective:** Validate component rendering and state transitions on JVM without needing an emulator.
* **Execution Task:** Place JVM Compose tests in `app/src/test/` using `createComposeRule()` + **Robolectric**:
* **State Coverage:** Test that screens correctly render **Loading**, **Success**, **Empty**, and **Error** states when emitted by ViewModel `StateFlow`.
* **Event Handling:** Assert that user clicks (e.g., equalizer toggles, gain sliders) trigger expected ViewModel callbacks and update the UI accordingly.
* **Equalizer 5-Band `+/-` (always tunable, Q1):** Must test `EqualizerComponent` `IconButton 48dp` `+`/`-` `performClick` → `Text +1dB` + `_eqBands` Flow + `equalizer?.setBandLevel` (booster OFF and ON, `Flat`→`Custom` and `Custom` presets, both `hm5xr8gueiz5x4c6` + `A1013A5320TH000257`). EQ is always tunable — `enabled = level <15 / >-15` (not `isBoostEnabled`), see `MainActivity.kt:3088` + `AudioEffectManager.kt:464`.

#### 📱 D. Device Prep for Manual QA (Redmi hm5xr8gueiz5x4c6 + ADVAN A1013A5320TH000257)

* **Objective:** Prevent `Allow notifications` + `Onboarding GET STARTED` overlays from blocking `uiautomator dump`/`screencap`/`input swipe` after `pm clear`/`install -r`.
* **Execution Task (before any dump/screencap/swipe/click after clear/reinstall):**
  ```bash
  bash scripts/device_prep.sh hm5xr8gueiz5x4c6 .build-outputs/app-playstore-debug.apk
  bash scripts/device_prep.sh A1013A5320TH000257 .build-outputs/app-playstore-debug.apk
  # Q17 silent pm grant POST_NOTIFICATIONS + Q18 run-as has_seen_onboarding=true (fallback tap GET STARTED if run-as fails)
  # Verifies: dumpsys window mCurrentFocus mFocusedApp mAwake true, has_seen_onboarding true
  ```
* **Manual alternative:** `adb -s hm5xr8gueiz5x4c6 shell pm grant com.milkys.soundbooster android.permission.POST_NOTIFICATIONS && adb shell "run-as ... has_seen_onboarding true"` then `force-stop + am start` + `uiautomator dump` should show `SPONSORED AD` `Adaptive Equalizer Boost` without `Allow`/`GET STARTED`.
* **CI (JVM) alternative:** `testDebugUnitTest`/`verifyRoborazziDebug` headless — set fake `PreferencesRepository` `hasSeenOnboarding=true`, no system dialog.

### 3.3 QA/QC Folder Convention (`qc/` canon)
* **Master plan:** `qc_plan.md` (workspace root) is source of truth for device matrix, gates, and changelogs.
* **Evidence root:** `qc/` — `qc/reports/{lint,tests,roborazzi,gitleaks}`, `qc/artifacts/{apks,screenshots/manual}`, `qc/traces/{heapsnapshots,perf}`, `qc/fixtures/{presets,locales}`, `qc/checklists/{smoke.md,accessibility.md}`, `qc/changelogs/`.
* **Human summary (singleton, no datestamp):** `qc/QC_SUMMARY.md` — single rolling file with `Next Actions` (agent queue, ONLY OPEN) vs `Run YYYY-MM-DD` (log) + `Bug Status OPEN/FIXED` + pointers to reports. Read top `Next Actions` + `Bug Status` before any QC task; `Run` sections are log, not queue (loop prevention, see `.gemini/rules/model_delegation.md:§5`). History via `git log --follow qc/QC_SUMMARY.md`.
* **Reports are gitignored** (`qc/reports/`, `qc/artifacts/`, `qc/traces/`); `qc/changelogs/*.md`, `qc/fixtures/*`, `qc/checklists/*.md`, `qc/QC_SUMMARY.md` and Roborazzi reference images `app/src/test/screenshots/` are tracked.
* **Ephemeral recordings:** `qc/artifacts/recordings/*.mp4` (scrcpy 4.1 `--record`, gitignored) and `/tmp/qc-*.mp4` are ephemeral manual QA only, never CI-uploaded (see `qc_plan.md` §5.5).
* **Changelogs:** `CHANGELOG.md` (root, Keep-a-Changelog) + per-tag `qc/changelogs/YYYYMMDD-HHMMSS-vX.Y.Z.md` linked from `CHANGELOG.md` (see `qc_plan.md` §10).
* **Coverage:** Phase 3 — Optional (Kover soft report-only, not blocking in v1; see `qc_plan.md` §11).

### 3.4 Privacy & CI/CD Pipeline Verification

#### 🔒 A. Personal Identifiable Information (PII) & Secret Sanity Check
* **Objective:** Prevent committing sensitive credentials, private keys, developer paths, personal email addresses, or personal names into source code or test fixtures.
* **Execution Task:**
  ```bash
  # Scan for hardcoded credentials and secrets
  gitleaks dir --redact
Mandatory Privacy Rules:

Emails & Names: Never hardcode personal developer emails or real user names in Kotlin code, comments, or UI previews. Replace them with standard placeholders (e.g., user@example.com, "Jane Doe").

Hardcoded Credentials & Tokens: API keys, AdMob app IDs, sign-in tokens, or passwords must never be committed in plain text. Always extract them into .env or Gradle local.properties mapped to BuildConfig.

Local File Paths: Ensure local machine absolute paths (e.g., /Users/username/... or C:\Users\...) are not hardcoded in build scripts or unit tests. Use relative paths or ${projectDir} / System.getenv("HOME").

⚙️ B. GitHub Actions Workflow Configuration Checks
Objective: Ensure all CI/CD pipeline definitions under .github/workflows/ are syntactically correct, use supported runner environments, and follow security best practices.

Execution Task:

Bash
```
# Validate GitHub Actions YAML syntax and security
actionlint .github/workflows/*.yml
```

Mandatory Workflow Rules:

Expression Security: Do not inject untrusted contexts directly into run: scripts (e.g., run: echo "${{ github.event.pull_request.title }}"). Always pass GitHub expressions via step env: variables to prevent shell script injection.

Runner Labels: Use valid, supported runner labels (e.g., ubuntu-latest, macos-latest).

Action Version Pinning: Ensure third-party GitHub Actions are pinned to explicit release tags or full commit SHA hashes rather than mutable @main or @master branches.

Secret References: Verify that referenced secrets (e.g., ${{ secrets.SIGNING_KEY }}) match documented repository secret names.

---

## 📁 4. Key Repository Architecture & Mapping

```
Milkys-Sound-Booster-EQ/
├── AGENTS.md                   # This instruction file for Jules & AI agents
├── qc_plan.md                  # QA & QC master plan (qc/ canon, device matrix, gates, changelogs)
├── CHANGELOG.md                # Keep-a-Changelog (root) + qc/changelogs/ per tag
├── .jules/                     # Jules agent task execution configuration & rules
│   ├── config.yaml
│   └── rules.md
├── qc/                         # QA & QC evidence root
│   ├── QC_SUMMARY.md            # human-readable rolling summary (singleton, no datestamp — queue vs log)
│   ├── reports/{lint,tests,roborazzi,gitleaks}/
│   ├── artifacts/{apks,screenshots/manual,recordings/}  # recordings = ephemeral scrcpy MP4s (gitignored)
│   ├── traces/{heapsnapshots,perf}/
│   ├── fixtures/{presets,locales}/
│   ├── checklists/{smoke.md,accessibility.md}/
│   └── changelogs/             # per-tag release notes
├── app/                        # Android application module
│   ├── src/main/
│   │   ├── java/com/milkys/soundbooster/
│   │   │   ├── MainActivity.kt            # Dashboard + 3 window-size groups (COMPACT/MEDIUM/EXPANDED)
│   │   │   ├── AudioEffectManager.kt      # DSP engine (Equalizer, LoudnessEnhancer, AudioTrack silence)
│   │   │   ├── VolumeBoosterService.kt    # Foreground service (mediaPlayback) + overlay WindowManager
│   │   │   ├── VolumeBoosterTileService.kt# Quick Settings Tile
│   │   │   ├── AdConsentManager.kt        # UMP consent (reflection)
│   │   │   ├── data/PreferencesRepository.kt # DataStore 1.1.7 (15 keys, migrate)
│   │   │   └── ui/                        # Compose: components/HearingWarningCard, theme/AppColors
│   │   ├── res/                # XML layouts, drawables, localized string resources (res/values-*/)
│   │   └── AndroidManifest.xml # System permissions, service declarations
│   └── build.gradle.kts
├── scripts/                    # Automation scripts for build, environment checks, assets
│   ├── setup_jules_env.sh      # Automated VM setup script for Jules
│   ├── build.sh                # Main Linux/macOS build script
│   └── check_requirements.sh  # System requirements validation script
├── howto/                      # Detailed technical documentation guides
└── .github/workflows/          # CI/CD and Jules GitHub Action automation workflows
```

---

## 🎨 5. Coding Standards & Conventions

Agents must follow these strict coding practices:

### 1. Kotlin & Jetpack Compose Rules
* **UI Components**: Build UI using Jetpack Compose components. Avoid legacy XML views unless necessary for system overlay compatibility.
* **State Management**: Use `StateFlow` and Compose `collectAsStateWithLifecycle()`. Never mutate state directly on main threads.
* **Strings & Localization**: **DO NOT hardcode UI text strings** in Kotlin code. All user-facing strings must be defined in `app/src/main/res/values/strings.xml` to support the 13 supported languages.
* **Theme Tokens**: Use Material 3 theme attributes (`MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`).

### 2. Audio DSP & Safety Boundaries
* **Safety Warning Banner**: Always maintain the prominent safety warning banner regarding hearing protection and speaker damage risks.
* **Volume Amplification Limit**: Boost level is strictly capped at **200% (+15dB)** — `AudioEffectManager.kt:281` `mapProgressToGain` 1500 mB. Do not bypass or remove gain ceiling limits.
* **Resource Cleanup**: Always release `AudioEffect`, `Equalizer`, and `LoudnessEnhancer` handles inside `onDestroy()` or `onCleared()` to prevent audio service memory leaks and system audio server crashes.

### 3. Threading & Asynchronous Operations
* **Non-blocking Main Thread**: Never perform blocking IO, Room database calls, or audio session initializations on the Android Main (UI) thread. Always dispatch to `Dispatchers.IO` or `Dispatchers.Default`.

---

## ⚡ 6. AI Model Delegation & Zero-Delay Shortcuts

When executing tasks or routing work via slash commands, reference these model tiers:

| Shortcut | Target Agent / Model Tier | Primary Tasks |
|---|---|---|
| `/quick <task>` | `@quick-task` (`flash_lite`) | Git operations (`git commit`, `push`), minor file edits, greps, status checks. |
| `/standard <task>` | `@standard-dev` (`flash`) | Compose UI screens, unit tests, bug fixes, build analysis. |
| `/hard-fix <task>` | `@complex-architect` (`pro`) | Audio DSP engine refactoring, architecture overhauls, memory leak debugging. |

### Global Question Labeling (Q# — Mandatory per A16)

* **Per-message reset (Q1-A):** Every assistant turn that asks questions MUST label them `Q1`, `Q2`, `Q3` in order starting at `1` for that message. Do not carry numbers across turns.
* **Multi-choice only (Q2-B):** Only questions with multiple choice answers get sub-labels `Q1-A`, `Q1-B`, `Q1-C` under that `Q#`. Single yes/no confirms stay as `Q1`/`Q2` without `Q1-A` sub-labels.
* **Example:**
  ```
  Q1: Should we keep COMPACT portrait lock or switch to UNSPECIFIED for Android 16?
    Q1-A: Keep PORTRAIT for COMPACT <600dp
    Q1-B: Switch all to UNSPECIFIED (recommended)
  Q2: Confirm PRIVACY_POLICY_URL https vs http?
  ```

---

## 📑 7. Documentation Guidance

For deeper feature specifications, review:
* 🛠️ `howto/setup_develop_build.md`: Comprehensive setup and build guide.
* 📱 `howto/general_information.md`: Architecture overview and system design.
* 📁 `howto/file_function_mapping.md`: Mapping of files to application capabilities.
* 📋 `qc_plan.md`: QA & QC master plan (qc/ canon, device matrix, gates, changelogs).
* 📝 `CHANGELOG.md` + `qc/changelogs/`: Release notes per version/tag.
