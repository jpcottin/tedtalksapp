package com.jpcexample.tedtalks.ui.main

import android.content.Context
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jpcexample.tedtalks.data.FakeTedTalksRepository
import com.jpcexample.tedtalks.data.TalkItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TedTalksViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val sampleTalks = listOf(
        TalkItem("1", "Title A", "Speaker A", "desc", "May 1, 2025", "5:00", "", "", null),
        TalkItem("2", "Title B", "Speaker B", "desc", "May 2, 2025", "6:00", "", "", null),
    )

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun init_loadsTalksAndExposesSuccessState() = runTest {
        val repo = FakeTedTalksRepository(Result.success(sampleTalks))

        val viewModel = TedTalksViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue("Expected Success but was $state", state is TedTalksUiState.Success)
        assertEquals(sampleTalks, (state as TedTalksUiState.Success).talks)
        assertEquals(1, repo.fetchCount)
    }

    @Test
    fun fetchFailure_exposesErrorState() = runTest {
        val repo = FakeTedTalksRepository(Result.failure(RuntimeException("boom")))

        val viewModel = TedTalksViewModel(repo)

        val state = viewModel.uiState.value
        assertTrue("Expected Error but was $state", state is TedTalksUiState.Error)
        assertEquals("boom", (state as TedTalksUiState.Error).message)
    }

    @Test
    fun loadTalks_canRetryAfterError() = runTest {
        val repo = FakeTedTalksRepository(Result.failure(RuntimeException("first try failed")))
        val viewModel = TedTalksViewModel(repo)
        assertTrue(viewModel.uiState.value is TedTalksUiState.Error)

        repo.response = Result.success(sampleTalks)
        viewModel.loadTalks()

        val state = viewModel.uiState.value
        assertTrue(state is TedTalksUiState.Success)
        assertEquals(sampleTalks, (state as TedTalksUiState.Success).talks)
        assertEquals(2, repo.fetchCount)
    }

    @Test
    fun selectTalk_updatesSelectedIdFlow() = runTest {
        val viewModel = TedTalksViewModel(FakeTedTalksRepository())

        viewModel.selectTalk("talk-123")

        assertEquals("talk-123", viewModel.selectedTalkId.value)
    }

    @Test
    fun clearSelection_resetsSelectedId() = runTest {
        val viewModel = TedTalksViewModel(FakeTedTalksRepository())
        viewModel.selectTalk("talk-123")

        viewModel.clearSelection()

        assertNull(viewModel.selectedTalkId.value)
    }

    // --- getExoPlayer: playback position bookkeeping (player is mocked, no media stack) ---

    private fun playerViewModel(player: ExoPlayer): TedTalksViewModel =
        TedTalksViewModel(FakeTedTalksRepository(Result.success(sampleTalks))) { player }

    @Before
    fun stubUriParse() {
        // MediaItem.fromUri calls android.net.Uri, which is a stub on the local JVM.
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    @After
    fun unstubUriParse() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun getExoPlayer_sameUrl_reusesPlayerWithoutRestartingMedia() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        val viewModel = playerViewModel(player)
        val context = mockk<Context>(relaxed = true)

        val first = viewModel.getExoPlayer(context, "https://cdn/a.mp4")
        val second = viewModel.getExoPlayer(context, "https://cdn/a.mp4")

        assertEquals(first, second)
        verify(exactly = 1) { player.setMediaItem(any()) }
        verify(exactly = 1) { player.prepare() }
    }

    @Test
    fun getExoPlayer_switchingBackToTalk_resumesSavedPosition() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        every { player.playbackState } returns Player.STATE_READY
        every { player.currentPosition } returns 42_000L
        val viewModel = playerViewModel(player)
        val context = mockk<Context>(relaxed = true)

        viewModel.getExoPlayer(context, "https://cdn/a.mp4")
        viewModel.getExoPlayer(context, "https://cdn/b.mp4") // saves a.mp4 at 42s
        viewModel.getExoPlayer(context, "https://cdn/a.mp4") // must resume there

        verify { player.seekTo(42_000L) }
    }

    @Test
    fun getExoPlayer_finishedTalk_restartsFromBeginning() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        every { player.playbackState } returns Player.STATE_ENDED
        every { player.currentPosition } returns 600_000L
        val viewModel = playerViewModel(player)
        val context = mockk<Context>(relaxed = true)

        viewModel.getExoPlayer(context, "https://cdn/a.mp4")
        viewModel.getExoPlayer(context, "https://cdn/b.mp4") // a.mp4 ended: position dropped
        viewModel.getExoPlayer(context, "https://cdn/a.mp4")

        verify(exactly = 0) { player.seekTo(any<Long>()) }
    }
}
