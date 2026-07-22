# 🛠️ Setup, Development, and Build Guide

This comprehensive guide walks you through setting up the development environment, running prebuilt requirement checks, developing, and compiling **Milkys Sound Booster & EQ** on **Linux**, **Windows**, and **macOS**.

---

## 📋 Table of Contents
1. [Prerequisites](#-prerequisites)
2. [Environment Configuration (`.env`)](#-environment-configuration-env)
3. [Prebuilt Machine Requirement Test Scripts](#-prebuilt-machine-requirement-test-scripts)
4. [Development Environment Setup](#-development-environment-setup)
5. [Building the Application](#-building-the-application)
   - [Linux & macOS](#1-linux--macos)
   - [Windows (CMD / Command Prompt)](#2-windows-cmd--command-prompt)
   - [Windows / Cross-Platform (PowerShell)](#3-windows--cross-platform-powershell)
   - [Standard Gradle Commands](#4-standard-gradle-commands)
6. [Build Logs & Output Locations](#-build-logs--output-locations)
7. [Troubleshooting & FAQ](#-troubleshooting--faq)

---

## ⚙️ Prerequisites

Ensure your host operating system has the following software installed:

| Requirement | Minimum / Recommended Version | Details |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | JDK 17 (JDK 21 Recommended) | Ensure `JAVA_HOME` environment variable is configured |
| **Android SDK** | API Level 36 (minSdk 24) | Set `ANDROID_HOME` or `ANDROID_SDK_ROOT` path |
| **Gradle** | Gradle 8.5+ or 9.x | Included via global `gradle` or local wrapper (`gradlew` / `gradlew.bat`) |
| **Android Studio** | Android Studio Ladybug / Jellyfish (Optional) | Recommended IDE for Jetpack Compose live previewing and editing |

---

## 🔑 Environment Configuration (`.env`)

The project uses `.env` files to configure build versioning, output directories, logs, and API keys.

1. **Create `.env` file**:
   If `.env` does not exist, copy `.env.example`:
   ```bash
   cp .env.example .env
   ```

2. **Available Configuration Parameters**:
   ```env
   # GEMINI_API_KEY: Optional key for server-side or local Gemini AI features
   GEMINI_API_KEY=MY_GEMINI_API_KEY

   # GOOGLE_ADS_API_KEY: Google AdMob App ID
   GOOGLE_ADS_API_KEY=ca-app-pub-3940256099942544~3347511713

   # INCLUDE_GOOGLE_ADS: Set to "true" to enable AdMob banners, "false" to exclude
   INCLUDE_GOOGLE_ADS=true

   # Build Versioning Parameters
   VERSION_CODE=3
   VERSION_NAME=3.0

   # Custom Directory Locations
   BUILD_OUTPUT_DIR=.build-outputs
   BUILD_LOGS_DIR=logs
   ```

---

## 🧪 Prebuilt Machine Requirement Test Scripts

Before attempting a build, run the requirement script for your operating system to automatically verify Java, Android SDK, Gradle, and `.env` settings.

### 🐧 Linux & 🍎 macOS
```bash
# Make requirement check script executable
chmod +x scripts/check_requirements.sh

# Run check
./scripts/check_requirements.sh
```

### 🪟 Windows (Command Prompt / CMD)
```cmd
scripts\check_requirements.bat
```

### ⚡ Windows / Cross-Platform (PowerShell)
```powershell
powershell -ExecutionPolicy Bypass -File scripts/check_requirements.ps1
```

---

## 💻 Development Environment Setup

### Option A: Using Android Studio (Recommended)
1. Launch **Android Studio**.
2. Select **Open** and choose the root directory of this repository (`/`).
3. Allow Gradle to perform the initial project sync.
4. Select `app` run configuration and target an Android Emulator or connected USB Device (Android 7.0+ / API 24+).
5. Click **Run (`Shift + F10`)**.

### Option B: Command Line Development
1. Edit Kotlin source files located in `app/src/main/java/com/example/`.
2. Edit resources in `app/src/main/res/`.
3. Test code syntax and build validity using `./scripts/build.sh assembleDebug` or `gradle assembleDebug`.

---

## 🔨 Building the Application

Automated build scripts manage `.env` variable loading, prerequisite validation, timestamped log recording, and output file copying.

### 1. Linux & macOS

```bash
# Make script executable
chmod +x scripts/build.sh

# Build Debug APK
./scripts/build.sh assembleDebug

# Build Release APK
./scripts/build.sh assembleRelease
```

### 2. Windows (CMD / Command Prompt)

```cmd
:: Build Debug APK
scripts\build.bat assembleDebug

:: Build Release APK
scripts\build.bat assembleRelease
```

### 3. Windows / Cross-Platform (PowerShell)

```powershell
# Build Debug APK
powershell -ExecutionPolicy Bypass -File scripts/build.ps1 -Task assembleDebug

# Build Release APK
powershell -ExecutionPolicy Bypass -File scripts/build.ps1 -Task assembleRelease
```

### 4. Standard Gradle Commands

You can also run Gradle directly from terminal or command line:

```bash
# Linux / macOS
gradle assembleDebug

# Windows CMD
gradle.bat assembleDebug
```

---

## 📂 Build Logs & Output Locations

Custom build scripts and Gradle tasks automatically export artifacts to environment-configured locations:

1. **Compiled APK Outputs**:
   - Location: `.build-outputs/` (or specified in `BUILD_OUTPUT_DIR` in `.env`).
   - Standard output file: `.build-outputs/app-debug.apk`.

2. **Execution Log Files**:
   - Location: `logs/` (or specified in `BUILD_LOGS_DIR` in `.env`).
   - Timestamped log file: `logs/build_YYYYMMDD_HHMMSS.log`.
   - Latest log symlink/file: `logs/latest_build.log`.

---

## ❓ Troubleshooting & FAQ

- **Error: `JAVA_HOME is not set`**:
  Ensure JDK 17+ is installed and set `export JAVA_HOME=/path/to/jdk` in `.bashrc` / `.zshrc` or system environment variables.
- **Error: `SDK location not found`**:
  Verify `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to your Android SDK installation (e.g., `/Users/username/Library/Android/sdk` on macOS or `C:\Users\username\AppData\Local\Android\Sdk` on Windows).
- **Error: `Permission denied` on Linux/macOS**:
  Run `chmod +x scripts/*.sh` to grant execution permissions.
