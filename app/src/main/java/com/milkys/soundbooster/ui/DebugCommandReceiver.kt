package com.milkys.soundbooster.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.milkys.soundbooster.AudioEffectManager
import com.milkys.soundbooster.BuildConfig
import com.milkys.soundbooster.VolumeBoosterService

/**
 * DEBUG-only command bridge (Q1-A single action).
 * Usage:
 *   adb shell am broadcast -a com.milkys.soundbooster.DEBUG --es cmd booster_on
 *   adb shell am broadcast -a com.milkys.soundbooster.DEBUG --es cmd dump_state
 * All writes mirror dashboard paths so state stays consistent.
 * No effect in release builds.
 */
class DebugCommandReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return
        if (intent.action != ACTION) return
        val app = ctx.applicationContext
        AudioEffectManager.init(app)
        val cmd = intent.getStringExtra("cmd")?.lowercase() ?: return
        Log.v(TAG, "cmd=$cmd extras=${intent.extras}")
        when (cmd) {
            "booster_on" -> {
                AudioEffectManager.setBoostEnabled(true)
                startSvc(app)
            }
            "booster_off" -> {
                AudioEffectManager.setBoostEnabled(false)
                if (!AudioEffectManager.isFloatingEnabled.value) stopSvc(app) else stopSvcIfIdle(app)
            }
            "booster_toggle" -> {
                val now = !AudioEffectManager.isBoostEnabled.value
                AudioEffectManager.setBoostEnabled(now)
                if (now) startSvc(app) else if (!AudioEffectManager.isFloatingEnabled.value) stopSvc(app)
            }
            "level" -> {
                val v = intent.getIntExtra("level", intent.getIntExtra("value", -1))
                if (v in 0..100) AudioEffectManager.setBoostProgress(v)
            }
            "preset" -> {
                val n = intent.getStringExtra("preset") ?: intent.getStringExtra("name") ?: return
                AudioEffectManager.applyPreset(n)
            }
            "set_eq" -> {
                val en = intent.getBooleanExtra("enabled", intent.getStringExtra("enabled")?.toBoolean() ?: false)
                AudioEffectManager.setEqEnabled(en)
            }
            "set_band" -> {
                val band = intent.getIntExtra("band", 0)
                val lvl = intent.getIntExtra("level", 0)
                AudioEffectManager.setBandLevel(band, lvl)
            }
            "overlay_show", "overlay_on", "overlay_enable" -> {
                AudioEffectManager.setFloatingEnabled(true)
                startSvc(app)
            }
            "overlay_hide", "overlay_off", "overlay_disable" -> {
                AudioEffectManager.setFloatingEnabled(false)
                if (!AudioEffectManager.isBoostEnabled.value) stopSvc(app)
            }
            "overlay_toggle" -> {
                val now = !AudioEffectManager.isFloatingEnabled.value
                AudioEffectManager.setFloatingEnabled(now)
                if (now) startSvc(app) else if (!AudioEffectManager.isBoostEnabled.value) stopSvc(app)
            }
            "overlay_power_on", "overlay_power_off", "overlay_power_toggle" -> {
                val want = when (cmd) {
                    "overlay_power_on" -> true
                    "overlay_power_off" -> false
                    else -> !AudioEffectManager.isBoostEnabled.value
                }
                AudioEffectManager.setBoostEnabled(want)
                if (want) startSvc(app) else if (!AudioEffectManager.isFloatingEnabled.value) stopSvc(app)
            }
            "dump_state" -> dump(app)
        }
        if (cmd != "dump_state") dump(app)
    }

    private fun dump(ctx: Context) {
        val s = buildString {
            append("isBoosted=${AudioEffectManager.isBoostEnabled.value} ")
            append("progress=${AudioEffectManager.boostProgress.value} ")
            append("isEqEnabled=${AudioEffectManager.isEqEnabled.value} ")
            append("bands=${AudioEffectManager.eqBands.value.joinToString()} ")
            append("preset=${AudioEffectManager.eqPreset.value} ")
            append("favorites=${AudioEffectManager.favoritePresets.value} ")
            append("isFloating=${AudioEffectManager.isFloatingEnabled.value}")
        }
        Log.v(TAG, s)
    }

    private fun startSvc(ctx: Context) {
        try {
            val i = Intent(ctx, VolumeBoosterService::class.java).apply { action = VolumeBoosterService.ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        } catch (e: Throwable) { Log.v(TAG, "startSvc failed: ${e.message}") }
    }
    private fun stopSvc(ctx: Context) {
        try {
            val i = Intent(ctx, VolumeBoosterService::class.java).apply { action = VolumeBoosterService.ACTION_STOP }
            ctx.startService(i)
        } catch (e: Throwable) { Log.v(TAG, "stopSvc failed: ${e.message}") }
    }
    private fun stopSvcIfIdle(ctx: Context) { if (!AudioEffectManager.isFloatingEnabled.value && !AudioEffectManager.isBoostEnabled.value) stopSvc(ctx) }

    companion object {
        const val ACTION = "com.milkys.soundbooster.DEBUG"
        const val TAG = "DebugCommand"
    }
}
