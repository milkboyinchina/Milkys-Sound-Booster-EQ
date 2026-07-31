package com.milkys.soundbooster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AdConsentManagerTest {

    @Before
    fun setUp() {
        AudioEffectManager.setAdConsentStatus("UNKNOWN")
        AudioEffectManager.setPersonalizedAdsConsent(true)
    }

    @Test
    fun adConsentManager_isUmpAvailableExecutesWithoutCrashing() {
        val available = AdConsentManager.isUmpAvailable()
        assertNotNull(available)
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
