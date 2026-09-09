package dev.jpcottin.tedtalksapp

import dev.jpcottin.tedtalksapp.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.jpcottin.tedtalksapp.data.TalkItem
import dev.jpcottin.tedtalksapp.data.TedTalksRepository
import dev.jpcottin.tedtalksapp.theme.MyApplicationTheme
import androidx.test.espresso.Espresso
import dev.jpcottin.tedtalksapp.ui.main.TedTalksViewModel
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Renders the full Nav3 graph using an in-memory fake repository so the test
 * does not hit the network.
 */
class MainNavigationTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<TestActivity>()

    private class StaticRepo(val talks: List<TalkItem>) : TedTalksRepository {
        override suspend fun fetchTalks(): Result<List<TalkItem>> = Result.success(talks)
    }

    private val talks = listOf(
        TalkItem("1", "First talk", "Alice", "Alice's description.", "May 1, 2025", "5:00", "", "https://ted.com/1", null),
        TalkItem("2", "Second talk", "Bob", "Bob's description.", "May 2, 2025", "6:00", "", "https://ted.com/2", null),
    )

    @Test
    fun navGraph_rendersListWithoutCrashing() {
        val viewModel = TedTalksViewModel(StaticRepo(talks))
        composeTestRule.setContent {
            MyApplicationTheme {
                MainNavigation(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TED Talks").assertIsDisplayed()
        composeTestRule.onNodeWithText("First talk").assertIsDisplayed()
    }

    @Test
    fun navGraph_tappingListItemNavigatesToDetail() {
        val viewModel = TedTalksViewModel(StaticRepo(talks))
        composeTestRule.setContent {
            MyApplicationTheme {
                MainNavigation(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Second talk").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bob's description.").assertIsDisplayed()
    }

    @Test
    fun navGraph_backFromDetailReturnsToList() {
        val viewModel = TedTalksViewModel(StaticRepo(talks))
        composeTestRule.setContent {
            MyApplicationTheme {
                MainNavigation(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Second talk").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bob's description.").assertIsDisplayed()

        Espresso.pressBack()
        composeTestRule.waitForIdle()

        // Back must dismiss the detail entry and land on the list, not finish
        // the activity (regression guard for the two-pane back fallback).
        composeTestRule.onNodeWithText("TED Talks").assertIsDisplayed()
        composeTestRule.onNodeWithText("First talk").assertIsDisplayed()
        assertNull(viewModel.selectedTalkId.value)
    }
}
