package com.milkys.soundbooster

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.milkys.soundbooster.ui.components.UpdateCard
import com.milkys.soundbooster.update.UpdateChannel
import com.milkys.soundbooster.update.UpdateResult
import com.milkys.soundbooster.update.UpdateUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateCardTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun state(result: UpdateResult? = null) = UpdateUiState(
        channel = UpdateChannel.GITHUB,
        currentVersion = "1.0.4",
        currentVersionCode = 26101202,
        result = result,
    )

    private fun setCard(
        state: UpdateUiState,
        onCheck: () -> Unit = {},
        onOpen: (String, String?) -> Unit = { _, _ -> },
    ) {
        composeTestRule.setContent {
            UpdateCard(
                state = state,
                cardColor = Color.White,
                borderDivider = Color.LightGray,
                textPrimary = Color.Black,
                textSecondary = Color.DarkGray,
                primaryAccent = Color(0xFF5E35B1),
                onCheckForUpdates = onCheck,
                onOpenUrl = onOpen,
            )
        }
    }

    @Test
    fun `check button invokes the callback`() {
        var clicked = false
        setCard(state(), onCheck = { clicked = true })

        composeTestRule.onNodeWithTag("check_for_updates_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `available state shows version and opens the update url`() {
        var openedUrl: String? = null
        setCard(
            state(UpdateResult.Available("1.0.5", "https://example.com/release")),
            onOpen = { url, _ -> openedUrl = url },
        )

        composeTestRule.onNodeWithText("Version 1.0.5 is available.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_update_source_button").performClick()

        assertEquals("https://example.com/release", openedUrl)
    }

    @Test
    fun `play store state opens the store fallback`() {
        var primary = ""
        var fallback: String? = null
        setCard(
            UpdateUiState(
                channel = UpdateChannel.PLAY_STORE,
                currentVersion = "1.0.4",
                currentVersionCode = 26101202,
                result = UpdateResult.OpenStore("market://details?id=pkg", "https://play.google.com/store/apps/details?id=pkg"),
            ),
            onOpen = { url, fallbackUrl ->
                primary = url
                fallback = fallbackUrl
            },
        )

        composeTestRule.onNodeWithTag("open_update_source_button").performClick()

        assertEquals("market://details?id=pkg", primary)
        assertEquals("https://play.google.com/store/apps/details?id=pkg", fallback)
    }
}
