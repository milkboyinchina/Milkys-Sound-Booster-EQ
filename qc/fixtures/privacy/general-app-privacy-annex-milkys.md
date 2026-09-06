# Annex: Milkys Sound Booster & EQ (com.milkys.soundbooster) — App-Specific Disclosure for https://milkboy.my.id/term/general-app-privacy

**Add this as Section 8 to https://milkboy.my.id/term/general-app-privacy (Q1-B app-specific annex inside general page, keep single URL https://milkboy.my.id/term/general-app-privacy for Play Console):**

## 8. Milkys Sound Booster & EQ — App-Specific Disclosure (com.milkys.soundbooster)

**Package:** `com.milkys.soundbooster`
**Version:** 0.1.27 (26090505) → 1.0.0 when ready for Play Console production (Q23)
**Role:** High-fidelity global audio booster & 5-band EQ (200% +15dB 1500mB, 60Hz/230Hz/910Hz/3.6kHz/14kHz), TileService, Foreground Service mediaPlayback, Overlay WindowManager

### Ads Placement (1 Banner + 2 Native by rows)
- **1 Adaptive Banner** top center for all window sizes (single outside when, outside LazyColumn, visible without scroll on COMPACT 491dp phone, MEDIUM 601dp tablet, EXPANDED w962dp land) — fallback Column Row SPONSORED AD + FrameLayout before AdView + AdListener hide onAdLoaded (never blank)
- **2 Native Ads pane-local by rows:** 
  - 1 row COMPACT (phone <600dp): Native #1 middle after QuickBoost before VisualEqualizer + Native #2 end after PresetManager before Battery
  - 2 rows MEDIUM (tablet 600-839dp): Native #1 top of Left row before Decibel + Native #2 top of Right row before VisualEqualizer
  - 3 rows EXPANDED (desktop >=840dp): Native #1 top of Left Pane before Decibel + Native #2 top of Right Pane before PresetManager (Pane2 VisualEqualizer clean, Pane3 showText false icons)
- Each NativeAdCard: Card fillMaxWidth RoundedCornerShape 20dp testTag native_ad_card, AdLoader ca-app-pub-3940256099942544/2247696110 (test, see below)

### AdMob Test vs Production (R3)
- **First production (closed testing Q22 done → production 0.1.2 current):** Test AdMob `ca-app-pub-3940256099942544~3347511713` `AdUnit banner 6300978111 native 2247696110` + `isAdsEnabled false` default (fresh installs 0 ads, Settings → Ads Keep On Outlined / Turn Off Button to enable) — safe, no test ads shown to real users by default, avoids AdMob Invalid traffic. Play allows test AdUnits in production review.
- **After Play lists com.milkys.soundbooster live:** AdMob console → Apps → Add app → Search store com.milkys.soundbooster → Create Ad Units Banner + Native → get prod APP_ID~ + AdUnit/ → swap 13 locales strings.xml:4 + .env GOOGLE_ADS_API_KEY + MainActivity.kt 2109 via secrets.GOOGLE_ADS_API_KEY, keep setTestDeviceIds F110E76BE642122F6DB37AF7A61167CD for QA, then bump 1.0.0 production update.

### Permissions & Data Safety (declared in Play Console)
- `POST_NOTIFICATIONS` (foreground service notification), `MODIFY_AUDIO_SETTINGS` (LoudnessEnhancer), `SYSTEM_ALERT_WINDOW` (overlay bubble), `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `INTERNET` (AdMob). No PII collected, only Advertising ID + Diagnostics (AdMob) + App activity, handled per Google Play Services Privacy Policy. UMP GDPR isUmpAvailable requestConsentInfoUpdate, Settings → Ads toggle + GDPR Personalized Ads toggle with Keep On Outlined / Turn Off Button (48dp).

### Toggle Behaviour
- Settings → Ads: Switch ads_toggle 48dp isAdsEnabled. Turn OFF triggers AlertDialog Support Development "Ads support developer — please kindly leave ads on to support voluntary development and server costs." Keep On Outlined (border PrimaryAccent, stays true, 1+2 remain) + Turn Off Button (filled PrimaryAccent, sets false, hides all 3). Verified hm5xr8gueiz5x4c6 491dp COMPACT + A1013A5320TH000257 601dp MEDIUM via scripts/device_prep.sh Q17 pm grant + Q18 run-as has_seen_onboarding=true before any dump/screencap.

**Effective Date:** July 23, 2026 (inherits General Policy) + 8. added 2026-09-06
**Contact:** webmaster@milkboy.my.id, https://milkboy.my.id, https://github.com/milkys/sound-booster-eq
**Canonical URL:** https://milkboy.my.id/term/general-app-privacy (keep http 308→https, Play requires https — updated .env PRIVACY_POLICY_URL http→https, fastlane 13 locales privacy_policy.txt https)

