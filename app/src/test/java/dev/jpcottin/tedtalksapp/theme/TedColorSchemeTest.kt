package dev.jpcottin.tedtalksapp.theme

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One palette everywhere but on an additive display (optical see-through XR glasses),
 * where a colour is light added to the room and near-black reads as transparent.
 */
class TedColorSchemeTest {
    private val flat = tedColorScheme(additiveDisplay = false)
    private val additive = tedColorScheme(additiveDisplay = true)

    @Test
    fun aFlatScreenKeepsThePaletteItAlwaysHad() {
        // The screenshot references are drawn with these.
        assertEquals(TedRed, flat.primary)
        assertEquals(NearBlack, flat.background)
        assertEquals(OnSurfaceWhite, flat.onSurface)
        assertEquals(OnSurfaceMuted, flat.onSurfaceVariant)
        assertEquals(OnSurfaceMuted, flat.secondary)
        assertEquals(SurfaceVariantDark, flat.surfaceVariant)
        assertEquals(TedDarkRed, flat.error)
    }

    @Test
    fun anAdditiveDisplayBrightensWhatWouldFadeIntoTheRoom() {
        assertTrue(additive.onSurfaceVariant.luminance() > flat.onSurfaceVariant.luminance())
        assertTrue(additive.secondary.luminance() > flat.secondary.luminance())
        assertTrue(additive.surfaceVariant.luminance() > flat.surfaceVariant.luminance())
        assertTrue(additive.error.luminance() > flat.error.luminance())
    }

    @Test
    fun secondaryTextOnAnAdditiveDisplayIsNearlyAsBrightAsBodyText() {
        assertTrue(additive.onSurfaceVariant.luminance() >= 0.6f * additive.onSurface.luminance())
    }

    @Test
    fun anAdditiveDisplayKeepsTheBrandAndTheSeeThroughBackground() {
        assertEquals(flat.primary, additive.primary)
        assertEquals(flat.background, additive.background)
        assertEquals(flat.onSurface, additive.onSurface)
    }
}
