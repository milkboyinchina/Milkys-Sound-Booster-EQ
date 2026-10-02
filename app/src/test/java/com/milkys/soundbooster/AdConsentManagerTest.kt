package com.milkys.soundbooster

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * UMP consent (Q3-A): with user-messaging-platform on the classpath,
 * [AdConsentManager.isUmpAvailable] must be true and every entry point must
 * always invoke its completion callback — success, failure, or no-GMS alike —
 * so onboarding never wedges waiting for consent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdConsentManagerTest {

    @Before
    fun setUp() {
        AudioEffectManager.setAdConsentStatus("UNKNOWN")
        AudioEffectManager.setPersonalizedAdsConsent(true)
    }

    @Test
    fun `ump is available with the real dependency`() {
        assertTrue(AdConsentManager.isUmpAvailable())
    }

    @Test
    fun `consent update always completes even without GMS`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).get()
        val latch = CountDownLatch(1)
        AdConsentManager.requestConsentInfoUpdate(activity) { latch.countDown() }
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun `reset consent never crashes without GMS`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).get()
        AdConsentManager.resetConsent(activity)
    }

    @Test
    fun audioEffectManager_defaultConsentValues() {
        assertEquals("UNKNOWN", AudioEffectManager.adConsentStatus.value)
        assertEquals(true, AudioEffectManager.isPersonalizedAdsConsent.value)
    }

    @Test
    fun audioEffectManager_setConsentStatusAndPersonalizedAds() {
        AudioEffectManager.setAdConsentStatus("GRANTED")
        assertEquals("GRANTED", AudioEffectManager.adConsentStatus.value)

        AudioEffectManager.setPersonalizedAdsConsent(false)
        assertEquals(false, AudioEffectManager.isPersonalizedAdsConsent.value)

        AudioEffectManager.setPersonalizedAdsConsent(true)
        assertEquals(true, AudioEffectManager.isPersonalizedAdsConsent.value)
    }
}
