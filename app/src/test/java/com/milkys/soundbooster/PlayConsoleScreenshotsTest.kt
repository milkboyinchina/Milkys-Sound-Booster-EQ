package com.milkys.soundbooster

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class PlayConsoleScreenshotsTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AudioEffectManager.init(context)
    }

    private fun getScreenshotPath(filename: String): String {
        val screenshotDir = try {
            BuildConfig.SCREENSHOT_OUTPUT_DIR.ifEmpty { "screenshots" }
        } catch (e: Exception) {
            "screenshots"
        }
        val dir = File(screenshotDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return "${screenshotDir}/$filename"
    }

    @Test
    fun capture_all_app_screens() {
        composeTestRule.setContent {
            DashboardScreen(
                onStartService = {},
                onStopService = {}
            )
        }
        composeTestRule.waitForIdle()

        // 1. Capture Main Booster & Equalizer Screen (Dark Theme)
        composeTestRule.onRoot().captureRoboImage(filePath = getScreenshotPath("01_main_sound_booster_dark.png"))

        // 2. Toggle Theme to Light Theme and Capture
        composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = getScreenshotPath("02_main_sound_booster_light.png"))

        // 3. Switch back to Dark Theme and open Settings
        composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = getScreenshotPath("03_settings_dialog.png"))

        // 4. Click Open Source License button inside Settings
        composeTestRule.onNodeWithTag("open_source_license_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = getScreenshotPath("04_open_source_license_dialog.png"))

        // 5. Close Open Source License dialog and open Privacy Policy Terms
        composeTestRule.onNodeWithTag("close_license_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("privacy_terms_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage(filePath = getScreenshotPath("05_privacy_terms_dialog.png"))
    }
}
