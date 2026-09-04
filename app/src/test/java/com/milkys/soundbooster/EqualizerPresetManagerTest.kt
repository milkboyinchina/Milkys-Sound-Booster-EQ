package com.milkys.soundbooster

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EqualizerPresetManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences before each test
        val prefs = context.getSharedPreferences("volume_booster_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        AudioEffectManager.init(context)
    }

    @Test
    fun testMatchedPresetName() {
        // Flat preset matches default [0, 0, 0, 0, 0]
        assertEquals("Flat", AudioEffectManager.getMatchedPresetName(intArrayOf(0, 0, 0, 0, 0)))
        assertEquals("Bass Booster", AudioEffectManager.getMatchedPresetName(intArrayOf(8, 5, 2, 0, 0)))

        // Non-matching bands return null (triggering "EQ : Custom")
        assertNull(AudioEffectManager.getMatchedPresetName(intArrayOf(1, 2, 3, 4, 5)))
    }

    @Test
    fun testCustomPresetNameLengthLimit() {
        // Name longer than 10 chars should be rejected
        val longName = "VeryLongNameExceeding10Chars"
        val bands = intArrayOf(1, 2, 3, 4, 5)
        val err = AudioEffectManager.validateCustomPreset(longName, bands)
        assertNotNull(err)
        assertTrue(err!!.contains("10 characters"))

        val result = AudioEffectManager.saveCustomPresetWithResult(longName, bands)
        assertNotNull(result)
    }

    @Test
    fun testCustomPresetCountLimit7() {
        // Save 7 custom presets successfully
        for (i in 1..7) {
            val name = "P$i"
            val bands = intArrayOf(i, -i, i, -i, i)
            val err = AudioEffectManager.saveCustomPresetWithResult(name, bands)
            assertNull("Preset $i should be saved successfully", err)
        }
        assertEquals(7, AudioEffectManager.customPresets.value.size)

        // 8th custom preset creation should be rejected
        val name8 = "P8"
        val bands8 = intArrayOf(2, 3, 4, 5, 6)
        val err8 = AudioEffectManager.saveCustomPresetWithResult(name8, bands8)
        assertNotNull(err8)
        assertTrue(err8!!.contains("Maximum 7"))
    }

    @Test
    fun testDuplicatePresetNameRejection() {
        // Built-in name duplicate check
        val errBuiltIn = AudioEffectManager.validateCustomPreset("Flat", intArrayOf(1, 1, 1, 1, 1))
        assertNotNull(errBuiltIn)
        assertTrue(errBuiltIn!!.contains("already exists"))

        // Save a valid custom preset
        AudioEffectManager.saveCustomPresetWithResult("Custom1", intArrayOf(1, 2, 3, 4, 5))

        // Attempting to save another preset with the same name should be rejected
        val errDuplicate = AudioEffectManager.validateCustomPreset("Custom1", intArrayOf(2, 3, 4, 5, 6))
        assertNotNull(errDuplicate)
        assertTrue(errDuplicate!!.contains("already exists"))
    }

    @Test
    fun testDuplicatePresetValuesRejection() {
        // Attempting to save custom preset with exact built-in band values should be rejected
        val flatBands = intArrayOf(0, 0, 0, 0, 0)
        val errFlatValues = AudioEffectManager.validateCustomPreset("MyFlat", flatBands)
        assertNotNull(errFlatValues)
        assertTrue(errFlatValues!!.contains("match an existing preset"))

        // Save Custom1
        AudioEffectManager.saveCustomPresetWithResult("Custom1", intArrayOf(3, 3, 3, 3, 3))

        // Save Custom2 with identical bands [3, 3, 3, 3, 3] should be rejected
        val errDupValues = AudioEffectManager.validateCustomPreset("Custom2", intArrayOf(3, 3, 3, 3, 3))
        assertNotNull(errDupValues)
        assertTrue(errDupValues!!.contains("match an existing preset"))
    }

    @Test
    fun testFavoritesCapLimit4() {
        assertTrue(AudioEffectManager.toggleFavorite("Flat"))
        assertTrue(AudioEffectManager.toggleFavorite("Bass Booster"))
        assertTrue(AudioEffectManager.toggleFavorite("Vocal Booster"))
        assertTrue(AudioEffectManager.toggleFavorite("Rock"))
        assertEquals(4, AudioEffectManager.favoritePresets.value.size)

        // 5th favorite attempt should return false (blocked)
        assertFalse(AudioEffectManager.toggleFavorite("Pop"))
        assertEquals(4, AudioEffectManager.favoritePresets.value.size)

        // Untag one favorite
        assertTrue(AudioEffectManager.toggleFavorite("Flat"))
        assertEquals(3, AudioEffectManager.favoritePresets.value.size)

        // Now tagging "Pop" should succeed
        assertTrue(AudioEffectManager.toggleFavorite("Pop"))
        assertEquals(4, AudioEffectManager.favoritePresets.value.size)
    }

    @Test
    fun testExportAndImportPreset() {
        val bands = intArrayOf(4, 3, 2, 1, 0)
        AudioEffectManager.saveCustomPresetWithResult("MyPreset", bands)

        val exportedJson = AudioEffectManager.exportPreset("MyPreset")
        assertTrue(exportedJson.contains("\"name\": \"MyPreset\""))
        assertTrue(exportedJson.contains("\"values\""))

        // Delete custom preset
        AudioEffectManager.deleteCustomPreset("MyPreset")
        assertFalse(AudioEffectManager.customPresets.value.containsKey("MyPreset"))

        // Re-import from JSON
        val (importedName, err) = AudioEffectManager.importPresetWithResult(exportedJson)
        assertNull(err)
        assertEquals("MyPreset", importedName)
        assertTrue(AudioEffectManager.customPresets.value.containsKey("MyPreset"))
    }

    @Test
    fun testBatchDeleteCustomPresets() {
        AudioEffectManager.saveCustomPresetWithResult("P1", intArrayOf(1, 0, 0, 0, 0))
        AudioEffectManager.saveCustomPresetWithResult("P2", intArrayOf(2, 0, 0, 0, 0))
        AudioEffectManager.saveCustomPresetWithResult("P3", intArrayOf(3, 0, 0, 0, 0))

        AudioEffectManager.toggleFavorite("P1")
        AudioEffectManager.toggleFavorite("P2")

        assertEquals(3, AudioEffectManager.customPresets.value.size)
        assertTrue(AudioEffectManager.favoritePresets.value.contains("P1"))

        // Batch delete P1 and P2
        AudioEffectManager.deleteCustomPresets(setOf("P1", "P2"))

        assertEquals(1, AudioEffectManager.customPresets.value.size)
        assertFalse(AudioEffectManager.customPresets.value.containsKey("P1"))
        assertFalse(AudioEffectManager.customPresets.value.containsKey("P2"))
        assertTrue(AudioEffectManager.customPresets.value.containsKey("P3"))

        // Assert deleted presets were untagged from favorites
        assertFalse(AudioEffectManager.favoritePresets.value.contains("P1"))
        assertFalse(AudioEffectManager.favoritePresets.value.contains("P2"))
    }
}
