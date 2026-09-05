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
import android.media.AudioAttributes
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.milkys.soundbooster.data.PreferencesRepository

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
    private const val KEY_FAVORITE_PRESETS = "favorite_presets_set"
    private const val KEY_AD_CONSENT_STATUS = "ad_consent_status"
    private const val KEY_PERSONALIZED_ADS_CONSENT = "personalized_ads_consent"
    private const val KEY_EQ_ENABLED = "eq_enabled"

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

    private val _adConsentStatus = MutableStateFlow("UNKNOWN") // "UNKNOWN", "GRANTED", "DENIED"
    val adConsentStatus: StateFlow<String> = _adConsentStatus

    private val _isPersonalizedAdsConsent = MutableStateFlow(true)
    val isPersonalizedAdsConsent: StateFlow<Boolean> = _isPersonalizedAdsConsent

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

    private val _favoritePresets = MutableStateFlow<Set<String>>(emptySet())
    val favoritePresets: StateFlow<Set<String>> = _favoritePresets

    // 5 standard equalizer bands: 60Hz, 230Hz, 910Hz, 4kHz, 14kHz
    private val _eqBands = MutableStateFlow(intArrayOf(0, 0, 0, 0, 0)) // levels in dB (-15 to +15)
    val eqBands: StateFlow<IntArray> = _eqBands

    private val _isEqEnabled = MutableStateFlow(false)
    val isEqEnabled: StateFlow<Boolean> = _isEqEnabled

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
    @Volatile private var isPlayingSilence = false
    private var silenceJob: Job? = null
    private var audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val TAG = "AudioEffectManager"

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
        val consentStatus = prefs.getString(KEY_AD_CONSENT_STATUS, "UNKNOWN") ?: "UNKNOWN"
        val personalizedConsent = prefs.getBoolean(KEY_PERSONALIZED_ADS_CONSENT, true)
        val sliderStepped = prefs.getBoolean(KEY_SLIDER_STEPPED, true)
        val notifControlsEnabled = prefs.getBoolean(KEY_NOTIF_CONTROLS_ENABLED, true)
        val hasSeenOnboarding = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
        val warningDisabled = prefs.getBoolean(KEY_HEARING_WARNING_DISABLED, false)
        val warningHiddenUntil = prefs.getLong(KEY_HEARING_WARNING_HIDDEN_UNTIL, 0L)
        val darkTheme = prefs.getBoolean(KEY_DARK_THEME, true)
        val appLanguage = prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
        val defaultPreset = prefs.getString(KEY_DEFAULT_PRESET, "Flat") ?: "Flat"
        val customPresetsJson = prefs.getString(KEY_CUSTOM_PRESETS, "") ?: ""
        val favoritePresetsSet = prefs.getStringSet(KEY_FAVORITE_PRESETS, emptySet()) ?: emptySet()
        val eqEnabled = prefs.getBoolean(KEY_EQ_ENABLED, false)

        _isBoostEnabled.value = enabled
        _boostProgress.value = boost
        _isFloatingEnabled.value = floating
        _isAdsEnabled.value = adsEnabled
        _adConsentStatus.value = consentStatus
        _isPersonalizedAdsConsent.value = personalizedConsent

        _isSliderStepped.value = sliderStepped
        _isNotifControlsEnabled.value = notifControlsEnabled
        _hasSeenOnboarding.value = hasSeenOnboarding
        _isHearingWarningDisabled.value = warningDisabled
        _hearingWarningHiddenUntil.value = warningHiddenUntil
        _isEqEnabled.value = eqEnabled
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
        _favoritePresets.value = favoritePresetsSet

        val activePreset = if (preset.isNotEmpty()) preset else defaultPreset
        _eqPreset.value = activePreset

        val bands = bandsStr.split(",").map { it.toIntOrNull() ?: 0 }.toIntArray()
        if (bands.size == 5 && preset == "Custom") {
            _eqBands.value = bands
        } else {
            val presetBands = getPresetBands(activePreset) ?: intArrayOf(0, 0, 0, 0, 0)
            _eqBands.value = presetBands
        }

        // Initialize actual audio effects if enabled — ensure silence track before effects binding
        if (enabled) {
            startSilencePlayback()
            // initEffects will run after track is initialized (called again from startSilencePlayback if needed)
            initEffects()
        }

        checkBatterySaverState()
        // One-time migration to DataStore (IO, non-blocking)
        audioScope.launch { try { PreferencesRepository.migrateFromSharedPrefs(appContext) } catch (e: Exception) { Log.w(TAG, "DataStore migration failed: ${e.message}") } }
    }

    private fun getPrefs() = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // DataStore helpers — dual-write to SharedPreferences (sync compat) + DataStore (async, IO)
    private fun persistBoolean(key: String, value: Boolean, dsKey: androidx.datastore.preferences.core.Preferences.Key<Boolean>) {
        getPrefs()?.edit()?.putBoolean(key, value)?.apply()
        val ctx = context ?: return
        audioScope.launch { try { PreferencesRepository.putBoolean(ctx, dsKey, value) } catch (e: Exception) { Log.w(TAG, "DataStore putBoolean failed: ${e.message}") } }
    }
    private fun persistInt(key: String, value: Int, dsKey: androidx.datastore.preferences.core.Preferences.Key<Int>) {
        getPrefs()?.edit()?.putInt(key, value)?.apply()
        val ctx = context ?: return
        audioScope.launch { try { PreferencesRepository.putInt(ctx, dsKey, value) } catch (e: Exception) { Log.w(TAG, "DataStore putInt failed: ${e.message}") } }
    }
    private fun persistLong(key: String, value: Long, dsKey: androidx.datastore.preferences.core.Preferences.Key<Long>) {
        getPrefs()?.edit()?.putLong(key, value)?.apply()
        val ctx = context ?: return
        audioScope.launch { try { PreferencesRepository.putLong(ctx, dsKey, value) } catch (e: Exception) { Log.w(TAG, "DataStore putLong failed: ${e.message}") } }
    }
    private fun persistString(key: String, value: String, dsKey: androidx.datastore.preferences.core.Preferences.Key<String>) {
        getPrefs()?.edit()?.putString(key, value)?.apply()
        val ctx = context ?: return
        audioScope.launch { try { PreferencesRepository.putString(ctx, dsKey, value) } catch (e: Exception) { Log.w(TAG, "DataStore putString failed: ${e.message}") } }
    }
    private fun persistStringSet(key: String, value: Set<String>, dsKey: androidx.datastore.preferences.core.Preferences.Key<Set<String>>) {
        getPrefs()?.edit()?.putStringSet(key, value)?.apply()
        val ctx = context ?: return
        audioScope.launch { try { PreferencesRepository.putStringSet(ctx, dsKey, value) } catch (e: Exception) { Log.w(TAG, "DataStore putStringSet failed: ${e.message}") } }
    }

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
                    if (sessionId > 0) {
                        try { LoudnessEnhancer(sessionId) } catch (inner: Throwable) { null }
                    } else null
                }
                loudnessEnhancer?.apply {
                    try { enabled = _isBoostEnabled.value } catch (t: Throwable) { Log.w(TAG, "enable enhancer failed: ${t.message}") }
                    try { setTargetGain(mapProgressToGain(_boostProgress.value)) } catch (t: Throwable) { Log.w(TAG, "setTargetGain failed: ${t.message}") }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "LoudnessEnhancer init failed: ${e.message}")
            loudnessEnhancer = null
        }

        try {
            if (equalizer == null) {
                equalizer = try {
                    Equalizer(0, 0)
                } catch (t: Throwable) {
                    if (sessionId > 0) {
                        try { Equalizer(0, sessionId) } catch (inner: Throwable) { null }
                    } else null
                }
                equalizer?.apply {
                    try { enabled = _isBoostEnabled.value } catch (t: Throwable) { Log.w(TAG, "enable equalizer failed: ${t.message}") }
                    applySavedBands()
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Equalizer init failed: ${e.message}")
            equalizer = null
        }
    }

    private fun releaseEffects() {
        try {
            loudnessEnhancer?.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to release enhancer: ${e.message}")
        }
        try {
            equalizer?.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to release equalizer: ${e.message}")
        }
        loudnessEnhancer = null
        equalizer = null
    }

    private fun mapProgressToGain(progress: Int): Int {
        // Progress (0 to 100%) maps to 0 to 1500 millibels (+15 dB) per AGENTS 5.2 safety limit (max 200% / +15-20dB).
        // Clamped to 1500 mB to prevent hearing damage / speaker burnout; was previously 3000 mB (+30dB).
        return (progress * 15).coerceIn(0, 1500)
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

    @Synchronized
    fun setBoostEnabled(enabled: Boolean) {
        if (enabled == _isBoostEnabled.value) {
            // Already in desired state, but ensure effects are in correct state (handle race on rapid toggle)
            if (enabled) {
                try { loudnessEnhancer?.enabled = true } catch (e: Throwable) { Log.w(TAG, "setBoostEnabled noop enable enhancer: ${e.message}") }
                try { equalizer?.enabled = true } catch (e: Throwable) { Log.w(TAG, "setBoostEnabled noop enable eq: ${e.message}") }
                if (audioTrack == null || !isPlayingSilence) {
                    startSilencePlayback()
                    initEffects()
                    try { loudnessEnhancer?.enabled = true } catch (e: Throwable) { Log.w(TAG, "retry enable enhancer: ${e.message}") }
                    try { equalizer?.enabled = true } catch (e: Throwable) { Log.w(TAG, "retry enable eq: ${e.message}") }
                }
            }
            return
        }
        _isBoostEnabled.value = enabled
        persistBoolean(KEY_ENABLED, enabled, PreferencesRepository.KEY_ENABLED)

        if (enabled) {
            // If previous disable just happened, ensure old track is fully released before re-creating
            if (audioTrack != null) {
                Log.w(TAG, "setBoostEnabled(true) with stale audioTrack, forcing release")
                stopSilencePlayback()
                releaseEffects()
            }
            startSilencePlayback()
            initEffects()
            try {
                loudnessEnhancer?.enabled = true
            } catch (e: Throwable) {
                Log.w(TAG, "enable enhancer failed: ${e.message}")
                // Retry after init if track was just created
                try { loudnessEnhancer?.enabled = true } catch (inner: Throwable) { Log.w(TAG, "retry enable enhancer 2: ${inner.message}") }
            }
            try {
                equalizer?.enabled = true
            } catch (e: Throwable) {
                Log.w(TAG, "enable equalizer failed: ${e.message}")
            }
            // Verify hardware actually enabled, if not, retry once
            if (loudnessEnhancer?.enabled == false) {
                Log.w(TAG, "loudnessEnhancer still disabled after enable, retrying init")
                initEffects()
                try { loudnessEnhancer?.enabled = true } catch (e: Throwable) { Log.w(TAG, "final retry enable: ${e.message}") }
            }
        } else {
            try {
                loudnessEnhancer?.enabled = false
            } catch (e: Throwable) {
                Log.w(TAG, "disable enhancer: ${e.message}")
            }
            try {
                equalizer?.enabled = false
            } catch (e: Throwable) {
                Log.w(TAG, "disable eq: ${e.message}")
            }
            releaseEffects()
            stopSilencePlayback()
        }
    }

    fun setBoostProgress(progress: Int) {
        _boostProgress.value = progress
        persistInt(KEY_BOOST, progress, PreferencesRepository.KEY_BOOST)

        try {
            loudnessEnhancer?.setTargetGain(mapProgressToGain(progress))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setFloatingEnabled(enabled: Boolean) {
        _isFloatingEnabled.value = enabled
        persistBoolean(KEY_FLOATING, enabled, PreferencesRepository.KEY_FLOATING)
    }

    fun setAdsEnabled(enabled: Boolean) {
        _isAdsEnabled.value = enabled
        persistBoolean(KEY_ADS_ENABLED, enabled, PreferencesRepository.KEY_ADS_ENABLED)
    }

    fun setAdConsentStatus(status: String) {
        _adConsentStatus.value = status
        persistString(KEY_AD_CONSENT_STATUS, status, PreferencesRepository.KEY_AD_CONSENT_STATUS)
    }

    fun setPersonalizedAdsConsent(enabled: Boolean) {
        _isPersonalizedAdsConsent.value = enabled
        persistBoolean(KEY_PERSONALIZED_ADS_CONSENT, enabled, PreferencesRepository.KEY_PERSONALIZED_ADS_CONSENT)
    }

    fun setSliderStepped(enabled: Boolean) {
        _isSliderStepped.value = enabled
        persistBoolean(KEY_SLIDER_STEPPED, enabled, PreferencesRepository.KEY_SLIDER_STEPPED)
    }

    fun setNotifControlsEnabled(enabled: Boolean) {
        _isNotifControlsEnabled.value = enabled
        persistBoolean(KEY_NOTIF_CONTROLS_ENABLED, enabled, PreferencesRepository.KEY_NOTIF_CONTROLS_ENABLED)
    }

    fun setHasSeenOnboarding(seen: Boolean) {
        _hasSeenOnboarding.value = seen
        persistBoolean(KEY_HAS_SEEN_ONBOARDING, seen, PreferencesRepository.KEY_HAS_SEEN_ONBOARDING)
    }

    fun setEqEnabled(enabled: Boolean) {
        _isEqEnabled.value = enabled
        persistBoolean(KEY_EQ_ENABLED, enabled, PreferencesRepository.KEY_EQ_ENABLED)
        if (enabled) {
            // Apply current bands when enabling
            equalizer?.let { eq ->
                audioScope.launch {
                    try {
                        val range = try { eq.getBandLevelRange() } catch (e: Throwable) { null }
                        val bands = _eqBands.value
                        for (i in bands.indices) {
                            val mB = (bands[i] * 100).coerceIn(range?.get(0)?.toInt() ?: -1500, range?.get(1)?.toInt() ?: 1500)
                            eq.setBandLevel(i.toShort(), mB.toShort())
                        }
                        eq.enabled = true
                    } catch (e: Throwable) { android.util.Log.w(TAG, "setEqEnabled true failed: ${e.message}") }
                }
            }
        } else {
            try { equalizer?.enabled = false } catch (e: Throwable) { android.util.Log.w(TAG, "setEqEnabled false failed: ${e.message}") }
        }
    }

    fun setHearingWarningDisabled(disabled: Boolean) {
        _isHearingWarningDisabled.value = disabled
        persistBoolean(KEY_HEARING_WARNING_DISABLED, disabled, PreferencesRepository.KEY_HEARING_WARNING_DISABLED)
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        persistBoolean(KEY_DARK_THEME, isDark, PreferencesRepository.KEY_DARK_THEME)
    }

    fun setAppLanguage(language: String) {
        _appLanguage.value = language
        persistString(KEY_APP_LANGUAGE, language, PreferencesRepository.KEY_APP_LANGUAGE)
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
        persistLong(KEY_HEARING_WARNING_HIDDEN_UNTIL, until, PreferencesRepository.KEY_HEARING_WARNING_HIDDEN_UNTIL)
    }

    fun setBandLevel(bandIndex: Int, dBLevel: Int) {
        val bands = _eqBands.value.clone()
        if (bandIndex in bands.indices) {
            bands[bandIndex] = dBLevel
            _eqBands.value = bands
            _eqPreset.value = "Custom"
            
            persistString(KEY_BANDS, bands.joinToString(","), PreferencesRepository.KEY_BANDS)
            persistString(KEY_PRESET, "Custom", PreferencesRepository.KEY_PRESET)

            // Always update UI (already done via _eqBands), defer hardware to IO with range clamp
            audioScope.launch {
                try {
                    val eq = equalizer ?: return@launch
                    val range = try { eq.getBandLevelRange() } catch (e: Throwable) { null }
                    val mB = (dBLevel * 100).coerceIn(range?.get(0)?.toInt() ?: -1500, range?.get(1)?.toInt() ?: 1500)
                    eq.setBandLevel(bandIndex.toShort(), mB.toShort())
                } catch (e: Throwable) {
                    Log.w(TAG, "setBandLevel failed band $bandIndex level $dBLevel: ${e.message}")
                }
            }
        }
    }

    fun getPresetBands(presetName: String): IntArray? {
        val arr = BUILT_IN_PRESETS[presetName] ?: _customPresets.value[presetName]
        return arr?.clone()
    }

    fun applyPreset(presetName: String) {
        val levels = getPresetBands(presetName) ?: return
        _eqPreset.value = presetName
        _eqBands.value = levels.clone()
        persistString(KEY_BANDS, levels.joinToString(","), PreferencesRepository.KEY_BANDS)
        persistString(KEY_PRESET, presetName, PreferencesRepository.KEY_PRESET)

        val eq = equalizer
        if (eq != null) {
            audioScope.launch {
                try {
                    val range = try { eq.getBandLevelRange() } catch (e: Throwable) { null }
                    for (i in levels.indices) {
                        val mB = (levels[i] * 100).coerceIn(range?.get(0)?.toInt() ?: -1500, range?.get(1)?.toInt() ?: 1500)
                        eq.setBandLevel(i.toShort(), mB.toShort())
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "applyPreset failed preset $presetName: ${e.message}")
                }
            }
        }
    }

    fun toggleFavorite(name: String): Boolean {
        val current = _favoritePresets.value.toMutableSet()
        if (current.contains(name)) {
            current.remove(name)
            _favoritePresets.value = current
            saveFavoritesToPrefs()
            return true
        } else {
            if (current.size >= 4) {
                return false // Reached max 4 favorites limit
            }
            current.add(name)
            _favoritePresets.value = current
            saveFavoritesToPrefs()
            return true
        }
    }

    private fun saveFavoritesToPrefs() {
        persistStringSet(KEY_FAVORITE_PRESETS, _favoritePresets.value, PreferencesRepository.KEY_FAVORITE_PRESETS)
    }

    fun getMatchedPresetName(bands: IntArray = _eqBands.value): String? {
        for ((name, b) in BUILT_IN_PRESETS) {
            if (b.contentEquals(bands)) return name
        }
        for ((name, b) in _customPresets.value) {
            if (b.contentEquals(bands)) return name
        }
        return null
    }

    fun validateCustomPreset(name: String, bands: IntArray): String? {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || cleanName.length > 10) {
            return "Preset name must be between 1 and 10 characters."
        }
        if (_customPresets.value.size >= 7 && !_customPresets.value.containsKey(cleanName)) {
            return "Maximum 7 custom presets allowed."
        }
        if (BUILT_IN_PRESETS.containsKey(cleanName) || _customPresets.value.containsKey(cleanName)) {
            return "Preset name already exists."
        }
        val allPresets = BUILT_IN_PRESETS + _customPresets.value
        if (allPresets.values.any { it.contentEquals(bands) }) {
            return "Equalizer values match an existing preset."
        }
        return null
    }

    fun saveCustomPreset(name: String, bands: IntArray): Boolean {
        return saveCustomPresetWithResult(name, bands) == null
    }

    fun saveCustomPresetWithResult(name: String, bands: IntArray): String? {
        val err = validateCustomPreset(name, bands)
        if (err != null) return err
        val cleanName = name.trim()
        val newMap = _customPresets.value.toMutableMap()
        newMap[cleanName] = bands.clone()
        _customPresets.value = newMap
        _eqPreset.value = cleanName
        _eqBands.value = bands.clone()
        saveCustomPresetsToPrefs()
        
        persistString(KEY_BANDS, bands.joinToString(","), PreferencesRepository.KEY_BANDS)
        persistString(KEY_PRESET, cleanName, PreferencesRepository.KEY_PRESET)

        val eq = equalizer
        if (eq != null) {
            try {
                for (i in bands.indices) {
                    eq.setBandLevel(i.toShort(), (bands[i] * 100).toShort())
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "saveCustomPreset failed preset $cleanName: ${e.message}")
                }
            }
        return null
    }

    fun deleteCustomPreset(name: String) {
        deleteCustomPresets(setOf(name))
    }

    fun deleteCustomPresets(names: Set<String>) {
        val newMap = _customPresets.value.toMutableMap()
        var modified = false
        val favs = _favoritePresets.value.toMutableSet()
        var favsModified = false

        for (name in names) {
            if (newMap.containsKey(name)) {
                newMap.remove(name)
                modified = true
                if (favs.remove(name)) {
                    favsModified = true
                }
                if (_eqPreset.value == name) {
                    applyPreset(_defaultPreset.value)
                }
                if (_defaultPreset.value == name) {
                    setDefaultPreset("Flat")
                }
            }
        }
        if (modified) {
            _customPresets.value = newMap
            saveCustomPresetsToPrefs()
        }
        if (favsModified) {
            _favoritePresets.value = favs
            saveFavoritesToPrefs()
        }
    }

    fun setDefaultPreset(name: String) {
        _defaultPreset.value = name
        persistString(KEY_DEFAULT_PRESET, name, PreferencesRepository.KEY_DEFAULT_PRESET)
    }

    private fun saveCustomPresetsToPrefs() {
        try {
            val jsonObj = org.json.JSONObject()
            for ((key, value) in _customPresets.value) {
                val arr = org.json.JSONArray()
                for (b in value) arr.put(b)
                jsonObj.put(key, arr)
            }
            persistString(KEY_CUSTOM_PRESETS, jsonObj.toString(), PreferencesRepository.KEY_CUSTOM_PRESETS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportPreset(name: String): String {
        val bands = getPresetBands(name) ?: _eqBands.value
        val json = org.json.JSONObject()
        json.put("name", name)
        val arr = org.json.JSONArray()
        for (b in bands) arr.put(b)
        json.put("values", arr)
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
            item.put("values", arr)
            item.put("isCustom", _customPresets.value.containsKey(name))
            presetsArr.put(item)
        }
        root.put("presets", presetsArr)
        root.put("defaultPreset", _defaultPreset.value)
        root.put("currentPreset", _eqPreset.value)
        return root.toString(2)
    }

    fun importPreset(jsonStr: String): String? {
        return importPresetWithResult(jsonStr).first
    }

    fun importPresetWithResult(jsonStr: String): Pair<String?, String?> {
        try {
            val json = org.json.JSONObject(jsonStr.trim())
            if (json.has("presets")) {
                val presetsArr = json.getJSONArray("presets")
                var lastImported: String? = null
                var lastError: String? = null
                var count = 0
                for (i in 0 until presetsArr.length()) {
                    val item = presetsArr.getJSONObject(i)
                    val name = item.getString("name")
                    val arrKey = if (item.has("values")) "values" else "bands"
                    val bandsArr = item.getJSONArray(arrKey)
                    if (bandsArr.length() == 5) {
                        val bands = IntArray(5)
                        for (j in 0 until 5) bands[j] = bandsArr.getInt(j).coerceIn(-15, 15)
                        val err = saveCustomPresetWithResult(name, bands)
                        if (err == null) {
                            lastImported = name
                            count++
                        } else {
                            lastError = err
                        }
                    }
                }
                if (json.has("defaultPreset")) {
                    val def = json.getString("defaultPreset")
                    if (BUILT_IN_PRESETS.containsKey(def) || _customPresets.value.containsKey(def)) {
                        setDefaultPreset(def)
                    }
                }
                return if (count > 0) Pair(lastImported, null) else Pair(null, lastError ?: "Invalid preset format")
            } else {
                val arrKey = if (json.has("values")) "values" else if (json.has("bands")) "bands" else null
                if (arrKey != null) {
                    val name = json.optString("name", "Imported").trim()
                    val bandsArr = json.getJSONArray(arrKey)
                    if (bandsArr.length() == 5) {
                        val bands = IntArray(5)
                        for (j in 0 until 5) bands[j] = bandsArr.getInt(j).coerceIn(-15, 15)
                        val err = saveCustomPresetWithResult(name, bands)
                        return if (err == null) Pair(name, null) else Pair(null, err)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(null, "Invalid preset JSON format.")
        }
        return Pair(null, "Invalid preset JSON format.")
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

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.play()
                audioTrack = track

                silenceJob?.cancel()
                silenceJob = audioScope.launch {
                    val silenceBuffer = ShortArray(bufferSize / 2)
                    while (isPlayingSilence) {
                        try {
                            val activeTrack = audioTrack
                            if (activeTrack != null && activeTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                activeTrack.write(silenceBuffer, 0, silenceBuffer.size)
                            }
                        } catch (e: Throwable) {
                            Log.w(TAG, "Silence write failed: ${e.message}")
                        }
                        delay(100)
                    }
                }
            } else {
                try {
                    track.release()
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to release uninitialized track: ${e.message}")
                }
                isPlayingSilence = false
            }
        } catch (e: Throwable) {
            Log.w(TAG, "startSilencePlayback failed: ${e.message}")
            isPlayingSilence = false
        }
    }

    private fun stopSilencePlayback() {
        isPlayingSilence = false
        silenceJob?.cancel()
        silenceJob = null
        try {
            // stop() throws IllegalStateException if not playing — guard via playState
            if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.stop()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to stop track: ${e.message}")
        }
        try {
            audioTrack?.release()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to release track: ${e.message}")
        }
        audioTrack = null
    }

    /**
     * Release all audio resources. Call from Service.onDestroy / Activity.onDestroy
     * to prevent AudioEffect memory leaks and audio server crashes (AGENTS 5.2).
     */
    fun release() {
        try {
            loudnessEnhancer?.enabled = false
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to disable enhancer: ${e.message}")
        }
        try {
            equalizer?.enabled = false
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to disable equalizer: ${e.message}")
        }
        releaseEffects()
        stopSilencePlayback()
        // Cancel singleton scope to avoid thread-pool leak (B-002)
        try {
            audioScope.cancel()
        } catch (e: Throwable) {
            Log.w(TAG, "audioScope cancel failed: ${e.message}")
        }
        audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        context = null
    }
}
