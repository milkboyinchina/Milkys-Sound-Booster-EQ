package com.milkys.soundbooster.ui

import android.util.Log
import com.milkys.soundbooster.BuildConfig

/**
 * DEBUG-only verbose bridge for prefs/flow bounce hunts (Q1-A minimal scope).
 * All calls are compiled out of release builds via [BuildConfig.DEBUG] so
 * production logging and performance are untouched. Tail on device with:
 * `bash scripts/log_tail.sh` (writes qc/traces/log-bounce-*.log, gitignored).
 */
object DebugLogBridge {
    const val TAG_AEM = "AudioEffectManager"
    const val TAG_DASH = "DashboardScreen"
    const val TAG_SVC = "VolumeBoosterService"

    // Log.v is not mocked in plain JVM unit tests — swallow so unit tests
    // exercise the same call sites as the device without Robolectric.
    @JvmStatic
    fun v(tag: String, msg: String) {
        if (!BuildConfig.DEBUG) return
        try {
            Log.v(tag, msg)
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun v(tag: String, msg: String, tr: Throwable) {
        if (!BuildConfig.DEBUG) return
        try {
            Log.v(tag, msg, tr)
        } catch (_: Throwable) {
        }
    }
}
