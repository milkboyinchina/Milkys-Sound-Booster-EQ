package com.example

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.PowerManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object AudioEffectManager {
    private const val PREFS_NAME = "volume_booster_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_BOOST = "boost"
    private const val KEY_PRESET = "preset"
    private const val KEY_BANDS = "bands"
    private const val KEY_FLOATING = "floating"
    private const val KEY_ADS_ENABLED = "ads_enabled"
    private const val KEY_SLIDER_STEPPED = "slider_stepped"
    private const val KEY_NOTIF_CONTROLS_ENABLED = "notif_controls_enabled"
    private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
    private const val KEY_HEARING_WARNING_DISABLED = "hearing_warning_disabled"
    private const val KEY_HEARING_WARNING_HIDDEN_UNTIL = "hearing_warning_hidden_until"
    private const val KEY_DARK_THEME = "dark_theme"

    private var context: Context? = null
    
    // State flows for UI observation
    private val _isBoostEnabled = MutableStateFlow(false)
    val isBoostEnabled: StateFlow<Boolean> = _isBoostEnabled

    private val _boostProgress = MutableStateFlow(0) // 0 to 100
    val boostProgress: StateFlow<Int> = _boostProgress

    private val _isFloatingEnabled = MutableStateFlow(false)
    val isFloatingEnabled: StateFlow<Boolean> = _isFloatingEnabled

    private val _isAdsEnabled = MutableStateFlow(true)
    val isAdsEnabled: StateFlow<Boolean> = _isAdsEnabled

    private val _isSliderStepped = MutableStateFlow(true)
    val isSliderStepped: StateFlow<Boolean> = _isSliderStepped

    private val _isNotifControlsEnabled = MutableStateFlow(true)
    val isNotifControlsEnabled: StateFlow<Boolean> = _isNotifControlsEnabled

    private val _hasSeenOnboarding = MutableStateFlow(false)
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding

    private val _isHearingWarningDisabled = MutableStateFlow(false)
    val isHearingWarningDisabled: StateFlow<Boolean> = _isHearingWarningDisabled

    private val _hearingWarningHiddenUntil = MutableStateFlow(0L)
    val hearingWarningHiddenUntil: StateFlow<Long> = _hearingWarningHiddenUntil

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    private val _eqPreset = MutableStateFlow("Flat")
    val eqPreset: StateFlow<String> = _eqPreset

    // 5 standard equalizer bands: 60Hz, 230Hz, 910Hz, 4kHz, 14kHz
    private val _eqBands = MutableStateFlow(intArrayOf(0, 0, 0, 0, 0)) // levels in dB (-15 to +15)
    val eqBands: StateFlow<IntArray> = _eqBands

    // Hardware/System state
    private val _isBatterySaverOn = MutableStateFlow(false)
    val isBatterySaverOn: StateFlow<Boolean> = _isBatterySaverOn

    private val _isBatteryOptimized = MutableStateFlow(false)
    val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized

    // Audio effects
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    
    // Silence AudioTrack for keeping global session active
    private var audioTrack: AudioTrack? = null
    private var isPlayingSilence = false

    fun init(ctx: Context) {
        val appContext = ctx.applicationContext
        context = appContext
        
        // Load preferences
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val boost = prefs.getInt(KEY_BOOST, 20) // Default 20% boost
        val preset = prefs.getString(KEY_PRESET, "Flat") ?: "Flat"
        val bandsStr = prefs.getString(KEY_BANDS, "0,0,0,0,0") ?: "0,0,0,0,0"
        val floating = prefs.getBoolean(KEY_FLOATING, false)
        val adsEnabled = prefs.getBoolean(KEY_ADS_ENABLED, true)
        val sliderStepped = prefs.getBoolean(KEY_SLIDER_STEPPED, true)
        val notifControlsEnabled = prefs.getBoolean(KEY_NOTIF_CONTROLS_ENABLED, true)
        val hasSeenOnboarding = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
        val warningDisabled = prefs.getBoolean(KEY_HEARING_WARNING_DISABLED, false)
        val warningHiddenUntil = prefs.getLong(KEY_HEARING_WARNING_HIDDEN_UNTIL, 0L)
        val darkTheme = prefs.getBoolean(KEY_DARK_THEME, true)

        _isBoostEnabled.value = enabled
        _boostProgress.value = boost
        _eqPreset.value = preset
        _isFloatingEnabled.value = floating
        _isAdsEnabled.value = adsEnabled
        _isSliderStepped.value = sliderStepped
        _isNotifControlsEnabled.value = notifControlsEnabled
        _hasSeenOnboarding.value = hasSeenOnboarding
        _isHearingWarningDisabled.value = warningDisabled
        _hearingWarningHiddenUntil.value = warningHiddenUntil
        _isDarkTheme.value = darkTheme
        
        val bands = bandsStr.split(",").map { it.toIntOrNull() ?: 0 }.toIntArray()
        _eqBands.value = if (bands.size == 5) bands else intArrayOf(0, 0, 0, 0, 0)

        // Initialize actual audio effects if enabled
        if (enabled) {
            startSilencePlayback()
            initEffects()
        }
        
        checkBatterySaverState()
    }

    private fun getPrefs() = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun checkBatterySaverState() {
        val ctx = context ?: return
        val powerManager = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        _isBatterySaverOn.value = powerManager.isPowerSaveMode
        
        val isOptimized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !powerManager.isIgnoringBatteryOptimizations(ctx.packageName)
        } else {
            false
        }
        _isBatteryOptimized.value = isOptimized
    }

    private fun initEffects() {
        try {
            // Apply globally (Audio Session ID 0)
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(0).apply {
                    enabled = _isBoostEnabled.value
                    setTargetGain(mapProgressToGain(_boostProgress.value))
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            loudnessEnhancer = null
        }

        try {
            if (equalizer == null) {
                equalizer = Equalizer(100, 0).apply {
                    enabled = _isBoostEnabled.value
                    applySavedBands()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            equalizer = null
        }
    }

    private fun releaseEffects() {
        try {
            loudnessEnhancer?.release()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            equalizer?.release()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        loudnessEnhancer = null
        equalizer = null
    }

    private fun mapProgressToGain(progress: Int): Int {
        // Progress (0 to 100%) maps to 0 to 3000 millibels (+30 dB)
        return progress * 30
    }

    private fun applySavedBands() {
        val eq = equalizer ?: return
        val bands = _eqBands.value
        try {
            val numBands = eq.numberOfBands
            for (i in 0 until numBands.toInt().coerceAtMost(bands.size)) {
                // dB level (-15 to 15) to millibels (-1500 to 1500)
                val mB = bands[i] * 100
                eq.setBandLevel(i.toShort(), mB.toShort())
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setBoostEnabled(enabled: Boolean) {
        _isBoostEnabled.value = enabled
        getPrefs()?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()

        if (enabled) {
            startSilencePlayback()
            initEffects()
            try {
                loudnessEnhancer?.enabled = true
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            try {
                equalizer?.enabled = true
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        } else {
            try {
                loudnessEnhancer?.enabled = false
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            try {
                equalizer?.enabled = false
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            releaseEffects()
            stopSilencePlayback()
        }
    }

    fun setBoostProgress(progress: Int) {
        _boostProgress.value = progress
        getPrefs()?.edit()?.putInt(KEY_BOOST, progress)?.apply()

        try {
            loudnessEnhancer?.setTargetGain(mapProgressToGain(progress))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setFloatingEnabled(enabled: Boolean) {
        _isFloatingEnabled.value = enabled
        getPrefs()?.edit()?.putBoolean(KEY_FLOATING, enabled)?.apply()
    }

    fun setAdsEnabled(enabled: Boolean) {
        _isAdsEnabled.value = enabled
        getPrefs()?.edit()?.putBoolean(KEY_ADS_ENABLED, enabled)?.apply()
    }

    fun setSliderStepped(enabled: Boolean) {
        _isSliderStepped.value = enabled
        getPrefs()?.edit()?.putBoolean(KEY_SLIDER_STEPPED, enabled)?.apply()
    }

    fun setNotifControlsEnabled(enabled: Boolean) {
        _isNotifControlsEnabled.value = enabled
        getPrefs()?.edit()?.putBoolean(KEY_NOTIF_CONTROLS_ENABLED, enabled)?.apply()
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        _hasSeenOnboarding.value = seen
        getPrefs()?.edit()?.putBoolean(KEY_HAS_SEEN_ONBOARDING, seen)?.apply()
    }

    fun setHearingWarningDisabled(disabled: Boolean) {
        _isHearingWarningDisabled.value = disabled
        getPrefs()?.edit()?.putBoolean(KEY_HEARING_WARNING_DISABLED, disabled)?.apply()
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        getPrefs()?.edit()?.putBoolean(KEY_DARK_THEME, isDark)?.apply()
    }

    fun hideHearingWarningFor7Days() {
        val until = System.currentTimeMillis() + (7L * 24L * 60L * 60L * 1000L)
        _hearingWarningHiddenUntil.value = until
        getPrefs()?.edit()?.putLong(KEY_HEARING_WARNING_HIDDEN_UNTIL, until)?.apply()
    }

    fun setBandLevel(bandIndex: Int, dBLevel: Int) {
        val bands = _eqBands.value.clone()
        if (bandIndex in bands.indices) {
            bands[bandIndex] = dBLevel
            _eqBands.value = bands
            _eqPreset.value = "Custom"
            
            getPrefs()?.edit()?.apply {
                putString(KEY_BANDS, bands.joinToString(","))
                putString(KEY_PRESET, "Custom")
            }?.apply()

            try {
                equalizer?.setBandLevel(bandIndex.toShort(), (dBLevel * 100).toShort())
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun applyPreset(presetName: String) {
        _eqPreset.value = presetName
        val levels = when (presetName) {
            "Bass Booster" -> intArrayOf(8, 5, 2, 0, 0)
            "Vocal Booster" -> intArrayOf(-2, 1, 4, 3, -1)
            "Rock" -> intArrayOf(4, 2, -1, 2, 5)
            "Pop" -> intArrayOf(-1, 2, 5, 1, -2)
            "Jazz" -> intArrayOf(3, 2, 1, 2, -1)
            "Flat" -> intArrayOf(0, 0, 0, 0, 0)
            else -> return
        }
        _eqBands.value = levels
        getPrefs()?.edit()?.apply {
            putString(KEY_BANDS, levels.joinToString(","))
            putString(KEY_PRESET, presetName)
        }?.apply()

        val eq = equalizer
        if (eq != null) {
            try {
                for (i in levels.indices) {
                    eq.setBandLevel(i.toShort(), (levels[i] * 100).toShort())
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun startSilencePlayback() {
        if (isPlayingSilence) return
        isPlayingSilence = true
        try {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBufSize > 0) minBufSize else 4096

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.play()
                audioTrack = track

                CoroutineScope(Dispatchers.IO).launch {
                    val silenceBuffer = ShortArray(bufferSize)
                    while (isPlayingSilence) {
                        try {
                            val activeTrack = audioTrack
                            if (activeTrack != null && activeTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                activeTrack.write(silenceBuffer, 0, silenceBuffer.size)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                        delay(100)
                    }
                }
            } else {
                try {
                    track.release()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                isPlayingSilence = false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            isPlayingSilence = false
        }
    }

    private fun stopSilencePlayback() {
        isPlayingSilence = false
        try {
            audioTrack?.stop()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            audioTrack?.release()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        audioTrack = null
    }
}
