package com.milkys.soundbooster

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.milkys.soundbooster.ui.components.PresetManagerCard
import com.milkys.soundbooster.ui.theme.AppColors
import com.milkys.soundbooster.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h1200dp-mdpi", sdk = [34])
class EqualizerFontScaleTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AudioEffectManager.resetInitForTest()
        AudioEffectManager.init(context)
    }

    private fun setEqualizer(fontScale: Float, dark: Boolean = false, onBandChange: (Int, Int) -> Unit = { _, _ -> }) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                MyApplicationTheme(darkTheme = dark, dynamicColor = false) {
                    EqualizerComponent(
                        isEnabled = true,
                        eqBands = intArrayOf(0, 0, 0, 0, 0),
                        customPresets = emptyMap(),
                        cardColor = if (dark) AppColors.DarkCard else AppColors.LightCard,
                        borderDivider = if (dark) AppColors.BorderDark else AppColors.BorderLight,
                        textPrimary = if (dark) AppColors.DarkTextPrimary else AppColors.LightTextPrimary,
                        textSecondary = if (dark) AppColors.DarkTextSecondary else AppColors.LightTextSecondary,
                        primaryAccent = if (dark) AppColors.PrimaryAccentDark else AppColors.PrimaryAccentLight,
                        onBandChange = onBandChange,
                        isDarkTheme = dark,
                    )
                }
            }
        }
    }

    private fun setPresetManager(dark: Boolean = false) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                MyApplicationTheme(darkTheme = dark, dynamicColor = false) {
                    PresetManagerCard(
                        isEnabled = true,
                        currentPreset = "Flat",
                        defaultPreset = "Flat",
                        customPresets = mapOf("MyPreset" to intArrayOf(1, 2, 3, 4, 5)),
                        favoritePresets = listOf("Flat"),
                        eqBands = intArrayOf(0, 0, 0, 0, 0),
                        cardColor = if (dark) AppColors.DarkCard else AppColors.LightCard,
                        borderDivider = if (dark) AppColors.BorderDark else AppColors.BorderLight,
                        textPrimary = if (dark) AppColors.DarkTextPrimary else AppColors.LightTextPrimary,
                        textSecondary = if (dark) AppColors.DarkTextSecondary else AppColors.LightTextSecondary,
                        primaryAccent = if (dark) AppColors.PrimaryAccentDark else AppColors.PrimaryAccentLight,
                        onApplyPreset = {},
                        onSaveCustomPreset = { _, _ -> true },
                        onDeleteCustomPreset = {},
                        onDeleteCustomPresets = {},
                        onToggleFavorite = { true },
                        onSetDefaultPreset = {},
                        onExportPreset = { "{}" },
                        onExportAllPresets = { "{}" },
                        onImportPreset = { "{}" },
                        isDarkTheme = dark,
                    )
                }
            }
        }
    }

    @Test
    fun `equalizer compact 1x light`() {
        setEqualizer(1.0f)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/eq-font-320-1.0-light.png")
    }

    @Test
    fun `equalizer compact 1_5x light`() {
        setEqualizer(1.5f)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/eq-font-320-1.5-light.png")
    }

    @Test
    fun `equalizer compact 2x light`() {
        setEqualizer(2.0f)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/eq-font-320-2.0-light.png")
    }

    @Test
    fun `equalizer compact 2x dark`() {
        setEqualizer(2.0f, dark = true)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/eq-font-320-2.0-dark.png")
    }

    @Test
    fun `plus button works at 2x with a 48dp target`() {
        var changedBand = -1
        var changedLevel = 0
        setEqualizer(2.0f) { band, level ->
            changedBand = band
            changedLevel = level
        }

        val description = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.content_desc_increase_band, 1)

        composeTestRule.onNodeWithContentDescription(description)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(0, changedBand)
        assertEquals(1, changedLevel)
    }

    @Test
    fun `preset actions 2x light`() {
        setPresetManager()
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/preset-actions-320-2.0-light.png")
    }

    @Test
    fun `preset actions 2x dark`() {
        setPresetManager(dark = true)
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/preset-actions-320-2.0-dark.png")
    }

    @Test
    fun `preset action buttons keep 48dp width at 2x`() {
        setPresetManager()
        val description = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.content_desc_save_preset)

        composeTestRule.onNodeWithContentDescription(description)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }
}
