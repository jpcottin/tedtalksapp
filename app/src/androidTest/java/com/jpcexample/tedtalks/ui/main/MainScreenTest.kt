package com.jpcexample.tedtalks.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mainScreen_rendersWithoutCrashing() {
        composeTestRule.setContent {
            MainScreen()
        }
        
        composeTestRule.waitForIdle()
        // The list should always be visible regardless of screen size
        composeTestRule.onNodeWithText("TED Talks").assertExists()
    }
}

