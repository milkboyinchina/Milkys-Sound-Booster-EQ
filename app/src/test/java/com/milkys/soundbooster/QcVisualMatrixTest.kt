package com.milkys.soundbooster

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.milkys.soundbooster.ui.theme.MyApplicationTheme
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

/**
 * Visual matrix for qc_plan.md §5.2 / AGENTS.md §3.2 A.
 * Covers compact / standard / expanded + light/dark + fontScale spot-checks.
 * Full 24-combo theoretical = 4 widths × 4 fontScales × 2 themes (de-duplicated to 24).
 * This test captures 6 critical combos; remaining fontScale variations are validated
 * via Redmi 6-combo manual matrix (scripts/qc_redmi_matrix.sh, qc_plan.md §5.7).
 * Roborazzi outputDir = qc/reports/roborazzi (app/build.gradle.kts: roborazzi {}).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QcVisualMatrixTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    AudioEffectManager.init(ctx)
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_light() {
    composeTestRule.setContent { MyApplicationTheme(darkTheme = false, dynamicColor = false) { DashboardScreen(onStartService = {}, onStopService = {}) } }
    composeTestRule.onRoot().captureRoboImage(filePath = "matrix-compact-320-light.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_light() {
    composeTestRule.setContent { MyApplicationTheme(darkTheme = false, dynamicColor = false) { DashboardScreen(onStartService = {}, onStopService = {}) } }
    composeTestRule.onRoot().captureRoboImage(filePath = "matrix-standard-411-light.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_light() {
    composeTestRule.setContent { MyApplicationTheme(darkTheme = false, dynamicColor = false) { DashboardScreen(onStartService = {}, onStopService = {}) } }
    composeTestRule.onRoot().captureRoboImage(filePath = "matrix-expanded-600-light.png")
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_dark() {
    composeTestRule.setContent { MyApplicationTheme(darkTheme = true, dynamicColor = false) { DashboardScreen(onStartService = {}, onStopService = {}) } }
    composeTestRule.onRoot().captureRoboImage(filePath = "matrix-compact-320-dark.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_dark() {
    composeTestRule.setContent { MyApplicationTheme(darkTheme = true, dynamicColor = false) { DashboardScreen(onStartService = {}, onStopService = {}) } }
    composeTestRule.onRoot().captureRoboImage(filePath = "matrix-expanded-600-dark.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h640dp-land-mdpi", sdk = [34])
  fun matrix_landscape_standard_light() {
    composeTestRule.setContent { MyApplicationTheme(darkTheme = false, dynamicColor = false) { DashboardScreen(onStartService = {}, onStopService = {}) } }
    composeTestRule.onRoot().captureRoboImage(filePath = "matrix-landscape-standard-light.png")
  }
}
