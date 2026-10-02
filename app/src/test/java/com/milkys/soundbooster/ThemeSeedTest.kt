package com.milkys.soundbooster

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Q2-B: fresh installs seed theme from the system night mode; a persisted
 * choice always wins afterwards.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeSeedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("volume_booster_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        AudioEffectManager.resetInitForTest()
    }

    private fun systemIsNight(): Boolean {
        val mask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mask != Configuration.UI_MODE_NIGHT_NO
    }

    @Test
    fun `fresh install follows system theme`() {
        AudioEffectManager.init(context)
        assertEquals(systemIsNight(), AudioEffectManager.isDarkTheme.value)
    }

    @Test
    fun `persisted choice wins over system`() {
        context.getSharedPreferences("volume_booster_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("dark_theme", !systemIsNight()).commit()
        AudioEffectManager.init(context)
        assertEquals(!systemIsNight(), AudioEffectManager.isDarkTheme.value)
    }

    @Test
    fun `header toggle persists and survives reinit`() {
        AudioEffectManager.init(context)
        AudioEffectManager.setDarkTheme(!systemIsNight())
        AudioEffectManager.init(context) // service-restart path must not clobber
        assertEquals(!systemIsNight(), AudioEffectManager.isDarkTheme.value)
    }
}
