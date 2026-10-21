# F-Droid Release Plan — Milkys Sound Booster & EQ

> **Status:** Draft (planning only — no code changes yet)
> **Target:** Publish the FOSS build (`BUILD_TARGET=fdroid`, no AdMob) to the official F-Droid repository.
> **App ID:** `com.milkys.soundbooster`
> **License:** GNU GPL v3.0 (`LICENSE`)
> **Min/Target SDK:** 24 / 36

---

## 1. Objective

Ship a fully free-software build of Milkys Sound Booster & EQ to F-Droid, using the
existing `BUILD_TARGET=fdroid` path (`INCLUDE_GOOGLE_ADS=false`), with no proprietary
dependencies, no proprietary plugins, and no proprietary metadata in that build.

F-Droid must be able to build the APK from source on its build servers using only the
repository + a documented recipe.

---

## 2. Current Readiness Snapshot

| Item | Status | Evidence |
|---|---|---|
| License (GPLv3) | ✅ Ready | `LICENSE` |
| Source public | ✅ Ready | GitHub `milkboyinchina/Milkys-Sound-Booster-EQ` |
| F-Droid build target | ✅ Exists | `app/build.gradle.kts` (`envIncludeGoogleAds`, `envBuildTarget`) |
| F-Droid update checker | ✅ Exists | `update/FdroidUpdateChecker.kt` |
| Fastlane metadata (13 locales) | ✅ Exists | `fastlane/metadata/android/*` |
| Ads excluded in fdroid target | ✅ Conditional | `if (envIncludeGoogleAds)` deps |
| Google services plugin | ⚠️ Always applied | `plugins { alias(libs.plugins.google.services) }` |
| AdMob manifest meta-data | ⚠️ Always emitted | `AndroidManifest.xml` (all builds) |
| Firebase/credentials/googleid catalog entries | ℹ️ Unused | `gradle/libs.versions.toml` (not in deps) |
| `.env`-less build | ❌ Fails | `VERSION_CODE`/`VERSION_NAME` error if `.env` missing |
| F-Droid metadata recipe | ❌ Missing | not in repo / fdroiddata |
| CI fdroid release artifact | ⚠️ Only if `BUILD_TARGET=both` | `.github/workflows/ci-cd.yml` hydrates from `.env.example` (`playstore`) |

---

## 3. F-Droid Policy / Compliance Checklist

- [ ] **FOSS-only build**: `fdroid` target contains no `play-services-ads`, no UMP, no Firebase.
- [ ] **No proprietary plugins in fdroid build**: `google-services` plugin must not run for fdroid.
- [ ] **No proprietary manifest entries in fdroid build**: AdMob `APPLICATION_ID` meta-data must be absent.
- [ ] **Build from source**: F-Droid recipe builds with Gradle; no prebuilt binaries.
- [ ] **Versioning**: `versionCode`/`versionName` come from the tagged commit (no auto-bump on tags).
- [ ] **Permissions justified**: `INTERNET`, `MODIFY_AUDIO_SETTINGS`, foreground service, overlay, notifications.
- [ ] **Privacy policy URL reachable**: `PRIVACY_POLICY_URL`.
- [ ] **Anti-features**: None for the fdroid flavor (no ads). Document if any later (e.g. `NonFreeNet` for update checks — acceptable).
- [ ] **Reproducible build** (optional, recommended later): pin toolchain + disable timestamps.

---

## 4. Required Code / Build Changes (P0)

### 4.1 Make `google-services` plugin conditional

**File:** `app/build.gradle.kts`

Move from the `plugins {}` block to a conditional `apply`:

```kotlin
// plugins block: remove alias(libs.plugins.google.services)
...
if (envIncludeGoogleAds) {
    apply(plugin = "com.google.gms.google-services")
}
```

Keep `googleServices { missingGoogleServicesStrategy = WARN }` guarded the same way.

### 4.2 Remove AdMob manifest meta-data from fdroid builds

**File:** `app/src/main/AndroidManifest.xml`

Either:
- move the `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" .../>` block into a `src/playstore/AndroidManifest.xml` overlay, or
- wrap it so it is only merged when ads are enabled.

Simplest: create `app/src/playstore/AndroidManifest.xml` containing only the meta-data and remove it from the main manifest. Then fdroid builds won't carry it.

### 4.3 `.env`-less / F-Droid-friendly defaults

**File:** `app/build.gradle.kts`

Provide fallbacks so a missing `.env` does not hard-fail:

- `VERSION_CODE`: fall back to `System.getenv("VERSION_CODE")` then a sensible default (e.g. `1`).
- `VERSION_NAME`: fall back to `System.getenv("VERSION_NAME")` then `"dev"`.

F-Droid recipe will still set these explicitly, but defaults make local/CI robust.

### 4.4 Keep fdroid flavor free of proprietary plugins in CI

**File:** `.github/workflows/ci-cd.yml`

Add a verification step that builds the fdroid target in CI:

```bash
BUILD_TARGET=fdroid INCLUDE_GOOGLE_ADS=false SKIP_VERSION_BUMP=1 ./scripts/build.sh assembleRelease
```

and fails the job if it pulls proprietary deps.

### 4.5 (Optional cleanup) remove unused catalog entries

Remove `firebase-*`, `androidx-credentials`, `googleid` from `gradle/libs.versions.toml` to reduce F-Droid scanner noise. They are not referenced in `dependencies`.

---

## 5. F-Droid Metadata Recipe (P0)

Create a merge request in `fdroid/fdroiddata` adding
`metadata/com.milkys.soundbooster.yml`. A template is stored in this repo at
`fdroid/com.milkys.soundbooster.yml` (see below) and must be kept in sync with the tag.

Key fields:
- `SourceCode`, `IssueTracker`, `Changelog`
- `License: GPL-3.0-only`
- `AutoUpdateMode: Version` (or `Tags` with regex)
- `UpdateCheckMode: Tags`
- `CurrentVersion` / `CurrentVersionCode` matching the tag
- Build recipe with env vars and prebuild step

### 5.1 Template (stored in repo)

See `fdroid/com.milkys.soundbooster.yml`.

---

## 6. Fastlane Metadata (P1)

F-Droid can consume Fastlane metadata. The repo already ships 13 locales under
`fastlane/metadata/android/`. Verify:
- `title.txt`, `short_description.txt`, `full_description.txt`, `privacy_policy.txt` present for `en-US` (required) and others.
- Changelog files under `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` for each release.
- Icons/screenshots referenced by F-Droid metadata (512px icon).

Action: add `fastlane/metadata/android/en-US/changelogs/<newVersionCode>.txt` per release.

---

## 7. Release & Versioning Process (P0)

1. Bump `VERSION_NAME`/`VERSION_CODE` in `.env` for the release (or tag-driven in CI).
2. Create tag `vX.Y.Z` on `main`.
3. CI (`build-and-release`) builds playstore + (if enabled) fdroid artifacts and attaches them to the GitHub release.
4. F-Droid detects the new tag (AutoUpdateMode) or a manual MR updates `CurrentVersion*`.
5. F-Droid builds the fdroid flavor from the tag and publishes to its repo.
6. In-app `FdroidUpdateChecker` then reports the new version to fdroid installs.

**Version-code rule:** tags are source of truth → `SKIP_VERSION_BUMP=1` on tag builds (already enforced).

---

## 8. Verification Gates (must pass before submission)

```bash
# Environment
./scripts/check_requirements.sh

# fdroid build (no ads)
BUILD_TARGET=fdroid INCLUDE_GOOGLE_ADS=false SKIP_VERSION_BUMP=1 \
  ./scripts/build.sh assembleRelease

# Tests / lint / visuals
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew verifyRoborazziDebug

# Secret scan
gitleaks dir --redact
```

Additional F-Droid-specific checks:
- `grep -R "play-services-ads\|firebase" app/build.gradle.kts` returns nothing in fdroid path.
- APK contains no `com.google.android.gms.ads` classes (inspect with `unzip -l` / `jadx`).
- No `google-services.json` required to build.

---

## 9. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| F-Droid build fails due to missing `.env` | Add defaults (4.3) + prebuild step in recipe |
| Google services plugin breaks fdroid build | Conditional apply (4.1) |
| AdMob meta-data leaks into fdroid APK | Manifest overlay (4.2) |
| F-Droid review rejects proprietary deps | Grep/CI check (8) |
| Version mismatch between GitHub & F-Droid | Tag-driven versioning + AutoUpdateMode |
| Update checker points to wrong package | `FdroidUpdateChecker` uses `context.packageName` |

---

## 10. Task Breakdown

| ID | Task | Owner | Status |
|---|---|---|---|
| FD-001 | Conditional google-services plugin | Jules | OPEN |
| FD-002 | AdMob manifest overlay for playstore only | Jules | OPEN |
| FD-003 | `.env`-less build defaults | Jules | OPEN |
| FD-004 | CI fdroid release build step | Jules | OPEN |
| FD-005 | Remove unused firebase/credentials catalog entries | Jules | OPEN |
| FD-006 | Create fdroiddata MR using template | Maintainer | OPEN |
| FD-007 | Fastlane changelog files per versionCode | Maintainer | OPEN |
| FD-008 | Verify fdroid APK has no proprietary classes | Jules | OPEN |

---

## 11. Acceptance Criteria

- [ ] `BUILD_TARGET=fdroid` builds release APK on a clean clone with no `.env`.
- [ ] Resulting APK has zero `com.google.android.gms.ads.*` / Firebase classes.
- [ ] `gitleaks` + `lint` + tests + Roborazzi all green.
- [ ] F-Droid metadata MR submitted and accepted.
- [ ] In-app update checker resolves the F-Droid listing.

---

## 12. References

- `AGENTS.md` §1, §5 (coding standards, DSP safety)
- `howto/setup_develop_build.md`
- `app/build.gradle.kts` (build targets)
- `update/FdroidUpdateChecker.kt`
- F-Droid inclusion policy: https://f-droid.org/docs/Inclusion_Policy/
- F-Droid build metadata: https://f-droid.org/docs/Build_Metadata_Reference/
