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



* **Run Android Lint & Static Analysis:**
```bash
./gradlew lintDebug

```


* **Run Environment Pre-checks:**
```bash
./scripts/check_requirements.sh

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



```

```
---

## 📁 4. Key Repository Architecture & Mapping

```
Milkys-Sound-Booster-EQ/
├── AGENTS.md                   # This instruction file for Jules & AI agents
├── .jules/                     # Jules agent task execution configuration & rules
│   ├── config.yaml
│   └── rules.md
├── app/                        # Android application module
│   ├── src/main/
│   │   ├── java/com/milkys/soundbooster/
│   │   │   ├── audio/          # DSP engine, Equalizer, LoudnessEnhancer wrappers
│   │   │   ├── service/        # Audio processing foreground service, Tile service, Overlay
│   │   │   ├── ui/             # Jetpack Compose screens, components, viewmodels, theme
│   │   │   └── data/           # Room DB entities, DAOs, repositories, preferences
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
* **Volume Amplification Limit**: Boost level is strictly capped at **200% (+15dB to +20dB)**. Do not bypass or remove gain ceiling limits.
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

---

## 📑 7. Documentation Guidance

For deeper feature specifications, review:
* 🛠️ `howto/setup_develop_build.md`: Comprehensive setup and build guide.
* 📱 `howto/general_information.md`: Architecture overview and system design.
* 📁 `howto/file_function_mapping.md`: Mapping of files to application capabilities.
