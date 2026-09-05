#!/usr/bin/env bash
# scripts/device_prep.sh — Device prep for Redmi/ADVAN inspections after pm clear / install -r
# Handles: POST_NOTIFICATIONS Allow dialog (pm grant silent Q17) + Onboarding GET STARTED overlay (run-as Q18)
# Usage: bash scripts/device_prep.sh [serial] [apk_path]  e.g. bash scripts/device_prep.sh hm5xr8gueiz5x4c6 .build-outputs/app-playstore-debug.apk
#        bash scripts/device_prep.sh A1013A5320TH000257
set -euo pipefail
DEVICE="${1:-hm5xr8gueiz5x4c6}"
APK="${2:-}"
if [ -n "$APK" ] && [ -f "$APK" ]; then
  echo "[*] Installing $APK to $DEVICE ..."
  adb -s "$DEVICE" install -r "$APK" 2>&1 | tail -n 3
fi
echo "[*] Device prep for $DEVICE ..."
adb -s "$DEVICE" shell svc power wakeup 2>&1 | head -n 2 || true
adb -s "$DEVICE" shell settings put system screen_off_timeout 1800000 2>&1 || true
# Q17 silent pm grant POST_NOTIFICATIONS (no Allow dialog)
echo "[*] Granting POST_NOTIFICATIONS ..."
adb -s "$DEVICE" shell pm grant com.milkys.soundbooster android.permission.POST_NOTIFICATIONS 2>&1 || echo "[!] pm grant failed (try tap Allow fallback)"
# Launch to create prefs if first run
adb -s "$DEVICE" shell am force-stop com.milkys.soundbooster 2>&1 || true
adb -s "$DEVICE" shell am start -n com.milkys.soundbooster/.MainActivity 2>&1 | tail -n 2
sleep 3
# Q18 run-as: set has_seen_onboarding true via SharedPrefs (fast, no tap)
echo "[*] Setting has_seen_onboarding=true via run-as ..."
if adb -s "$DEVICE" shell "run-as com.milkys.soundbooster sh -c 'cat /data/data/com.milkys.soundbooster/shared_prefs/volume_booster_prefs.xml 2>&1 | head -n 30'" 2>&1 | grep -q "volume_booster_prefs"; then
  adb -s "$DEVICE" shell "run-as com.milkys.soundbooster sh -c '
    PREF=/data/data/com.milkys.soundbooster/shared_prefs/volume_booster_prefs.xml
    if [ -f \"\$PREF\" ]; then
      if grep -q has_seen_onboarding \"\$PREF\"; then
        sed -i \"s/.*has_seen_onboarding.*/    <boolean name=\"has_seen_onboarding\" value=\"true\" \\/>/\" \"\$PREF\"
      else
        sed -i \"/<map>/a \\    <boolean name=\"has_seen_onboarding\" value=\"true\" \\/>\" \"\$PREF\"
      fi
      echo \"[run-as] patched \$PREF\"
      cat \"\$PREF\" | grep -E \"has_seen|ad_consent\" | head -n 5
    else
      echo \"[run-as] no prefs yet\"
    fi
  '" 2>&1 | tail -n 10
else
  echo "[!] run-as not available (release build) -> tap fallback"
fi
# Fallback tap GET STARTED if still visible (check via uiautomator)
echo "[*] Checking onboarding overlay ..."
if adb -s "$DEVICE" shell uiautomator dump /sdcard/ui_prep_check.xml 2>&1 | grep -q "ui_prep_check"; then
  if adb -s "$DEVICE" shell cat /sdcard/ui_prep_check.xml 2>&1 | grep -q "GET STARTED"; then
    echo "[*] Onboarding GET STARTED visible -> tapping ..."
    # Try testTag via uiautomator text search, tap center
    adb -s "$DEVICE" shell input tap 540 1800 2>&1 || true
    sleep 1
    adb -s "$DEVICE" shell input tap 540 1500 2>&1 || true
    sleep 1
  else
    echo "[*] No GET STARTED overlay detected"
  fi
fi
# Relaunch clean
adb -s "$DEVICE" shell am force-stop com.milkys.soundbooster 2>&1 || true
adb -s "$DEVICE" shell am start -n com.milkys.soundbooster/.MainActivity 2>&1 | tail -n 2
sleep 3
adb -s "$DEVICE" shell dumpsys window 2>&1 | grep -E "mCurrentFocus|mFocusedApp|mAwake" | head -n 5
adb -s "$DEVICE" shell dumpsys power 2>&1 | grep mWakefulness | head -n 2
echo "[*] device_prep done $DEVICE: POST_NOTIFICATIONS granted, hasSeenOnboarding true, awake true"
echo "[*] Verify: adb -s $DEVICE shell 'run-as com.milkys.soundbooster cat /data/data/com.milkys.soundbooster/shared_prefs/volume_booster_prefs.xml' | grep has_seen"
