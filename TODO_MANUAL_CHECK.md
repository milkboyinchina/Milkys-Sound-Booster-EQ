# TODO Manual Check - Milkys Sound Booster & EQ - refactor/codebase-improvements

> Branch: `refactor/codebase-improvements` | Commits: `149273c` (P0/P1), `d810ec4` (tests toolchain), `94b3dce` (ui modular)
> This file is for **manual verification** after automated checks. Complete each section and tick boxes before merging to `main`.

---

## 1. Automated Checks (Run Locally / CI)

### 1.1 Lint & Compile
```bash
bash ./gradlew :app:compileDebugKotlin --no-daemon   # BUILD SUCCESSFUL (only Divider deprecations)
bash ./gradlew :app:lintDebug                        # BUILD SUCCESSFUL, 0 errors (130 warnings)
# Check report: app/build/reports/lint-results-debug.html
```
- [ ] `lintDebug` shows 0 errors, no `ContentDescription` / `ClickableViewAccessibility` errors
- [ ] `compileDebugKotlin` no errors (warnings: `Divider->HorizontalDivider`, `Icons.Filled->AutoMirrored` expected)

### 1.2 Unit Tests (Java 21 toolchain auto-provision via Foojay)
```bash
bash ./gradlew :app:testDebugUnitTest --no-daemon     # 20/20 PASSED (7 suites)
# Or filtered: bash ./gradlew :app:testDebugUnitTest --tests "EqualizerPresetManagerTest" --tests "AdConsentManagerTest"
```
- [ ] `EqualizerPresetManagerTest` 8 tests pass (`app/src/test/java/.../EqualizerPresetManagerTest.kt:52` config sdk=34)
- [ ] `AdConsentManagerTest` 3 tests pass
- [ ] `FloatingOverlayTest` 5 tests pass (`@Config(sdk=[34])`)
- [ ] `GreetingScreenshotTest` + `PlayConsoleScreenshotsTest` pass (GraphicsMode NATIVE, sdk 34)
- [ ] `ExampleRobolectricTest` + `ExampleUnitTest` pass
- [ ] Verify JDK provision: `~/.gradle/jdks/eclipse_adoptium-21*` exists (log: `GradleWorker Daemon 1` with `java --add-exports`)

### 1.3 Build Output
```bash
bash scripts/check_requirements.sh
bash scripts/build.sh assembleDebug   # copies to .build-outputs/
```
- [ ] `check_requirements.sh` PASS (0 errors)
- [ ] Debug APK generated at `.build-outputs/app-debug-*.apk` and `app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Manual UI / Theme Checks (AGENTS.md 3.2 A/B/C)

### 2.1 Theme Tokens
- [ ] `ui/theme/Color.kt:11` `object AppColors` exists with 12 semantic tokens (DarkBackground etc.)
- [ ] `ui/theme/Theme.kt:13` `DarkColorScheme`/`LightColorScheme` use `AppColors` (background/surface)
- [ ] `MainActivity.kt:244` `bgColor/cardColor/textPrimary` use `AppColors` (no hardcoded `Color(0xFF...)` in dashboard)
- [ ] Toggle theme: Light/Dark via `AppHeaderRow` -> `MyApplicationTheme` switches correctly, no hardcoded hex in screens

### 2.2 Modularization P0-4
- [ ] `ui/components/HearingWarningCard.kt:1` exists (53 LOC extracted from `MainActivity.kt:2853`), `MainActivity.kt:68` imports it, placeholder comment left
- [ ] `MainActivity.kt` still compiles (3850 LOC -> expected after split ~3790, verify no duplicate definition)
- [ ] **Remaining TODO (not yet extracted, do manually or in next PR):** `AppHeaderRow:2484`, `DecibelBoosterCard:2589`, `QuickBoostPresetsCard:2788`, `VisualEqualizerCard:2911`, `EqualizerComponent:2967`, `SystemBatteryDiagnosticCard:3740`, `AudioVisualizer:1828`, `AdaptiveBannerAdCard:1924`, `NativeAdCard:2045`, `OnboardingQuickStartDialog:2231` -> create `ui/components/*.kt` + `ui/dashboard/DashboardScreen.kt`

### 2.3 Roborazzi Visual Matrix (AGENTS 3.2 A)
```bash
bash ./gradlew verifyRoborazziDebug --no-daemon
```
Manual matrix per `AGENTS.md:3.2` - verify screenshots at:
- [ ] Compact 320dp, Standard 360-411dp, Expanded 600dp+, Landscape (scrollable, no clip)
- [ ] Font scales 0.85x, 1.0x, 1.3x, 2.0x (Settings -> Display -> Font Size -> Largest) - EQ title `EQUALIZER` one line, 5-band sliders visible (height 240dp fix from `.plan/eq_regression_fix_plan_260804.md`)
- [ ] Light + Dark theme token passes

### 2.4 Accessibility (AGENTS 3.2 B)
- [ ] Run `lintDebug` and filter `accessibility` - 0 errors
- [ ] All `Icon` have `contentDescription = stringResource(...)` or `null` decorative (`MainActivity.kt:1018` etc., `VolumeBoosterService.kt:530` floating)
- [ ] All `IconButton`/`Surface` `Modifier.defaultMinSize(48.dp)` (check `HearingWarningCard.kt:43` close button 32dp -> should be 48dp, fix if needed)
- [ ] No `Modifier.height(...)` on text containers (use `heightIn` or `wrapContent`)

---

## 3. Audio Engine Manual Checks (AGENTS 5.2)

### 3.1 Safety & Gain
- [ ] `AudioEffectManager.kt:272` `mapProgressToGain` returns `progress*15` max 1500 mB (+15dB) not 3000; comment references AGENTS 5.2
- [ ] UI boost 0-100% maps to 0-200% loudness (check slider at 100% shows +100% not +200% overflow)

### 3.2 Resource Lifecycle
- [ ] `AudioEffectManager.kt:688` uses `AudioTrack.Builder` + `AudioAttributes.USAGE_MEDIA` (not deprecated `STREAM_MUSIC`)
- [ ] `AudioEffectManager.kt:19` has `silenceJob: Job?` + `audioScope: SupervisorJob` + `TAG`, `release():760` exists
- [ ] `VolumeBoosterService.kt:169` `onDestroy()` calls `AudioEffectManager.release()` before `super.onDestroy()`
- [ ] Test: enable booster, press home, `adb shell dumpsys audio` shows no leaked `LoudnessEnhancer`/`Equalizer` handles after service stop
- [ ] No `WebView/cache` mkdir in `MainActivity.kt:92` (removed)

### 3.3 Power & Doze
- [ ] `MainActivity.kt:144` `onResume` `checkBatterySaverState()`, `SystemBatteryDiagnosticCard` shows saver/optimization correctly

---

## 4. State & Architecture Checks

### 4.1 Lifecycle-Aware State (AGENTS 5.1)
- [ ] `MainActivity.kt:173` `collectAsStateWithLifecycle()` (not `collectAsState`) with import `androidx.lifecycle.compose.collectAsStateWithLifecycle`
- [ ] `VolumeBoosterService.kt:54` same for overlay Compose (`FloatingBubble`/`FloatingDashboard`)
- [ ] Verify no main-thread blocking: Room prefs via `Dispatchers.IO` (currently `SharedPreferences` still direct - next PR should migrate to `DataStore`)

### 4.2 DataStore Migration TODO (P1-1 next)
- [ ] Create `data/PreferencesRepository.kt` interface + `DataStorePreferencesRepository` impl
- [ ] Create `ui/viewmodel/BoosterViewModel.kt`, `EqViewModel.kt`, `SettingsViewModel.kt` with `StateFlow` + `viewModelScope`
- [ ] Inject via `ViewModelProvider.Factory` (or Hilt) and replace `AudioEffectManager` singletons step-by-step
- [ ] Add `PreferencesRepositoryTest` with `TestScope`

### 4.3 Ads Build Variant TODO (P1-2 next)
- [ ] Current: reflection `Class.forName("MobileAds")` in `MainActivity.kt:118`, `AdConsentManager.kt:12`
- [ ] Next: add `android { flavorDimensions "store"; productFlavors { playstore { }; fdroid { } } }` in `app/build.gradle.kts:66` and `if (BuildConfig.INCLUDE_GOOGLE_ADS)` direct imports, remove reflection

---

## 5. Build & CI Checks

### 5.1 Version Catalog
- [ ] `gradle/libs.versions.toml:13` `composeBom 2025.01.00` (was 2024.09.00), `app/build.gradle.kts:174` no commented `coil/retrofit/camera/moshi` deps
- [ ] `gradle/libs.versions.toml:1` versions `agp 9.1.1`, `kotlin 2.0.21`, `robolectric 4.16.1` (check `GradleDependency` warnings in lint -> 19 warnings expected for outdated libs, safe)
- [ ] `gradle/libs.versions.toml:21` removed unused `coilCompose, retrofit, accompanist, camera` entries

### 5.2 CI Workflow
- [ ] `.github/workflows/ci-cd.yml:31` uses `setup-java 21`, `setup-gradle v4`, `gitleaks-action v2`
- [ ] Verify `verify` job runs `lintDebug` + `testDebugUnitTest` + `gitleaks` + `actionlint` on push to `main`/`staging`
- [ ] `build-and-release` only on `workflow_dispatch` (manual)

### 5.3 Push
- [ ] Local: `git push origin refactor/codebase-improvements` (requires GitHub PAT, current push failed with `could not read Username` - retry with `gh auth login` or PAT)
- [ ] Verify remote: `git log origin/refactor/codebase-improvements --oneline` shows `94b3dce`

---

## 6. Final Manual Smoke Test (Device/Emulator API 34+)

1. Install debug APK, grant `POST_NOTIFICATIONS`, enable booster (power toggle) -> notification `Milkys Sound Booster & EQ Active (+XX%)` appears with `-10%/+10%/OFF` when `isNotifControlsEnabled`
2. Toggle `Overlay Control` -> grant `SYSTEM_ALERT_WINDOW` -> floating bubble draggable, snap to edge, expand to `Booster Overlay` with 4 favorites
3. Test 5-band EQ presets: `Flat`, `Bass Booster`, save custom `MyPreset` (1-10 chars, max 7), favorite max 4, export/import JSON
4. Check `HearingWarningCard` dismiss -> hidden 7 days (`hideHearingWarningFor7Days`), reappears after clearing prefs
5. Language switch via Settings -> `AppCompatDelegate.setApplicationLocales` + `config.setLocale`
6. Theme toggle -> no hardcoded color regressions

---

**Sign-off:** All boxes ticked -> merge `refactor/codebase-improvements` -> `main` via PR with `lintDebug` + `testDebugUnitTest` green.
