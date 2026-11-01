package dev.jpcottin.tedtalksapp.ui.main

import dev.jpcottin.tedtalksapp.TestActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.unit.dp
import dev.jpcottin.tedtalksapp.data.TalkItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class TalkListPaneTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<TestActivity>()

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

    @Test
    fun success_pullingDownFiresRefresh() {
        var refreshed = false
        composeTestRule.setContent {
            TalkListPane(
                uiState = TedTalksUiState.Success(talks),
                selectedTalkId = null,
                onTalkClick = {},
                onRetry = {},
                onRefresh = { refreshed = true },
            )
        }

        // A short flick stays under the pull threshold; drag most of the pane.
        composeTestRule.onRoot().performTouchInput {
            swipeDown(startY = height * 0.3f, endY = height * 0.95f, durationMillis = 600)
        }
        composeTestRule.waitForIdle()

        assert(refreshed) { "Expected pull-to-refresh to fire" }
    }

    @Test
    fun audioToggle_asksForTheOtherChoice() {
        var asked: Boolean? = null
        composeTestRule.setContent {
            TalkListPane(
                uiState = TedTalksUiState.Success(talks),
                selectedTalkId = null,
                onTalkClick = {},
                onRetry = {},
                includeAudio = false,
                onIncludeAudioChange = { asked = it },
            )
        }
        composeTestRule.onNodeWithTag("audio_toggle").assertIsDisplayed().assertIsNotSelected().performClick()
        assertEquals(true, asked)
    }

    @Test
    fun audioToggle_isAbsentWhenTheListHasNoChoice() {
        composeTestRule.setContent {
            TalkListPane(uiState = TedTalksUiState.Success(talks), selectedTalkId = null, onTalkClick = {}, onRetry = {})
        }
        composeTestRule.onNodeWithTag("audio_toggle").assertDoesNotExist()
    }

    @Test
    fun audioTalks_wearABadgeInTheList() {
        val mixed = talks + TalkItem("3", "A podcast episode", "Carol", "desc", "May 3, 2025", "41:18", "", "", "https://example.com/e.mp3", isAudio = true)
        composeTestRule.setContent {
            TalkListPane(uiState = TedTalksUiState.Success(mixed), selectedTalkId = null, onTalkClick = {}, onRetry = {})
        }
        composeTestRule.onAllNodesWithContentDescription("Audio").assertCountEquals(1)
    }

    @Test
    fun dpadDevice_hasNoPullToRefresh() {
        // A TV has no pull gesture; the box used to take the first row's focus
        // scroll for a pull and leave its arrow over the row.
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true
        var refreshed = false
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUiMediaScope provides DpadScope) {
                TalkListPane(
                    uiState = TedTalksUiState.Success(talks),
                    selectedTalkId = null,
                    onTalkClick = {},
                    onRetry = {},
                    onRefresh = { refreshed = true },
                )
            }
        }
        composeTestRule.onRoot().performTouchInput {
            swipeDown(startY = height * 0.3f, endY = height * 0.95f, durationMillis = 600)
        }
        composeTestRule.waitForIdle()
        assertFalse("a pull on a D-pad device must not refresh", refreshed)
        composeTestRule.onNodeWithText("First talk").assertIsDisplayed()
    }

    /** A far-viewed device driven by a remote: no pointer at all. */
    private object DpadScope : UiMediaScope {
        override val windowWidth = 960.dp
        override val windowHeight = 540.dp
        override val windowPosture = UiMediaScope.Posture.Flat
        override val pointerPrecision = UiMediaScope.PointerPrecision.None
        override val keyboardKind = UiMediaScope.KeyboardKind.None
        override val hasMicrophone = false
        override val hasCamera = false
        override val viewingDistance = UiMediaScope.ViewingDistance.Far
    }

    @Test
    fun success_exposesTheOpenTalkAsSelected() {
        composeTestRule.setContent {
            TalkListPane(uiState = TedTalksUiState.Success(talks), selectedTalkId = "1", onTalkClick = {}, onRetry = {})
        }
        composeTestRule.onNodeWithText("First talk").assertIsSelected()
        composeTestRule.onNodeWithText("Second talk").assertIsNotSelected()
    }

    @Test
    fun success_refreshErrorIsConsumedWhenShown() {
        var shownCalls = 0
        composeTestRule.setContent {
            TalkListPane(uiState = TedTalksUiState.Success(talks), selectedTalkId = null, onTalkClick = {},
                onRetry = {}, refreshError = "offline", onRefreshErrorShown = { shownCalls++ })
        }
        composeTestRule.onNodeWithText("Couldn't refresh: offline").assertIsDisplayed()
        assertEquals(1, shownCalls)
    }

    @Test
    fun success_refreshErrorShowsSnackbar() {
        composeTestRule.setContent {
            TalkListPane(
                uiState = TedTalksUiState.Success(talks),
                selectedTalkId = null,
                onTalkClick = {},
                onRetry = {},
                refreshError = "offline",
            )
        }

        composeTestRule.onNodeWithText("Couldn't refresh: offline").assertIsDisplayed()
        composeTestRule.onNodeWithText("First talk").assertIsDisplayed()
    }
}
