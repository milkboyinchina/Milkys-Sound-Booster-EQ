package com.milkys.soundbooster

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * POWER bounce regression: OFF→ON after a stop must stick.
 * Root cause was release() nulling the persist context (writes silently dropped)
 * plus init() (service onCreate) clobbering the live flow with stale prefs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoosterToggleBounceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("volume_booster_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        AudioEffectManager.resetInitForTest()
        AudioEffectManager.init(context)
    }

    @Test
    fun `write after release still persists`() {
        AudioEffectManager.setBoostEnabled(true)
        assertTrue(AudioEffectManager.isBoostEnabled.value)
        AudioEffectManager.release()
        AudioEffectManager.setBoostEnabled(false)
        AudioEffectManager.setBoostEnabled(true)
        assertTrue(AudioEffectManager.isBoostEnabled.value)
        val persisted = context.getSharedPreferences("volume_booster_prefs", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)
        assertTrue(persisted)
    }

    @Test
    fun `reinit never clobbers live boost state`() {
        AudioEffectManager.setBoostEnabled(true)
        assertTrue(AudioEffectManager.isBoostEnabled.value)
        // Simulate the write racing the shutdown: disk stale, memory is source of truth.
        context.getSharedPreferences("volume_booster_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("enabled", false).commit()
        AudioEffectManager.init(context) // service onCreate path
        assertTrue(AudioEffectManager.isBoostEnabled.value)
    }

    @Test
    fun `off on off on sequence ends on`() {
        AudioEffectManager.setBoostEnabled(true)
        AudioEffectManager.setBoostEnabled(false)
        assertFalse(AudioEffectManager.isBoostEnabled.value)
        AudioEffectManager.setBoostEnabled(true)
        AudioEffectManager.init(context)
        assertTrue(AudioEffectManager.isBoostEnabled.value)
    }
}
