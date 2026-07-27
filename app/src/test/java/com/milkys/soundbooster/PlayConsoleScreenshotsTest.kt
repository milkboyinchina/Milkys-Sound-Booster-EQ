package com.milkys.soundbooster

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.milkys.soundbooster.ui.theme.MyApplicationTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// ==============================================================================
// Play Console Screenshot Test Suite
// Generates screenshots for 3 device types:
//   phone_  = Pixel 8 Phone (1080x2400, ~430dpi)
//   tab_    = 7-inch Tablet (600dp wide, hdpi ~240dpi)
//   xltab_  = 10-inch Tablet (800dp wide, xhdpi ~320dpi)
// ==============================================================================

// ------------------------------------------------------------------------------
// PHONE screenshots — Pixel 8 (1080x2400px)
// ------------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PhoneScreenshotsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        AudioEffectManager.setHasSeenOnboarding(true)
        AudioEffectManager.setHearingWarningDisabled(false)
        AudioEffectManager.setBoostEnabled(true)
        AudioEffectManager.setBoostProgress(100)
    }

    @Test
    fun capture_phone_screenshots() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(onStartService = {}, onStopService = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/phone_01_decibel_volume_booster.png")
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/phone_03_hearing_speaker_warning.png")

        composeTestRule.onNode(hasScrollAction()).performTouchInput {
            swipeUp(startY = 1400f, endY = 200f)
        }
        Thread.sleep(800)
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/phone_02_5band_equalizer_presets.png")
    }
}

// ------------------------------------------------------------------------------
// 7-INCH TABLET screenshots — 600dp wide, hdpi (~240dpi)
// Play Console: "7-inch tablet" category
// ------------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Nexus7 qualifier: w600dp-h960dp-large-notlong-notround-any-xhdpi-keyshidden-nonav
@Config(qualifiers = RobolectricDeviceQualifiers.Nexus7, sdk = [36])
class TabletScreenshotsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        AudioEffectManager.setHasSeenOnboarding(true)
        AudioEffectManager.setHearingWarningDisabled(false)
        AudioEffectManager.setBoostEnabled(true)
        AudioEffectManager.setBoostProgress(100)
    }

    @Test
    fun capture_tab_screenshots() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(onStartService = {}, onStopService = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/tab_01_decibel_volume_booster.png")
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/tab_03_hearing_speaker_warning.png")

        // Tablet may fit all content on screen (no scroll node needed); try scroll then capture.
        // Nexus7: 960dp × 2.0 (xhdpi) = 1920px tall.
        try {
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = 1700f, endY = 200f)
            }
            Thread.sleep(800)
        } catch (_: Throwable) { /* content fits on screen — no scroll needed */ }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/tab_02_5band_equalizer_presets.png")
    }
}

// ------------------------------------------------------------------------------
// 10-INCH TABLET screenshots — 800dp wide, xhdpi (~320dpi)
// Play Console: "10-inch tablet" category
// ------------------------------------------------------------------------------
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Nexus9 portrait: w768dp-h1024dp-xlarge-notlong-notround-any-xhdpi-keyshidden-nonav
@Config(qualifiers = "w768dp-h1024dp-xlarge-notlong-notround-any-xhdpi-keyshidden-nonav", sdk = [36])
class XlTabletScreenshotsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        AudioEffectManager.setHasSeenOnboarding(true)
        AudioEffectManager.setHearingWarningDisabled(false)
        AudioEffectManager.setBoostEnabled(true)
        AudioEffectManager.setBoostProgress(100)
    }

    @Test
    fun capture_xltab_screenshots() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(onStartService = {}, onStopService = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/xltab_01_decibel_volume_booster.png")
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/xltab_03_hearing_speaker_warning.png")

        // 10-inch tablet: 1024dp × 2.0 (xhdpi) = 2048px tall.
        try {
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = 1800f, endY = 200f)
            }
            Thread.sleep(800)
        } catch (_: Throwable) { /* content fits on screen — no scroll needed */ }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/xltab_02_5band_equalizer_presets.png")
    }
}
