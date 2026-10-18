package com.milkys.soundbooster

import com.milkys.soundbooster.update.UpdateChannel
import com.milkys.soundbooster.update.UpdateChecker
import com.milkys.soundbooster.update.UpdateController
import com.milkys.soundbooster.update.UpdateResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateControllerTest {

    private class FakeChecker(
        private val result: UpdateResult,
    ) : UpdateChecker {
        override val channel: UpdateChannel = UpdateChannel.GITHUB
        override suspend fun check(): UpdateResult = result
    }

    @Test
    fun `checkForUpdates emits checking then available`() = runTest {
        val controller = UpdateController(
            checker = FakeChecker(UpdateResult.Available("1.0.5", "https://example.com")),
            scope = this,
            currentVersion = "1.0.4",
            currentVersionCode = 26101202,
        )

        controller.checkForUpdates()
        assertTrue(controller.state.value.isChecking)

        advanceUntilIdle()

        assertFalse(controller.state.value.isChecking)
        assertEquals(UpdateResult.Available("1.0.5", "https://example.com"), controller.state.value.result)
    }

    @Test
    fun `checkForUpdates converts thrown errors into an error state`() = runTest {
        val controller = UpdateController(
            checker = object : UpdateChecker {
                override val channel: UpdateChannel = UpdateChannel.FDROID
                override suspend fun check(): UpdateResult = error("offline")
            },
            scope = this,
            currentVersion = "1.0.4",
            currentVersionCode = 26101202,
        )

        controller.checkForUpdates()
        advanceUntilIdle()

        val result = controller.state.value.result
        assertTrue(result is UpdateResult.Error)
        assertEquals("offline", (result as UpdateResult.Error).message)
    }

    @Test
    fun `clearResult resets only the result`() = runTest {
        val controller = UpdateController(
            checker = FakeChecker(UpdateResult.UpToDate("1.0.4")),
            scope = this,
            currentVersion = "1.0.4",
            currentVersionCode = 26101202,
        )

        controller.checkForUpdates()
        advanceUntilIdle()
        controller.clearResult()

        assertEquals(null, controller.state.value.result)
        assertEquals("1.0.4", controller.state.value.currentVersion)
    }
}
