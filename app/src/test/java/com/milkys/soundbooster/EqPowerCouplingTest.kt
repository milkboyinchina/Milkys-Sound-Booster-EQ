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
 * Q1-A: EQ requires the booster session. Enabling EQ while the booster is OFF
 * is rejected, and turning the booster OFF always turns EQ OFF with it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EqPowerCouplingTest {

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
    fun `eq cannot turn on while booster off`() {
        assertFalse(AudioEffectManager.isBoostEnabled.value)
        AudioEffectManager.setEqEnabled(true)
        assertFalse(AudioEffectManager.isEqEnabled.value)
    }

    @Test
    fun `booster off turns eq off`() {
        AudioEffectManager.setBoostEnabled(true)
        AudioEffectManager.setEqEnabled(true)
        assertTrue(AudioEffectManager.isEqEnabled.value)
        AudioEffectManager.setBoostEnabled(false)
        assertFalse(AudioEffectManager.isEqEnabled.value)
    }

    @Test
    fun `eq on off cycle with booster on`() {
        AudioEffectManager.setBoostEnabled(true)
        AudioEffectManager.setEqEnabled(true)
        assertTrue(AudioEffectManager.isEqEnabled.value)
        AudioEffectManager.setEqEnabled(false)
        assertFalse(AudioEffectManager.isEqEnabled.value)
        assertTrue(AudioEffectManager.isBoostEnabled.value)
    }
}
