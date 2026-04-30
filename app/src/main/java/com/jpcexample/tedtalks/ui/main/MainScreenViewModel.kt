package com.jpcexample.tedtalks.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.jpcexample.tedtalks.data.TalkItem
import com.jpcexample.tedtalks.data.TedTalksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TedTalksViewModel : ViewModel() {

    private val repository = TedTalksRepository()

    private val _uiState = MutableStateFlow<TedTalksUiState>(TedTalksUiState.Loading)
    val uiState: StateFlow<TedTalksUiState> = _uiState.asStateFlow()

    private val _selectedTalkId = MutableStateFlow<String?>(null)
    val selectedTalkId: StateFlow<String?> = _selectedTalkId.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var currentVideoUrl: String? = null

    init {
        loadTalks()
    }

    fun loadTalks() {
        viewModelScope.launch {
            _uiState.value = TedTalksUiState.Loading
            repository.fetchTalks()
                .onSuccess { _uiState.value = TedTalksUiState.Success(it) }
                .onFailure { _uiState.value = TedTalksUiState.Error(it.message ?: "Unknown error") }
        }
    }

    fun selectTalk(id: String) {
        _selectedTalkId.value = id
    }

    fun clearSelection() {
        _selectedTalkId.value = null
        exoPlayer?.pause()
    }

    fun getExoPlayer(context: Context, videoUrl: String): ExoPlayer {
        val player = exoPlayer ?: ExoPlayer.Builder(context.applicationContext).build().also {
            exoPlayer = it
        }
        if (currentVideoUrl != videoUrl) {
            currentVideoUrl = videoUrl
            player.setMediaItem(MediaItem.fromUri(videoUrl))
            player.prepare()
            player.playWhenReady = true
        }
        return player
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}

sealed interface TedTalksUiState {
    data object Loading : TedTalksUiState
    data class Success(val talks: List<TalkItem>) : TedTalksUiState
    data class Error(val message: String) : TedTalksUiState
}
