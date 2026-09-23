package com.milkys.soundbooster

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Crash-loop breaker policy: silent onCreate auto-start must never launch the
 * foreground service when notifications are denied (ForegroundServiceDidNotStartInTimeException).
 */
class ServiceStartLogicTest {

    @Test
    fun `auto-start when boost persisted and notifications granted`() {
        assertTrue(shouldAutoStartService(boostEnabled = true, floatingEnabled = false, notificationsEnabled = true))
    }

    @Test
    fun `auto-start when overlay persisted and notifications granted`() {
        assertTrue(shouldAutoStartService(boostEnabled = false, floatingEnabled = true, notificationsEnabled = true))
    }

    @Test
    fun `no auto-start when nothing persisted`() {
        assertFalse(shouldAutoStartService(boostEnabled = false, floatingEnabled = false, notificationsEnabled = true))
        assertFalse(shouldAutoStartService(boostEnabled = false, floatingEnabled = false, notificationsEnabled = false))
    }

    @Test
    fun `no auto-start when notifications denied even with persisted state`() {
        assertFalse(shouldAutoStartService(boostEnabled = true, floatingEnabled = false, notificationsEnabled = false))
        assertFalse(shouldAutoStartService(boostEnabled = false, floatingEnabled = true, notificationsEnabled = false))
        assertFalse(shouldAutoStartService(boostEnabled = true, floatingEnabled = true, notificationsEnabled = false))
    }
}
