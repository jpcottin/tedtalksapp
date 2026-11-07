package dev.jpcottin.tedtalksapp.ui.main

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Android XR cinema panel takes the video's shape at the largest size its box allows. */
class CinemaPanelSizeTest {
    @Test
    fun aSixteenByNineVideoFillsTheBox() {
        assertSize(1120f, 630f, cinemaPanelSize(1920, 1080))
        assertSize(1120f, 630f, cinemaPanelSize(1280, 720))
    }

    @Test
    fun aWiderVideoKeepsTheWidthAndLosesHeight() {
        // A scope film, 2.39:1.
        assertSize(1120f, 1120f / (1920f / 804f), cinemaPanelSize(1920, 804))
    }

    @Test
    fun aNarrowerVideoKeepsTheHeightAndLosesWidth() {
        assertSize(840f, 630f, cinemaPanelSize(1440, 1080))
        // Portrait: 9:16 stands as tall as the box and no wider than it needs.
        assertSize(630f * 9f / 16f, 630f, cinemaPanelSize(1080, 1920))
    }

    @Test
    fun theVideosPixelShapeCounts() {
        // 1440x1080 stored with 4:3 pixels is shown 16:9.
        assertSize(1120f, 630f, cinemaPanelSize(1440, 1080, pixelRatio = 4f / 3f))
    }

    @Test
    fun anOddStreamIsKeptWithinTheScopeBand() {
        assertSize(1120f, 1120f / 2.39f, cinemaPanelSize(4000, 100))
        assertSize(630f / 2.39f, 630f, cinemaPanelSize(100, 4000))
    }

    @Test
    fun anUnknownSizeTakesTheBoxShape() {
        // An audio talk has no frames: its cover fills a box-shaped panel.
        assertSize(1120f, 630f, cinemaPanelSize(0, 0))
        assertSize(1120f, 630f, cinemaPanelSize(1920, 0))
        assertSize(1120f, 630f, cinemaPanelSize(-1, 1080))
        assertSize(1120f, 630f, cinemaPanelSize(1920, 1080, pixelRatio = 0f))
    }

    @Test
    fun anotherBoxIsHonoured() {
        // 16:9 in a 4:3 box: the full width, and the height that keeps the shape.
        assertSize(400f, 225f, cinemaPanelSize(1920, 1080, box = DpSize(400.dp, 300.dp)))
    }

    @Test
    fun thePanelNeverLeavesTheBox() {
        for (width in listOf(1, 320, 1080, 1440, 1920, 3840, 10_000)) {
            for (height in listOf(1, 240, 720, 1080, 2160, 10_000)) {
                val size = cinemaPanelSize(width, height)
                assertTrue("$width x $height gave $size", size.width.value <= 1120f + EPSILON && size.height.value <= 630f + EPSILON)
            }
        }
    }

    private fun assertSize(width: Float, height: Float, actual: DpSize) {
        assertEquals("width of $actual", width, actual.width.value, EPSILON)
        assertEquals("height of $actual", height, actual.height.value, EPSILON)
    }

    private companion object { const val EPSILON = 0.01f }
}
