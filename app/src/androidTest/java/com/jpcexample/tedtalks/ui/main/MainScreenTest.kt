package com.jpcexample.tedtalks.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.compose.viewModel
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mainScreen_rendersWithoutCrashing() {
        composeTestRule.setContent {
            MainScreen(viewModel = viewModel())
        }
        
        composeTestRule.waitForIdle()
        // The list should always be visible regardless of screen size
        composeTestRule.onNodeWithText("TED Talks").assertExists()
    }
}

