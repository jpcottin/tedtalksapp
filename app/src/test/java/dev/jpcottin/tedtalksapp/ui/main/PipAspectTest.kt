package dev.jpcottin.tedtalksapp.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

/** The PiP window keeps the video's shape within the band the system accepts. */
class PipAspectTest {
    @Test
    fun aNormalVideoKeepsItsOwnRatio() {
        assertEquals(1920 to 1080, pipAspect(1920, 1080))
        assertEquals(1080 to 1920, pipAspect(1080, 1920))
    }

    @Test
    fun ultraWideIsClampedToTheWidestAllowed() {
        assertEquals(239 to 100, pipAspect(3000, 1000))
    }

    @Test
    fun ultraTallIsClampedToTheTallestAllowed() {
        assertEquals(100 to 239, pipAspect(100, 1000))
    }

    @Test
    fun theBandEdgesAreAccepted() {
        assertEquals(239 to 100, pipAspect(239, 100))
        assertEquals(100 to 239, pipAspect(100, 239))
    }

    @Test
    fun anUnknownSizeFallsBackToSixteenByNine() {
        assertEquals(16 to 9, pipAspect(0, 0))
        assertEquals(16 to 9, pipAspect(1280, 0))
        assertEquals(16 to 9, pipAspect(-1, 720))
    }
}
