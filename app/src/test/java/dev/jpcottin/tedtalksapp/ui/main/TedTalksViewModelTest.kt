package dev.jpcottin.tedtalksapp.ui.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.jpcottin.tedtalksapp.data.FakeTedTalksRepository
import dev.jpcottin.tedtalksapp.data.TalkItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun init_emptyFeed_exposesErrorWithRetry() = runTest {
        val viewModel = TedTalksViewModel(FakeTedTalksRepository(Result.success(emptyList())))
        val state = viewModel.uiState.value
        assertTrue("Expected Error but was $state", state is TedTalksUiState.Error)
    }

    @Test
    fun loadTalks_emptyRefresh_keepsTheListAndReportsIt() = runTest {
        val repo = FakeTedTalksRepository(Result.success(sampleTalks))
        val viewModel = TedTalksViewModel(repo)
        repo.response = Result.success(emptyList())
        viewModel.loadTalks()
        assertTrue(viewModel.uiState.value is TedTalksUiState.Success)
        assertEquals(sampleTalks, (viewModel.uiState.value as TedTalksUiState.Success).talks)
        assertTrue(viewModel.refreshError.value != null)
    }

    @Test
    fun selectTalk_anotherTalkWhileOnePlays_pausesThePlayer() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        val viewModel = playerViewModel(player)
        viewModel.selectTalk("1")
        viewModel.getExoPlayer(mockk(relaxed = true), "https://cdn/a.mp4")
        viewModel.selectTalk("2")                                // B takes the pane
        verify(exactly = 1) { player.pause() }
    }

    @Test
    fun selectTalk_sameTalkAgain_doesNotPause() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        val viewModel = playerViewModel(player)
        viewModel.selectTalk("1")
        viewModel.getExoPlayer(mockk(relaxed = true), "https://cdn/a.mp4")
        viewModel.selectTalk("1")
        verify(exactly = 0) { player.pause() }
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
    fun loadTalks_withListShowing_refreshesBehindItInsteadOfShowingLoading() = runTest {
        val repo = FakeTedTalksRepository(Result.success(sampleTalks))
        val viewModel = TedTalksViewModel(repo)
        val newTalks = sampleTalks.take(1)
        repo.response = Result.success(newTalks)
        repo.gate = CompletableDeferred()

        viewModel.loadTalks()

        // In flight: old list still showing, refresh flag up.
        assertTrue(viewModel.isRefreshing.value)
        assertEquals(sampleTalks, (viewModel.uiState.value as TedTalksUiState.Success).talks)

        repo.gate!!.complete(Unit)

        assertFalse(viewModel.isRefreshing.value)
        assertEquals(newTalks, (viewModel.uiState.value as TedTalksUiState.Success).talks)
        assertNull(viewModel.refreshError.value)
    }

    @Test
    fun loadTalks_whileAFetchIsInFlight_doesNotStartAnother() = runTest {
        val repo = FakeTedTalksRepository(Result.success(sampleTalks))
        val viewModel = TedTalksViewModel(repo)                  // the initial load completes
        assertEquals(1, repo.fetchCount)

        repo.gate = CompletableDeferred()
        viewModel.loadTalks()                                    // pull to refresh…
        viewModel.loadTalks()                                    // …and again before it returns
        assertEquals(2, repo.fetchCount)                         // one request, not two
        assertTrue(viewModel.isRefreshing.value)

        repo.gate!!.complete(Unit)
        assertFalse(viewModel.isRefreshing.value)                // the indicator goes with the request

        viewModel.loadTalks()                                    // a later refresh is fine
        assertEquals(3, repo.fetchCount)
    }

    @Test
    fun loadTalks_refreshFailure_keepsListAndReportsError() = runTest {
        val repo = FakeTedTalksRepository(Result.success(sampleTalks))
        val viewModel = TedTalksViewModel(repo)
        repo.response = Result.failure(RuntimeException("offline"))

        viewModel.loadTalks()

        assertFalse(viewModel.isRefreshing.value)
        assertEquals(sampleTalks, (viewModel.uiState.value as TedTalksUiState.Success).talks)
        assertEquals("offline", viewModel.refreshError.value)

        viewModel.clearRefreshError()

        assertNull(viewModel.refreshError.value)
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
    fun getExoPlayer_sameTalkAfterADeselection_playsAgainWithoutRePreparing() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        val viewModel = playerViewModel(player)
        val context = mockk<Context>(relaxed = true)

        viewModel.getExoPlayer(context, "https://cdn/a.mp4")
        viewModel.clearSelection()                               // Back: the player pauses
        viewModel.getExoPlayer(context, "https://cdn/a.mp4")     // the talk opened again, Play tapped

        verify(exactly = 1) { player.prepare() }
        verify(exactly = 2) { player.playWhenReady = true }
    }

    @Test
    fun getExoPlayer_sameTalkAfterItEnded_startsOver() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        every { player.playbackState } returns Player.STATE_ENDED
        val viewModel = playerViewModel(player)
        val context = mockk<Context>(relaxed = true)

        viewModel.getExoPlayer(context, "https://cdn/a.mp4")
        viewModel.getExoPlayer(context, "https://cdn/a.mp4")

        verify(exactly = 1) { player.seekToDefaultPosition() }
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

    @Test
    fun getExoPlayer_firstTalk_startsFromTheTopAndPlays() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        val viewModel = playerViewModel(player)

        viewModel.getExoPlayer(mockk(relaxed = true), "https://cdn/a.mp4")

        verify(exactly = 0) { player.seekTo(any<Long>()) }
        verify { player.playWhenReady = true }
    }

    // --- player lifecycle: one player per view model, paused on deselect, released on clear ---

    @Test
    fun getExoPlayer_switchingTalks_keepsOnePlayer() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        var built = 0
        val viewModel = TedTalksViewModel(FakeTedTalksRepository(Result.success(sampleTalks))) {
            built++
            player
        }
        val context = mockk<Context>(relaxed = true)

        viewModel.getExoPlayer(context, "https://cdn/a.mp4")
        viewModel.getExoPlayer(context, "https://cdn/b.mp4")

        assertEquals(1, built)
        verify(exactly = 2) { player.setMediaItem(any()) }
    }

    @Test
    fun clearSelection_pausesThePlayer() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        val viewModel = playerViewModel(player)
        viewModel.getExoPlayer(mockk(relaxed = true), "https://cdn/a.mp4")
        viewModel.selectTalk("1")

        viewModel.clearSelection()

        verify(exactly = 1) { player.pause() }
        verify(exactly = 0) { player.release() }
    }

    @Test
    fun clearSelection_beforeAnyPlayback_doesNotBuildAPlayer() = runTest {
        var built = 0
        val viewModel = TedTalksViewModel(FakeTedTalksRepository(Result.success(sampleTalks))) {
            built++
            mockk(relaxed = true)
        }
        viewModel.selectTalk("1")

        viewModel.clearSelection()

        assertEquals(0, built)
    }

    @Test
    fun onCleared_releasesThePlayer() = runTest {
        val player = mockk<ExoPlayer>(relaxed = true)
        val viewModel = playerViewModel(player)
        viewModel.getExoPlayer(mockk(relaxed = true), "https://cdn/a.mp4")
        val store = ViewModelStore().apply { put("talks", viewModel) }

        store.clear()   // the owner is gone: onCleared

        verify(exactly = 1) { player.release() }
    }

    @Test
    fun onCleared_withoutAPlayer_hasNothingToRelease() = runTest {
        var built = 0
        val viewModel = TedTalksViewModel(FakeTedTalksRepository(Result.success(sampleTalks))) {
            built++
            mockk(relaxed = true)
        }
        val store = ViewModelStore().apply { put("talks", viewModel) }

        store.clear()

        assertEquals(0, built)
    }
}
