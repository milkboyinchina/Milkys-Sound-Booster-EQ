# General Application Privacy Policy

*Effective date: July 23, 2026 · Last revised: October 2, 2026 (full rewrite for clarity; substance unchanged, app-specific Part B expanded).*

## 1. Summary

- The developer collects **no personally identifiable information (PII)**: no names, emails, accounts, locations, contacts, or audio recordings.
- Audio enhancement (boost, equalizer, presets) is processed **on your device in real time**. Audio never leaves the device.
- When ads are enabled, **Google (AdMob)** may process device identifiers and diagnostics under [Google's Privacy Policy](https://policies.google.com/privacy). Ads are **off by default**.
- This page has two parts: **Part A** (general, §§2–7, applies to all apps by this developer) and **Part B** (app-specific, §8).

## 2. Scope

This policy applies to all mobile applications published by the developer, including **Milkys Sound Booster & EQ** (`com.milkys.soundbooster`, see §8). Where an app-specific disclosure conflicts with Part A, the app-specific section governs for that app.

## 3. Data the developer collects

**None that identifies you.** The developer operates no accounts, no tracking servers, and no user profiles. Strictly anonymous, aggregate technical data (e.g. crash diagnostics delivered by system frameworks) may be used solely to fix stability and performance issues. Such data cannot identify, locate, or contact any individual.

The developer does **not** collect, and the apps do **not** transmit: precise location, contacts, call or message contents, photos, microphone recordings, or advertising-driven behavioral profiles.

## 4. Data processed by third parties

The apps rely on system frameworks and ad services provided by **Google LLC**. Those services act under their own policies — [Google Privacy Policy](https://policies.google.com/privacy) and [Google Play Services Privacy](https://policies.google.com/privacy) — and may automatically process:

| Data | Purpose | Controller |
|---|---|---|
| Advertising ID | Ad delivery, frequency capping, abuse prevention | Google |
| App activity (interactions, session state) | Ad relevance measurement, diagnostics | Google |
| Crash and performance diagnostics (stack traces, OS version, device model) | Stability and bug fixing | Google / developer (anonymous) |

No other third-party SDKs are embedded. There are no social logins, analytics trackers, or cross-app data brokers.

## 5. Advertising and consent (Google AdMob / UMP)

- Ads are **off by default**. Nothing ad-related renders until you enable **Settings → Ads**.
- When enabled, ads are served by Google AdMob. Google may use the Advertising ID, app activity, and diagnostics as described in §4.
- **Personalized ads** require your additional, explicit consent: **Settings → Personalized Ads**, plus the Google consent (UMP) form shown where the law requires it (e.g. EEA/UK). You may withdraw consent at any time from the same switch; the app then falls back to non-personalized ads or no ads per your Settings → Ads choice.
- During closed testing, creatives may render as Google-designated **test ads**. No action is needed; production ad units apply at the public release.
- Manage your identifier anytime: Android `Settings > Google > Ads > Reset or Delete advertising ID`.

## 6. Your controls

- **Advertising ID:** reset or delete it (`Settings > Google > Ads`). Deleting it does not break the app.
- **Permissions:** review or revoke any permission anytime (`Settings > Apps > [App name] > Permissions`). The app degrades gracefully (e.g. revoking overlay permission only removes the floating widget).
- **Uninstall:** removing the app deletes all locally stored settings (presets, favorites) with it; nothing remains on a developer server because there is none.

## 7. Children, changes, contact

- **Children:** the apps are general-audience utilities, not directed at children under 13. No child-directed profiling occurs.
- **Changes:** material updates are published on this page with a revised date. Continued use after revision constitutes acceptance.
- **Contact:** 📧 [webmaster@milkboy.my.id](mailto:webmaster@milkboy.my.id) · 🌐 [milkboy.my.id](https://www.milkboy.my.id)

---

## 8. Part B — Milkys Sound Booster & EQ (`com.milkys.soundbooster`)

High-fidelity audio booster (up to 200% / +15 dB) and 5-band equalizer (60 Hz / 230 Hz / 910 Hz / 3.6 kHz / 14 kHz) for Android, with Quick Settings tile, foreground media-playback service, and optional floating widget.

**On-device by design.** Boost, EQ curves, presets, favorites, and settings never leave the phone. The app does not record, store, or upload audio.

**Permissions and why each is needed:**

| Permission | Used for |
|---|---|
| Notifications | Booster controls while enhancement runs |
| Modify audio settings | Applies the equalizer and loudness enhancement |
| Display over other apps (optional) | Floating control widget |
| Foreground service, media playback | Keeps enhancement running while in use |
| Internet | Serves ads when you enable them (§5) |

*This §8 was expanded October 2, 2026; it inherits the July 23, 2026 effective date above.*
