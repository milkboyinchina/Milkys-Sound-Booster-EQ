package com.milkys.soundbooster

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cold-start black-screen regression (Vivo V1901K, Android 9 user report).
 *
 * Root cause: MainActivity.onCreate called applyLanguageToApp() unconditionally,
 * and AppCompatDelegate.setApplicationLocales() recreates the host Activity on
 * API <33. Each recreation re-entered onCreate -> set -> recreate forever
 * (377 onCreate loop observed on an API 28 emulator, black screen, no crash).
 *
 * applyLanguageToApp must therefore be idempotent within a process: repeated
 * calls with the same tag may invoke setApplicationLocales at most once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocaleColdStartTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AudioEffectManager.resetLanguageForTest()
    }

    @Test
    fun `repeated same-tag apply triggers at most one setApplicationLocales`() {
        repeat(5) { AudioEffectManager.applyLanguageToApp(context, "id") }
        assertTrue(
            "expected <=1 locale set for 5 identical applies, got " +
                "${AudioEffectManager.setApplicationLocalesCalls}",
            AudioEffectManager.setApplicationLocalesCalls <= 1,
        )
    }

    @Test
    fun `applied locales match requested tag`() {
        AudioEffectManager.applyLanguageToApp(context, "id")
        assertEquals(
            LocaleListCompat.forLanguageTags("id"),
            AppCompatDelegate.getApplicationLocales(),
        )
    }

    @Test
    fun `system resolves to empty list`() {
        AudioEffectManager.applyLanguageToApp(context, "system")
        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
    }

    @Test
    fun `tag change re-applies`() {
        AudioEffectManager.applyLanguageToApp(context, "id")
        AudioEffectManager.resetLanguageForTest()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        AudioEffectManager.applyLanguageToApp(context, "id")
        assertEquals(1, AudioEffectManager.setApplicationLocalesCalls)
    }
}
