package com.milkys.soundbooster.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val DATASTORE_NAME = "volume_booster_prefs"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

/**
 * PreferencesRepository — DataStore replacement for SharedPreferences (qc_plan.md §11, QC-008).
 * Provides type-safe keys, Flows for collectors, and suspend edit() on Dispatchers.IO.
 * Migration from SharedPreferences is handled externally by AudioEffectManager.init().
 */
object PreferencesRepository {

    // Keys mirroring AudioEffectManager PREFS
    val KEY_ENABLED = booleanPreferencesKey("enabled")
    val KEY_BOOST = intPreferencesKey("boost")
    val KEY_PRESET = stringPreferencesKey("preset")
    val KEY_BANDS = stringPreferencesKey("bands")
    val KEY_FLOATING = booleanPreferencesKey("floating")
    val KEY_ADS_ENABLED = booleanPreferencesKey("ads_enabled")
    val KEY_SLIDER_STEPPED = booleanPreferencesKey("slider_stepped")
    val KEY_NOTIF_CONTROLS_ENABLED = booleanPreferencesKey("notif_controls_enabled")
    val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    val KEY_HEARING_WARNING_DISABLED = booleanPreferencesKey("hearing_warning_disabled")
    val KEY_HEARING_WARNING_HIDDEN_UNTIL = longPreferencesKey("hearing_warning_hidden_until")
    val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    val KEY_CUSTOM_PRESETS = stringPreferencesKey("custom_presets_json")
    val KEY_DEFAULT_PRESET = stringPreferencesKey("default_preset_name")
    val KEY_FAVORITE_PRESETS = stringSetPreferencesKey("favorite_presets_set")
    val KEY_AD_CONSENT_STATUS = stringPreferencesKey("ad_consent_status")
    val KEY_PERSONALIZED_ADS_CONSENT = booleanPreferencesKey("personalized_ads_consent")
    val KEY_EQ_ENABLED = booleanPreferencesKey("eq_enabled")

    fun flowBoolean(context: Context, key: Preferences.Key<Boolean>, default: Boolean): Flow<Boolean> =
        context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[key] ?: default }

    fun flowInt(context: Context, key: Preferences.Key<Int>, default: Int): Flow<Int> =
        context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[key] ?: default }

    fun flowString(context: Context, key: Preferences.Key<String>, default: String): Flow<String> =
        context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[key] ?: default }

    fun flowLong(context: Context, key: Preferences.Key<Long>, default: Long): Flow<Long> =
        context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[key] ?: default }

    fun flowStringSet(context: Context, key: Preferences.Key<Set<String>>, default: Set<String>): Flow<Set<String>> =
        context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[key] ?: default }

    suspend fun getBoolean(context: Context, key: Preferences.Key<Boolean>, default: Boolean): Boolean =
        context.dataStore.data.map { it[key] ?: default }.first()

    suspend fun getInt(context: Context, key: Preferences.Key<Int>, default: Int): Int =
        context.dataStore.data.map { it[key] ?: default }.first()

    suspend fun getString(context: Context, key: Preferences.Key<String>, default: String): String =
        context.dataStore.data.map { it[key] ?: default }.first()

    suspend fun getLong(context: Context, key: Preferences.Key<Long>, default: Long): Long =
        context.dataStore.data.map { it[key] ?: default }.first()

    suspend fun getStringSet(context: Context, key: Preferences.Key<Set<String>>, default: Set<String>): Set<String> =
        context.dataStore.data.map { it[key] ?: default }.first()

    suspend fun putBoolean(context: Context, key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun putInt(context: Context, key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun putString(context: Context, key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun putLong(context: Context, key: Preferences.Key<Long>, value: Long) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun putStringSet(context: Context, key: Preferences.Key<Set<String>>, value: Set<String>) {
        context.dataStore.edit { it[key] = value }
    }

    /**
     * One-time migration from legacy SharedPreferences ("volume_booster_prefs") to DataStore.
     * Called from AudioEffectManager.init() on first run. Copies only keys that are non-default
     * in SharedPreferences but missing in DataStore (empty). Idempotent.
     */
    suspend fun migrateFromSharedPrefs(context: Context) {
        val prefs = context.getSharedPreferences("volume_booster_prefs", Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) return
        // Check if DataStore already has data (avoid double migration)
        val existing = context.dataStore.data.first()
        if (existing.asMap().isNotEmpty()) return

        context.dataStore.edit { ds ->
            prefs.all.forEach { (k, v) ->
                try {
                    when (v) {
                        is Boolean -> when (k) {
                            "enabled" -> ds[KEY_ENABLED] = v
                            "floating" -> ds[KEY_FLOATING] = v
                            "ads_enabled" -> ds[KEY_ADS_ENABLED] = v
                            "slider_stepped" -> ds[KEY_SLIDER_STEPPED] = v
                            "notif_controls_enabled" -> ds[KEY_NOTIF_CONTROLS_ENABLED] = v
                            "has_seen_onboarding" -> ds[KEY_HAS_SEEN_ONBOARDING] = v
                            "hearing_warning_disabled" -> ds[KEY_HEARING_WARNING_DISABLED] = v
                            "personalized_ads_consent" -> ds[KEY_PERSONALIZED_ADS_CONSENT] = v
                            "eq_enabled" -> ds[KEY_EQ_ENABLED] = v
                            "dark_theme" -> ds[KEY_DARK_THEME] = v
                        }
                        is Int -> when (k) {
                            "boost" -> ds[KEY_BOOST] = v
                        }
                        is Long -> when (k) {
                            "hearing_warning_hidden_until" -> ds[KEY_HEARING_WARNING_HIDDEN_UNTIL] = v
                        }
                        is String -> when (k) {
                            "preset" -> ds[KEY_PRESET] = v
                            "bands" -> ds[KEY_BANDS] = v
                            "app_language" -> ds[KEY_APP_LANGUAGE] = v
                            "custom_presets_json" -> ds[KEY_CUSTOM_PRESETS] = v
                            "default_preset_name" -> ds[KEY_DEFAULT_PRESET] = v
                            "ad_consent_status" -> ds[KEY_AD_CONSENT_STATUS] = v
                        }
                        is Set<*> -> if (k == "favorite_presets_set") {
                            @Suppress("UNCHECKED_CAST")
                            ds[KEY_FAVORITE_PRESETS] = v as Set<String>
                        }
                    }
                } catch (_: Exception) { /* skip unknown key */ }
            }
        }
    }
}
