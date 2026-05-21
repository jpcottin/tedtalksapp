package com.jpcexample.tedtalks.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpcexample.tedtalks.data.TalkItem
import org.junit.Rule
import org.junit.Test

class TalkDetailPaneTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val talk = TalkItem(
        id = "1",
        title = "A bold talk",
        speaker = "Alice",
        description = "An interesting description.",
        pubDate = "May 1, 2025",
        duration = "5:00",
        imageUrl = "",
        link = "https://ted.com/x",
        videoUrl = null,
    )

    @Test
    fun rendersSpeakerAndDescription() {
        composeTestRule.setContent {
            TalkDetailPane(talk = talk, showBackButton = false, onBack = {})
        }

        composeTestRule.onNodeWithText("A bold talk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("An interesting description.").assertIsDisplayed()
    }

    @Test
    fun backButton_visibilityFollowsParameter() {
        composeTestRule.setContent {
            TalkDetailPane(talk = talk, showBackButton = false, onBack = {})
        }
        composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun backButton_firesCallbackWhenShown() {
        var backed = false
        composeTestRule.setContent {
            TalkDetailPane(talk = talk, showBackButton = true, onBack = { backed = true })
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(backed) { "Expected back callback to fire" }
    }
}
