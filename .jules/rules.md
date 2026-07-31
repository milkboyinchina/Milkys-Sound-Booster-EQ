# 📜 Google Jules Agent Execution Rules (.jules/rules.md)

These guidelines govern how **Google Jules** must analyze, edit, build, verify, and submit code changes to this repository.

---

## 🔒 1. Audio DSP Engine & Safety Guardrails

1. **Volume Boost Ceiling**: Maximum gain amplifications must **NEVER exceed 200% (+20 dB)** under any circumstance.
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
1. Run `./scripts/setup_jules_env.sh` to ensure the environment dependencies are synced.
2. Run `./gradlew test` to ensure all unit tests pass without failure.
3. Run `./scripts/build.sh assembleDebug` to confirm that the Android Debug APK builds successfully and outputs to `.build-outputs/`.

---

## 🔀 4. Git Commit & Pull Request Guidelines

1. **Commit Messages**: Write imperative, descriptive commit titles (e.g. `feat(audio): add custom equalizer preset persistence`).
2. **PR Format**: Include:
   - **Summary**: Concise overview of what was changed and why.
   - **Verification**: Output log confirmation of `./gradlew test` and APK compilation.
   - **Ref**: Link to the original GitHub issue.
