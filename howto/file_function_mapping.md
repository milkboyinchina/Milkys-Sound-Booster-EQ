# 📁 File & Directory Function Mapping - Milkys Sound Booster & EQ

This guide details the exact mapping between files/directories in this repository and their corresponding application functions, UI components, background services, build scripts, and system features.

---

## 🧭 Directory Overview

```
/
├── app/                        # Main Android application module code & resources
├── assets/                     # Media & branding assets
├── howto/                      # Technical documentation & guides
├── logs/                       # Generated timestamped build logs
├── .build-outputs/             # Output folder for compiled APK files
└── scripts/                    # Cross-platform environment checks & build scripts
```

---

## 📄 File to Function Mapping

### 1. Root & Configuration Files

| File / Path | Corresponding Function & Feature |
| :--- | :--- |
| `/.env` | Runtime & build configuration file storing versioning parameters (`VERSION_CODE`, `VERSION_NAME`), target build output/log directories (`BUILD_OUTPUT_DIR`, `BUILD_LOGS_DIR`), and AdMob API parameters (`GOOGLE_ADS_API_KEY`, `INCLUDE_GOOGLE_ADS`). |
| `/.env.example` | Template configuration file copied automatically during builds if `.env` is absent. |
| `/build.gradle.kts` | Root Gradle build configuration file defining buildscript plugins and repositories. |
| `/settings.gradle.kts` | Project structure and repository resolution settings (e.g., Google Maven, MavenCentral). |
| `/metadata.json` | Google AI Studio platform metadata specifying the app name ("Milkys App"), description, and platform capabilities. |
| `/README.md` | Primary project documentation containing feature lists, quick start commands, build script guides, and links to `howto/` documentation. |

---

### 2. Application Source Code (`app/src/main/java/com/example/`)

| File / Path | Corresponding Function & Feature |
| :--- | :--- |
| **`MainActivity.kt`** | **Primary UI & User Interaction Layer (Jetpack Compose)**:<ul><li>**Top Header Bar**: Title display ("MILKYS APP"), dark/light theme switcher, settings modal trigger, and floating bubble toggle button.</li><li>**Master Boost Dial**: Power toggle button, gain percentage slider (+0% to +200%), and glowing ring status indicator.</li><li>**Sound Test Button**: Synthesizes and plays a 3-second multi-frequency melody (`playSoundTest3Sec`) directly next to the master power button.</li><li>**Hearing & Speaker Damage Warning Banner**: Prominent high-contrast alert card highlighting hearing loss and speaker damage risks (dismissable with 7-day remind timer).</li><li>**5-Band Equalizer Panel**: Frequency adjustment sliders (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) with range from -15dB to +15dB.</li><li>**Preset Selector Pills**: Quick preset buttons (*Flat, Classical, Jazz, Pop, Rock, Custom*).</li><li>**Vertical Scrollbar Indicator**: Active scroll track line replacing legacy dot indicators.</li><li>**Settings Modal**: Preferences for safety warnings, background service persistent notifications, and onboarding replays.</li><li>**Onboarding Screen**: Welcome dialog for first-time users.</li></ul> |
| **`AudioEffectManager.kt`** | **Audio Processing Engine & State Management**:<ul><li>Manages native Android `LoudnessEnhancer` DSP effects for boosting volume beyond standard levels.</li><li>Manages native Android `Equalizer` DSP effects across 5 frequency bands.</li><li>Stores and retrieves user preferences (boost levels, equalizer gains, dark theme state) via `SharedPreferences`.</li><li>Exposes reactive `StateFlow` streams for UI synchronization.</li></ul> |
| **`VolumeBoosterService.kt`** | **Foreground Processing Service & Floating Overlay**:<ul><li>Runs as an Android Foreground Service (`NotificationCompat`) to keep audio processing active when the app is minimized or in the background.</li><li>Manages the **System Floating Window Bubble** (`WindowManager`, `TYPE_APPLICATION_OVERLAY`), providing quick-access boost controls on top of other active apps.</li></ul> |
| **`VolumeBoosterTileService.kt`** | **Android Quick Settings Tile Integration**:<ul><li>Integrates with the Android System Quick Settings drop-down menu (`TileService`), enabling users to turn the sound booster ON or OFF with a single tap from anywhere in the OS.</li></ul> |

---

### 3. Android Resources (`app/src/main/res/`)

| File / Path | Corresponding Function & Feature |
| :--- | :--- |
| `res/drawable/ic_launcher_background.xml` | Adaptive App Icon vector background featuring a deep purple cracked gradient canvas and subtle aura glow. |
| `res/drawable/ic_launcher_foreground.xml` | Adaptive App Icon vector foreground depicting a cracked blasting speaker, headphone arc, energy lightning bolt, gauge meter, and electric audio waves. |
| `res/mipmap-*/` | System launcher icon resource sets across all device screen densities (`mdpi` to `xxxhdpi`). |
| `res/values/strings.xml` | Localized string definitions (app labels, warning texts, preset names). |
| `res/values/colors.xml` & `themes.xml` | Material Design 3 color palette and application window styling. |
| `app/src/main/AndroidManifest.xml` | Android system manifest declaring required permissions (`MODIFY_AUDIO_SETTINGS`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `INTERNET`), service components, and tile services. |

---

### 4. Build Scripts & Utilities (`scripts/`)

| File / Path | Corresponding Function & Feature |
| :--- | :--- |
| `scripts/check_requirements.sh` | Shell script for **Linux & macOS** that verifies JDK 17+, Android SDK, Gradle, and `.env` setup before building. |
| `scripts/check_requirements.bat` | Command Prompt script for **Windows** that checks Java runtime, Android SDK paths, Gradle tools, and `.env` files. |
| `scripts/check_requirements.ps1` | PowerShell script for **Windows & Cross-Platform** requirement verification. |
| `scripts/build.sh` | Automated build script for **Linux & macOS** that loads `.env` versioning, runs environment checks, executes Gradle tasks, outputs timestamped log files to `logs/`, and copies compiled APKs to `.build-outputs/`. |
| `scripts/build.bat` | Automated build script for **Windows CMD** with logging and APK copy pipeline. |
| `scripts/build.ps1` | Automated build script for **PowerShell** with logging and output copying. |

---

### 5. Documentation Directory (`howto/`)

| File / Path | Corresponding Function & Feature |
| :--- | :--- |
| `howto/setup_develop_build.md` | Guide covering environment prerequisites, development instructions, script usage, and cross-platform build commands for Linux, Windows, and macOS. |
| `howto/general_information.md` | Overview guide detailing application specifications, architecture diagrams, component layers, and repository structure. |
| `howto/file_function_mapping.md` | *(This file)* Comprehensive breakdown mapping repository folders and files to their functional roles. |

---

### 6. Output & Log Directories

| Directory / Path | Corresponding Function & Feature |
| :--- | :--- |
| `.build-outputs/` | Target folder where compiled `.apk` files (e.g., `app-debug.apk`) are automatically exported after successful builds. |
| `logs/` | Target folder containing timestamped build execution logs (e.g., `build_YYYYMMDD_HHMMSS.log`) and `latest_build.log`. |
