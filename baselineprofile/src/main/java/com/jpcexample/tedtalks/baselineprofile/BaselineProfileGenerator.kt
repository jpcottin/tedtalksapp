package com.jpcexample.tedtalks.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile that covers app startup plus the core
 * browse → open-detail → back journey, so R8/AOT can pre-compile those hot
 * paths. Output lands in `app/src/<variant>/generated/baselineProfiles/`.
 *
 * Run with: `./gradlew :app:generateReleaseBaselineProfile`
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = TARGET_PACKAGE,
        // Include the first frame so the startup window is part of the profile.
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        // Wait for the list to render, then exercise list ↔ detail navigation.
        val list = device.wait(Until.hasObject(By.scrollable(true)), 10_000)
        if (list) {
            val scrollable = device.findObject(By.scrollable(true))
            // A couple of scrolls so the lazy grid's bind/recycle paths are covered.
            scrollable?.fling(androidx.test.uiautomator.Direction.DOWN)
            device.waitForIdle()
            scrollable?.fling(androidx.test.uiautomator.Direction.UP)
            device.waitForIdle()

            // Open the first talk's detail pane, then return.
            device.findObject(By.scrollable(true))?.children?.firstOrNull()?.click()
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()
        }
    }
}
