package com.milkys.soundbooster

import androidx.compose.runtime.CompositionLocalProvider
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
 * Full 24-combo theoretical = 3 widths × 4 fontScales × 2 themes.
 * Roborazzi outputDir = qc/reports/roborazzi (app/build.gradle.kts: roborazzi {}).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QcVisualMatrixTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    AudioEffectManager.resetInitForTest()
    AudioEffectManager.init(ctx)
    AudioEffectManager.setHasSeenOnboarding(true)
    AudioEffectManager.setHearingWarningDisabled(true)
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_085_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 0.85f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-compact-320-0.85-light.png")
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_085_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 0.85f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-compact-320-0.85-dark.png")
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_10_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.0f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-compact-320-1.0-light.png")
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_10_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.0f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-compact-320-1.0-dark.png")
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_15_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-compact-320-1.5-light.png")
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_15_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-compact-320-1.5-dark.png")
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_20_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 2.0f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-compact-320-2.0-light.png")
  }

  @Test
  @Config(qualifiers = "w320dp-h640dp-mdpi", sdk = [34])
  fun matrix_compact_320_20_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 2.0f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-compact-320-2.0-dark.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_085_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 0.85f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-standard-411-0.85-light.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_085_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 0.85f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-standard-411-0.85-dark.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_10_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.0f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-standard-411-1.0-light.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_10_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.0f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-standard-411-1.0-dark.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_15_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-standard-411-1.5-light.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_15_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-standard-411-1.5-dark.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_20_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 2.0f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-standard-411-2.0-light.png")
  }

  @Test
  @Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
  fun matrix_standard_411_20_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 2.0f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-standard-411-2.0-dark.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_085_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 0.85f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-expanded-600-0.85-light.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_085_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 0.85f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-expanded-600-0.85-dark.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_10_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.0f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-expanded-600-1.0-light.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_10_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.0f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-expanded-600-1.0-dark.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_15_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-expanded-600-1.5-light.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_15_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-expanded-600-1.5-dark.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_20_light() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 2.0f)) {
        MyApplicationTheme(darkTheme = false, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-expanded-600-2.0-light.png")
  }

  @Test
  @Config(qualifiers = "w600dp-h900dp-mdpi", sdk = [34])
  fun matrix_expanded_600_20_dark() {
    composeTestRule.setContent {
      CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 2.0f)) {
        MyApplicationTheme(darkTheme = true, dynamicColor = false) {
          DashboardScreen(onStartService = {}, onStopService = {}, showDebugInfo = false)
        }
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/matrix-expanded-600-2.0-dark.png")
  }
}
