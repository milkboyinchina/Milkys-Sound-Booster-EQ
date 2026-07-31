# 🎵 Milkys Sound Booster & EQ (Milkys App)

<p align="center">
  <img src="assets/logo.png" width="160" height="160" alt="Milkys App Logo" />
</p>

<p align="center">
  ✨ <i>Vibe coded using Google AI</i> ✨
</p>

A high-fidelity global audio booster and 5-band graphic equalizer designed for Android. **Milkys Sound Booster & EQ** allows users to amplify speaker and headphone output up to 200%, fine-tune audio frequencies across five visual bands, run instant 3-second sound tests, toggle quick floating overlay controls, and manage AdMob integrations with customized build scripts and versioning parameters.

---

## 🚀 Key Features

- **Global Sound Amplification**: Safely amplify system audio output up to 200%.
- **5-Band Graphic Equalizer**: Fine-tune specific audio frequencies with precise dB level sliders.
- **Preconfigured Audio Presets**: Select from predefined sound profiles (*Flat, Classical, Jazz, Pop, Rock, Custom*).
- **Instant Sound Test Button**: Play a 3-second synthesize test tune directly next to the master power button.
- **Hearing & Speaker Damage Protection Warnings**: Prominent high-contrast safety warning banner highlighting hearing impairment and speaker burnout risks.
- **Multilingual Support & Dropdown Language Selection**: Per-app language selector dropdown in Settings supporting 13 locales (*English, Bahasa Indonesia, Bahasa Melayu, Hindi, Portuguese, French, Italian, German, Simplified Chinese, Traditional Chinese, Japanese, Korean, System Default*).
- **Quick-Control Floating Widget**: System overlay bubble for fast boost and equalizer access over any active app.
- **Quick Settings Tile Service**: Toggle the audio processing service directly from the Android notifications drawer.
- **Google AdMob Integration**: Environment-driven ad placement with full runtime toggles.

---

## ⚡ AI Model Delegation & Zero-Delay Shortcuts

This workspace uses **Subagent Model Delegation** and **Workspace Rules** (`.gemini/rules/model_delegation.md`) to optimize token usage and execution speed:

- **`/quick <task>`**: Instantly delegates to **`@quick-task`** on **Low (`flash_lite`)** for Git operations (`git commit`, `git push`), file reads, quick greps, and minor edits.
- **`/standard <task>`**: Instantly delegates to **`@standard-dev`** on **Medium (`flash`)** for feature updates, UI tweaks, and unit tests.
- **`/hard-fix <task>`** or **`/hard-fix-sonnet <task>`**: Instantly delegates to **`@complex-architect`** on **High (`pro`)** for architectural refactoring, multi-file debugging, and security/performance overhauls.

To enable model delegation rules in any current or future project workspace, run:
```bash
./scripts/setup_project_rules.sh /path/to/target-project
```

---

## 📚 Documentation & Guides (`howto/`)

For detailed technical guides and architecture specifications, explore the `howto/` directory:

- 🛠️ **[Setup, Development & Build Guide](howto/setup_develop_build.md)**: Step-by-step development setup, requirement checks, cross-platform build commands (Linux, Windows, macOS), logging, and troubleshooting.
- 📱 **[General Information Guide](howto/general_information.md)**: High-level overview, architecture diagram, component design, feature specifications, and repository structure map.
- 📁 **[File & Function Mapping Guide](howto/file_function_mapping.md)**: Detailed mapping breakdown showing which directory or file corresponds to which application feature or system function.

---

## 🛠️ Requirements & Dependencies

### Prerequisites
- **Android SDK**: minimum SDK `24` (Android 7.0), target SDK `36` (Android 15)
- **Java Development Kit (JDK)**: JDK 17+ (JDK 21 supported)
- **Gradle**: Gradle 8.5+ with Kotlin DSL (or global Gradle 9.x)

### Key Dependencies
- **Jetpack Compose**: Modern Kotlin UI toolkit with Material 3 components
- **Google Play Services Ads**: AdMob SDK for banner ad integration
- **Kotlin Coroutines & Flow**: Safe asynchronous background execution
- **AndroidX Room**: Reliable local database storage and key-value state persistence
- **Secrets Gradle Plugin**: Secure compilation management utilizing `.env` parameters

---

## 🔑 Environment Variables & Versioning Setup (`.env`)

The project uses `.env` variables for build versioning, file output directories, logging configuration, and API keys.

1. Create or copy the `.env` configuration file from `.env.example`:
   ```bash
   cp .env.example .env
   ```
2. Environment parameters in `.env`:

   | Environment Variable | Description | Default Value |
   | :--- | :--- | :--- |
   | `APP_NAME` | Application display label and title | `Milkys Sound Booster & EQ` |
   | `APPLICATION_ID` | Google Play Console Application Package Identifier | `com.milkys.soundbooster` |
   | `VERSION_CODE` | Numeric version code for Android build configuration | `26072301` |
   | `VERSION_NAME` | Human-readable version string | `0.1` |
   | `BUILD_OUTPUT_DIR` | Target folder where compiled APKs are stored | `.build-outputs` |
   | `BUILD_LOGS_DIR` | Target folder where build log files are created | `logs` |
   | `GOOGLE_ADS_API_KEY` | Google AdMob App ID (Test ID pre-configured) | `ca-app-pub-3940256099942544~3347511713` |
   | `INCLUDE_GOOGLE_ADS` | Set `true` to include AdMob, `false` to exclude | `true` |
   | `DEVELOPER_WEBSITE_URL` | Developer website URL link in Settings | `https://milkys.app` |
   | `PRIVACY_POLICY_URL` | Privacy Policy URL link in Settings & Privacy Page | `https://milkys.app/privacy` |

---

## 🧪 Prebuilt Machine Requirement Test Scripts

Verify your machine's JDK, Android SDK, Gradle, and environment configuration before building:

### Linux & macOS
```bash
chmod +x scripts/check_requirements.sh
./scripts/check_requirements.sh
```

### Windows (CMD)
```cmd
scripts\check_requirements.bat
```

### Windows (PowerShell) / Cross-Platform PowerShell
```powershell
powershell -ExecutionPolicy Bypass -File scripts/check_requirements.ps1
```

---

## 🔨 Build Scripts with Execution Logs

Custom build scripts automatically check prerequisites, load `.env` parameters, execute the build task, save timestamped logs in `BUILD_LOGS_DIR`, and copy output APKs into `BUILD_OUTPUT_DIR`.

### Linux & macOS (`scripts/build.sh`)
```bash
# Make script executable
chmod +x scripts/build.sh

# Build Debug APK (default)
./scripts/build.sh assembleDebug

# Build Release APK
./scripts/build.sh assembleRelease
```

### Windows CMD (`scripts/build.bat`)
```cmd
# Build Debug APK
scripts\build.bat assembleDebug

# Build Release APK
scripts\build.bat assembleRelease
```

### Windows / Multi-Platform PowerShell (`scripts/build.ps1`)
```powershell
powershell -ExecutionPolicy Bypass -File scripts/build.ps1 -Task assembleDebug
```

### Build Outputs & Log Files
- **Compiled APKs**: Auto-copied to `.build-outputs/` (or the folder defined by `BUILD_OUTPUT_DIR`).
- **Build Log Files**: Saved as `logs/build_YYYYMMDD_HHMMSS.log` and `logs/latest_build.log` (or the folder defined by `BUILD_LOGS_DIR`).

---

## 💻 Standard Gradle Build Commands

If you prefer building directly with Gradle:

- **Linux / macOS**:
  ```bash
  gradle assembleDebug
  ```
- **Windows**:
  ```cmd
  gradle assembleDebug
  ```

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** (GPLv3). See the [LICENSE](LICENSE) file for full details.
