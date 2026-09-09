package dev.jpcottin.tedtalksapp.ui.main

import dev.jpcottin.tedtalksapp.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.jpcottin.tedtalksapp.data.TalkItem
import org.junit.Rule
import org.junit.Test

class TalkDetailPaneTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<TestActivity>()

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

        // The detail body scrolls; on short/wide viewports (e.g. a TV rendering
        // this pane full-width) the speaker and description sit below the 16:9
        // hero, so scroll them into view before asserting.
        composeTestRule.onNodeWithText("A bold talk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("An interesting description.")
            .performScrollTo().assertIsDisplayed()
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
