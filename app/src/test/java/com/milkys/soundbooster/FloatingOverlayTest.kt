package com.milkys.soundbooster

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FloatingOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `verify preset abbreviation calculations`() {
        assertEquals("FL", getPresetAbbreviation("Flat"))
        assertEquals("BB", getPresetAbbreviation("Bass Booster"))
        assertEquals("VB", getPresetAbbreviation("Vocal Booster"))
        assertEquals("RK", getPresetAbbreviation("Rock"))
        assertEquals("PO", getPresetAbbreviation("Pop"))
        assertEquals("JZ", getPresetAbbreviation("Jazz"))
        assertEquals("DH", getPresetAbbreviation("Deep House"))
        assertEquals("SU", getPresetAbbreviation("Super"))
    }

    @Test
    fun `verify top 4 favorite presets selection logic`() {
        // Empty favorites fallback
        val emptyFavs = getTopFavoritePresets(emptySet())
        assertEquals(listOf("Flat", "Bass Booster", "Rock", "Pop"), emptyFavs)

        // Custom favorites prioritized
        val customFavs = getTopFavoritePresets(setOf("Rock", "CustomBass"))
        assertEquals(listOf("Rock", "CustomBass", "Flat", "Bass Booster"), customFavs)

        // Truncate to max 4 favorites
        val manyFavs = getTopFavoritePresets(setOf("Preset1", "Preset2", "Preset3", "Preset4", "Preset5"))
        assertEquals(4, manyFavs.size)
        assertEquals(listOf("Preset1", "Preset2", "Preset3", "Preset4"), manyFavs)
    }

    @Test
    fun `floating bubble renders collapsed state correctly`() {
        composeTestRule.setContent {
            FloatingBubble(
                isBoosted = true,
                boostProgress = 50,
                currentPreset = "Bass Booster",
                onClick = {},
                onDrag = { _, _ -> }
            )
        }
        composeTestRule.onNodeWithText("+50%").assertIsDisplayed()
    }

    @Test
    fun `floating dashboard renders expanded control panel correctly`() {
        composeTestRule.setContent {
            FloatingDashboard(
                isBoosted = true,
                boostProgress = 40,
                currentPreset = "Rock",
                favoritePresets = setOf("Rock", "Bass Booster"),
                onToggleBoost = {},
                onBoostChange = {},
                onPresetSelect = {},
                onOpenApp = {},
                onClose = {},
                onDisableOverlay = {}
            )
        }
        composeTestRule.onNodeWithText("Booster Overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Master Power State").assertIsDisplayed()
        composeTestRule.onNodeWithText("Boost Amplification").assertIsDisplayed()
        composeTestRule.onNodeWithText("+40%").assertIsDisplayed()
        composeTestRule.onNodeWithText("FAVORITE PRESETS").assertIsDisplayed()
        composeTestRule.onNodeWithText("RK").assertIsDisplayed()
    }

    @Test
    fun `verify overlay control setting state flow`() {
        AudioEffectManager.setFloatingEnabled(true)
        assertEquals(true, AudioEffectManager.isFloatingEnabled.value)

        AudioEffectManager.setFloatingEnabled(false)
        assertEquals(false, AudioEffectManager.isFloatingEnabled.value)
    }
}
