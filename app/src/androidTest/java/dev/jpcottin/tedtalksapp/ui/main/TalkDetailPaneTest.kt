package dev.jpcottin.tedtalksapp.ui.main

import dev.jpcottin.tedtalksapp.TestActivity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.unit.Dp
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
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

    @Test
    fun hero_longTitleDoesNotCoverThePlayButton() {
        val longTitle = "What if the climate movement felt like a house party for everyone?"
        composeTestRule.setContent {
            Box(Modifier.width(360.dp)) {
                TalkDetailPane(
                    talk = talk.copy(title = longTitle, videoUrl = "https://example.com/v.mp4"),
                    showBackButton = false, onBack = {},
                )
            }
        }
        val play = composeTestRule.onNodeWithContentDescription("Play video").getBoundsInRoot()
        val title = composeTestRule.onNodeWithText(longTitle, substring = true).getBoundsInRoot()
        val overlap = title.top < play.bottom && title.bottom > play.top
        assertFalse("title $title covers the play button $play", overlap)
    }

    @Test
    fun inlinePlayer_keepsItsViewAcrossAPostureSwitch() {
        // A half-opened foldable moves the video from the scrolling page to the
        // upper half of a split: the same PlayerView must move with it, not be
        // inflated again and handed the player a second time.
        ComposeUiFlags.isMediaQueryIntegrationEnabled = true
        val context = ApplicationProvider.getApplicationContext<Context>()
        lateinit var player: ExoPlayer
        composeTestRule.runOnUiThread { player = ExoPlayer.Builder(context).build() }
        try {
            var tabletop by mutableStateOf(false)
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalUiMediaScope provides PostureScope(tabletop, 841.dp, 673.dp),
                ) {
                    TalkDetailPane(
                        talk = talk.copy(videoUrl = "https://example.com/v.mp4"),
                        showBackButton = false,
                        onBack = {},
                        getExoPlayer = { player },
                    )
                }
            }
            composeTestRule.onNodeWithContentDescription("Play video").performClick()
            composeTestRule.waitForIdle()
            val before = playerView()
            assertNotNull("the inline player should be on screen after Play", before)

            composeTestRule.runOnIdle { tabletop = true }
            composeTestRule.waitForIdle()
            val after = playerView()
            assertNotNull("the inline player should survive the posture switch", after)
            assertSame("the posture switch inflated a new PlayerView", before, after)
            assertSame(player, after!!.player)
        } finally {
            composeTestRule.runOnUiThread { player.release() }
        }
    }

    private fun playerView(): PlayerView? {
        var found: PlayerView? = null
        composeTestRule.runOnUiThread {
            found = composeTestRule.activity.window.decorView.findPlayerView()
        }
        return found
    }

    private fun View.findPlayerView(): PlayerView? {
        if (this is PlayerView) return this
        if (this !is ViewGroup) return null
        for (i in 0 until childCount) {
            getChildAt(i).findPlayerView()?.let { return it }
        }
        return null
    }

    /** A near-viewed touch device whose window posture the test flips. */
    private class PostureScope(
        tabletop: Boolean,
        override val windowWidth: Dp,
        override val windowHeight: Dp,
    ) : UiMediaScope {
        override val windowPosture =
            if (tabletop) UiMediaScope.Posture.Tabletop else UiMediaScope.Posture.Flat
        override val pointerPrecision = UiMediaScope.PointerPrecision.Coarse
        override val keyboardKind = UiMediaScope.KeyboardKind.Virtual
        override val hasMicrophone = true
        override val hasCamera = true
        override val viewingDistance = UiMediaScope.ViewingDistance.Near
    }

}
