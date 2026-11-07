package dev.jpcottin.tedtalksapp.ui.main

import androidx.xr.scenecore.SpatialEnvironment
import org.junit.Assert.assertEquals
import org.junit.Test

/** The cinema dims the room through the app's passthrough preference and gives it back after. */
class CinemaLightsTest {
    /** The app's preference as the system keeps it; [writes] counts the changes made to it. */
    private class Environment(var preference: Float) {
        var writes = 0
        val lights = CinemaLights(preference = { preference }, setPreference = { preference = it; writes++ })
    }

    @Test
    fun dimmingTurnsPassthroughOff() {
        val environment = Environment(preference = 1f)
        environment.lights.dim()
        assertEquals(0f, environment.preference)
    }

    @Test
    fun restoringGivesBackThePreferenceFromBefore() {
        val environment = Environment(preference = 0.5f)
        environment.lights.dim()
        environment.lights.restore()
        assertEquals(0.5f, environment.preference)
    }

    @Test
    fun noPreferenceIsGivenBackAsNoPreference() {
        // The scene's "no preference" value hands passthrough back to the system.
        val none = SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE
        val environment = Environment(preference = none)
        environment.lights.dim()
        environment.lights.restore()
        assertEquals(none, environment.preference)
    }

    @Test
    fun dimmingTwiceStillRestoresTheOriginal() {
        val environment = Environment(preference = 1f)
        environment.lights.dim()
        environment.lights.dim()
        environment.lights.restore()
        assertEquals(1f, environment.preference)
        assertEquals(2, environment.writes)
    }

    @Test
    fun restoringWithoutDimmingChangesNothing() {
        val environment = Environment(preference = 1f)
        environment.lights.restore()
        assertEquals(1f, environment.preference)
        assertEquals(0, environment.writes)
    }

    @Test
    fun restoringTwiceRestoresOnce() {
        val environment = Environment(preference = 1f)
        environment.lights.dim()
        environment.lights.restore()
        environment.preference = 0.25f // the user changed it since
        environment.lights.restore()
        assertEquals(0.25f, environment.preference)
        assertEquals(2, environment.writes)
    }
}
