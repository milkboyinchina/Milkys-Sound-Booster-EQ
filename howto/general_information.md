# 📱 General Information Guide - Milkys Sound Booster & EQ

Welcome to the general technical and functional overview for **Milkys Sound Booster & EQ** (Milkys App). This document provides architectural insights, feature specifications, and repository structure details.

---

## 📌 App Overview & Metadata

- **Application Name**: Milkys Sound Booster & EQ
- **Package Name / Application ID**: `com.aistudio.volumebooster.vmbstr`
- **Current Version**: `3.0` (Version Code: `3`)
- **Target OS**: Android 7.0 (API Level 24) to Android 15 (API Level 36)
- **UI Framework**: Jetpack Compose with Material 3 Design System
- **Language**: 100% Kotlin

---

## 🎧 Core Features & Capabilities

### 1. Master Sound Amplification (+200% Booster)
- Utilizes Android native `LoudnessEnhancer` and `AudioTrack` DSP engines.
- Amplifies speaker, earphone, and Bluetooth headphone audio output beyond default hardware limits safely.
- Interactive glowing power dial with live gain feedback.

### 2. 5-Band Graphic Equalizer
- Five frequency bands: **60Hz** (Bass), **230Hz** (Low-Mid), **910Hz** (Mid), **3.6kHz** (High-Mid), **14kHz** (Treble).
- Smooth range sliders allowing adjustments from -15dB to +15dB per band.

### 3. Preconfigured Audio Presets
- Instant preset switcher including:
  - **Flat**: Balanced baseline response.
  - **Classical**: Enhanced mids and gentle highs for orchestra clarity.
  - **Jazz**: Warm bass boost with smooth mid presence.
  - **Pop**: Dynamic bass and treble boost for modern tracks.
  - **Rock**: Heavy punchy low-end with crisp high frequencies.
  - **Custom**: User-adjusted band configuration saved automatically.

### 4. Instant 3-Second Sound Test Button
- Integrated direct sound test generator next to the master dial.
- Synthesizes a 3-second multi-frequency test melody to verify equalizer and booster output instantly without needing an external audio player.

### 5. Hearing & Speaker Damage Safety Banner
- Prominent high-contrast safety warning banner highlighting hearing impairment and hardware damage risks when over-boosting audio.
- Dismissable with a 7-day auto-remind timer.

### 6. Floating Overlay Quick-Controls
- System-wide floating bubble overlay (`TYPE_APPLICATION_OVERLAY`) enabling users to tweak booster volume and equalizer levels from any active game, video, or music app.

### 7. Quick Settings Notification Tile Service
- Integrated Android Quick Settings Tile (`VolumeBoosterTileService`) allowing one-tap activation directly from the Android quick notifications drawer.

### 8. Environment-Driven AdMob & Target Build Strategy
- **Dual Distribution Targets**: Supports `playstore` (AdMob enabled), `fdroid` (AdMob disabled), and `both` (sequential build for both stores).
- Full runtime and compile-time AdMob support toggle controlled via `BUILD_TARGET` and `INCLUDE_GOOGLE_ADS` in `.env`.
- F-Droid builds automatically exclude Google AdMob SDK dependencies and remove the AdMob toggle from the app settings.

### 9. Open Source Licensing & Transparency
- Licensed under **GNU General Public License v3.0 (GPLv3)**.
- Integrated Open Source License viewer with full scrollable GPLv3 terms and direct GitHub repository link button (`https://github.com/milkys/sound-booster-eq`).

---

## 🏗️ Architecture & Component Design

The app follows modern Android **MVVM** and **Clean Architecture** patterns:

```
                  ┌─────────────────────────────────────┐
                  │    Jetpack Compose UI Layer         │
                  │  (MainActivity / Floating Controls) │
                  └──────────────────┬──────────────────┘
                                     │ StateFlow / Coroutines
                  ┌──────────────────▼──────────────────┐
                  │       AudioEffectManager            │
                  │   (Singleton Audio Controller)      │
                  └────────┬────────────────────┬───────┘
                           │                    │
          ┌────────────────▼──────────┐      ┌──▼──────────────────────────┐
          │ VolumeBoosterService      │      │ Android Room Database /     │
          │ (Foreground Processing)   │      │ Key-Value State Storage     │
          └───────────────────────────┘      └─────────────────────────────┘
```

- **`MainActivity.kt`**: Main Jetpack Compose entry point containing the dashboard, equalizer visualizer, preset selector, settings, and onboarding flows.
- **`AudioEffectManager.kt`**: Central audio state engine managing `LoudnessEnhancer`, `Equalizer`, and `Preset` parameters.
- **`VolumeBoosterService.kt`**: Android Foreground Service handling background audio processing, notification controls, and floating bubble overlays.
- **`VolumeBoosterTileService.kt`**: Quick Settings Tile integration for Android quick settings panel.

---

## 📂 Repository Directory Map

```
/
├── app/                              # Primary Android application module
│   ├── src/main/java/com/example/    # Kotlin source code files
│   │   ├── MainActivity.kt           # Jetpack Compose UI & Screen Views
│   │   ├── AudioEffectManager.kt     # DSP & Audio Equalizer Engine
│   │   ├── VolumeBoosterService.kt   # Foreground Service & Floating Overlay
│   │   └── VolumeBoosterTileService.kt # Quick Settings Tile
│   └── src/main/res/                 # Android Drawables, Strings, & XML Resources
├── assets/                           # Branding assets and app logo
├── howto/                            # Comprehensive documentation folder
│   ├── setup_develop_build.md        # Environment setup, dev, and build guide
│   ├── general_information.md        # General architecture & info guide
│   └── file_function_mapping.md      # Detailed file & folder to function mapping guide
├── scripts/                          # Multi-platform requirement & build scripts
│   ├── check_requirements.sh         # Linux/macOS requirement checker
│   ├── check_requirements.bat        # Windows CMD requirement checker
│   ├── check_requirements.ps1        # PowerShell requirement checker
│   ├── build.sh                      # Linux/macOS build script with logs
│   ├── build.bat                     # Windows CMD build script with logs
│   └── build.ps1                     # PowerShell build script with logs
├── .env.example                      # Environment variable template
├── build.gradle.kts                  # Root project build configuration
├── metadata.json                     # AI Studio platform metadata
└── README.md                         # Main repository README documentation
```

---

## 🛡️ Safety & Responsible Usage

Amplifying audio beyond hardware defaults can cause speaker clipping, harmonic distortion, speaker cone fatigue, and permanent hearing loss if listened to at high volumes for extended periods. Users should start at lower boost levels (+10% to +30%) before adjusting higher.
