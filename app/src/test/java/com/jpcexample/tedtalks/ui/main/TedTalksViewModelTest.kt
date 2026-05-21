package com.jpcexample.tedtalks.ui.main

import com.jpcexample.tedtalks.data.FakeTedTalksRepository
import com.jpcexample.tedtalks.data.TalkItem
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
}
