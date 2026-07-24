package com.milkys.soundbooster

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
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_CUSTOM_PRESETS = "custom_presets_json"
    private const val KEY_DEFAULT_PRESET = "default_preset_name"

    val BUILT_IN_PRESETS = mapOf(
        "Flat" to intArrayOf(0, 0, 0, 0, 0),
        "Bass Booster" to intArrayOf(8, 5, 2, 0, 0),
        "Vocal Booster" to intArrayOf(-2, 1, 4, 3, -1),
        "Rock" to intArrayOf(4, 2, -1, 2, 5),
        "Pop" to intArrayOf(-1, 2, 5, 1, -2),
        "Jazz" to intArrayOf(3, 2, 1, 2, -1)
    )

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

    private val _appLanguage = MutableStateFlow("system")
    val appLanguage: StateFlow<String> = _appLanguage

    private val _eqPreset = MutableStateFlow("Flat")
    val eqPreset: StateFlow<String> = _eqPreset

    private val _defaultPreset = MutableStateFlow("Flat")
    val defaultPreset: StateFlow<String> = _defaultPreset

    private val _customPresets = MutableStateFlow<Map<String, IntArray>>(emptyMap())
    val customPresets: StateFlow<Map<String, IntArray>> = _customPresets

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
        val appLanguage = prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
        val defaultPreset = prefs.getString(KEY_DEFAULT_PRESET, "Flat") ?: "Flat"
        val customPresetsJson = prefs.getString(KEY_CUSTOM_PRESETS, "") ?: ""

        _isBoostEnabled.value = enabled
        _boostProgress.value = boost
        _isFloatingEnabled.value = floating
        _isAdsEnabled.value = adsEnabled
        _isSliderStepped.value = sliderStepped
        _isNotifControlsEnabled.value = notifControlsEnabled
        _hasSeenOnboarding.value = hasSeenOnboarding
        _isHearingWarningDisabled.value = warningDisabled
        _hearingWarningHiddenUntil.value = warningHiddenUntil
        _isDarkTheme.value = darkTheme
        _appLanguage.value = appLanguage
        _defaultPreset.value = defaultPreset

        // Parse custom presets from SharedPreferences
        val parsedCustom = mutableMapOf<String, IntArray>()
        if (customPresetsJson.isNotEmpty()) {
            try {
                val jsonObj = org.json.JSONObject(customPresetsJson)
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = jsonObj.getJSONArray(key)
                    if (arr.length() == 5) {
                        val bandsArr = IntArray(5)
                        for (i in 0 until 5) bandsArr[i] = arr.getInt(i)
                        parsedCustom[key] = bandsArr
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _customPresets.value = parsedCustom

        val activePreset = if (preset.isNotEmpty()) preset else defaultPreset
        _eqPreset.value = activePreset

        val bands = bandsStr.split(",").map { it.toIntOrNull() ?: 0 }.toIntArray()
        if (bands.size == 5 && preset == "Custom") {
            _eqBands.value = bands
        } else {
            val presetBands = getPresetBands(activePreset) ?: intArrayOf(0, 0, 0, 0, 0)
            _eqBands.value = presetBands
        }

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
        val sessionId = audioTrack?.audioSessionId ?: 0
        try {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = try {
                    LoudnessEnhancer(0)
                } catch (t: Throwable) {
                    if (sessionId > 0) LoudnessEnhancer(sessionId) else null
                }
                loudnessEnhancer?.apply {
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
                equalizer = try {
                    Equalizer(0, 0)
                } catch (t: Throwable) {
                    if (sessionId > 0) Equalizer(0, sessionId) else null
                }
                equalizer?.apply {
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

    fun setAppLanguage(language: String) {
        _appLanguage.value = language
        getPrefs()?.edit()?.putString(KEY_APP_LANGUAGE, language)?.apply()
        context?.let { ctx ->
            applyLanguageToApp(ctx, language)
        }
    }

    fun applyLanguageToApp(ctx: Context, languageTag: String) {
        val localeList = if (languageTag.isEmpty() || languageTag == "system") {
            androidx.core.os.LocaleListCompat.getEmptyLocaleList()
        } else {
            androidx.core.os.LocaleListCompat.forLanguageTags(languageTag)
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)

        try {
            val locale = if (languageTag.isEmpty() || languageTag == "system") {
                java.util.Locale.getDefault()
            } else {
                java.util.Locale.forLanguageTag(languageTag)
            }
            java.util.Locale.setDefault(locale)
            val resources = ctx.resources
            val config = resources.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun getPresetBands(presetName: String): IntArray? {
        return BUILT_IN_PRESETS[presetName] ?: _customPresets.value[presetName]
    }

    fun applyPreset(presetName: String) {
        val levels = getPresetBands(presetName) ?: return
        _eqPreset.value = presetName
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

    fun saveCustomPreset(name: String, bands: IntArray): Boolean {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return false
        val newMap = _customPresets.value.toMutableMap()
        newMap[cleanName] = bands.clone()
        _customPresets.value = newMap
        _eqPreset.value = cleanName
        _eqBands.value = bands.clone()
        saveCustomPresetsToPrefs()
        
        getPrefs()?.edit()?.apply {
            putString(KEY_BANDS, bands.joinToString(","))
            putString(KEY_PRESET, cleanName)
        }?.apply()

        val eq = equalizer
        if (eq != null) {
            try {
                for (i in bands.indices) {
                    eq.setBandLevel(i.toShort(), (bands[i] * 100).toShort())
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return true
    }

    fun deleteCustomPreset(name: String) {
        val newMap = _customPresets.value.toMutableMap()
        if (newMap.containsKey(name)) {
            newMap.remove(name)
            _customPresets.value = newMap
            saveCustomPresetsToPrefs()
            if (_eqPreset.value == name) {
                applyPreset(_defaultPreset.value)
            }
            if (_defaultPreset.value == name) {
                setDefaultPreset("Flat")
            }
        }
    }

    fun setDefaultPreset(name: String) {
        _defaultPreset.value = name
        getPrefs()?.edit()?.putString(KEY_DEFAULT_PRESET, name)?.apply()
    }

    private fun saveCustomPresetsToPrefs() {
        try {
            val jsonObj = org.json.JSONObject()
            for ((key, value) in _customPresets.value) {
                val arr = org.json.JSONArray()
                for (b in value) arr.put(b)
                jsonObj.put(key, arr)
            }
            getPrefs()?.edit()?.putString(KEY_CUSTOM_PRESETS, jsonObj.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportPreset(name: String): String {
        val bands = getPresetBands(name) ?: _eqBands.value
        val json = org.json.JSONObject()
        json.put("name", name)
        json.put("type", "sound_booster_preset")
        val arr = org.json.JSONArray()
        for (b in bands) arr.put(b)
        json.put("bands", arr)
        return json.toString(2)
    }

    fun exportAllPresets(): String {
        val root = org.json.JSONObject()
        val presetsArr = org.json.JSONArray()
        val all = BUILT_IN_PRESETS + _customPresets.value
        for ((name, bands) in all) {
            val item = org.json.JSONObject()
            item.put("name", name)
            val arr = org.json.JSONArray()
            for (b in bands) arr.put(b)
            item.put("bands", arr)
            item.put("isCustom", _customPresets.value.containsKey(name))
            presetsArr.put(item)
        }
        root.put("presets", presetsArr)
        root.put("defaultPreset", _defaultPreset.value)
        root.put("currentPreset", _eqPreset.value)
        return root.toString(2)
    }

    fun importPreset(jsonStr: String): String? {
        try {
            val json = org.json.JSONObject(jsonStr.trim())
            if (json.has("presets")) {
                val presetsArr = json.getJSONArray("presets")
                var lastImported = ""
                var count = 0
                for (i in 0 until presetsArr.length()) {
                    val item = presetsArr.getJSONObject(i)
                    val name = item.getString("name")
                    val bandsArr = item.getJSONArray("bands")
                    if (bandsArr.length() == 5) {
                        val bands = IntArray(5)
                        for (j in 0 until 5) bands[j] = bandsArr.getInt(j).coerceIn(-15, 15)
                        saveCustomPreset(name, bands)
                        lastImported = name
                        count++
                    }
                }
                if (json.has("defaultPreset")) {
                    val def = json.getString("defaultPreset")
                    if (BUILT_IN_PRESETS.containsKey(def) || _customPresets.value.containsKey(def)) {
                        setDefaultPreset(def)
                    }
                }
                return if (count > 0) lastImported else null
            } else if (json.has("bands")) {
                val name = json.optString("name", "Imported Preset")
                val bandsArr = json.getJSONArray("bands")
                if (bandsArr.length() == 5) {
                    val bands = IntArray(5)
                    for (j in 0 until 5) bands[j] = bandsArr.getInt(j).coerceIn(-15, 15)
                    saveCustomPreset(name, bands)
                    return name
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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
