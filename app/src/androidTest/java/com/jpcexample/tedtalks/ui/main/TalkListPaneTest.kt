package com.jpcexample.tedtalks.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jpcexample.tedtalks.data.TalkItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TalkListPaneTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val talks = listOf(
        TalkItem("1", "First talk", "Alice", "desc", "May 1, 2025", "5:00", "", "", null),
        TalkItem("2", "Second talk", "Bob", "desc", "May 2, 2025", "6:00", "", "", null),
    )

    @Test
    fun success_rendersAppBarAndTalks() {
        composeTestRule.setContent {
            TalkListPane(
                uiState = TedTalksUiState.Success(talks),
                selectedTalkId = null,
                onTalkClick = {},
                onRetry = {},
            )
        }

        composeTestRule.onNodeWithText("TED Talks").assertIsDisplayed()
        composeTestRule.onNodeWithText("First talk").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second talk").assertIsDisplayed()
    }

    @Test
    fun success_clickingTalkFiresCallback() {
        var clicked: TalkItem? = null
        composeTestRule.setContent {
            TalkListPane(
                uiState = TedTalksUiState.Success(talks),
                selectedTalkId = null,
                onTalkClick = { clicked = it },
                onRetry = {},
            )
        }

        composeTestRule.onNodeWithText("Second talk")
            .assertHasClickAction()
            .performClick()

        assertEquals(talks[1], clicked)
    }

    @Test
    fun error_showsRetryAndFiresCallback() {
        var retried = false
        composeTestRule.setContent {
            TalkListPane(
                uiState = TedTalksUiState.Error("Network down"),
                selectedTalkId = null,
                onTalkClick = {},
                onRetry = { retried = true },
            )
        }

        composeTestRule.onNodeWithText("Couldn't load talks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network down").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try again").performClick()
        assert(retried) { "Expected retry callback to fire" }
    }
}
