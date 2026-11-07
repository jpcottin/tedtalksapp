package dev.jpcottin.tedtalksapp.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

/** The space switch is named after where a tap takes the app, like the system's own. */
class SpaceToggleLabelTest {
    @Test
    fun inHomeSpaceItOffersFullSpace() {
        assertEquals("Full Space", spaceToggleLabel(isFullSpace = false))
    }

    @Test
    fun inFullSpaceItOffersHomeSpace() {
        assertEquals("Home Space", spaceToggleLabel(isFullSpace = true))
    }
}
