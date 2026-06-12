package com.jpcexample.tedtalks.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jpcexample.tedtalks.data.DefaultTedTalksRepository
import com.jpcexample.tedtalks.data.TalkItem
import com.jpcexample.tedtalks.data.TedTalksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TedTalksViewModel(
    private val repository: TedTalksRepository = DefaultTedTalksRepository(),
    private val playerFactory: (Context) -> ExoPlayer = ::defaultPlayerFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TedTalksUiState>(TedTalksUiState.Loading)
    val uiState: StateFlow<TedTalksUiState> = _uiState.asStateFlow()

    private val _selectedTalkId = MutableStateFlow<String?>(null)
    val selectedTalkId: StateFlow<String?> = _selectedTalkId.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var currentVideoUrl: String? = null

    // Last playback position per video URL, so reopening a talk resumes where it left off.
    private val playbackPositions = mutableMapOf<String, Long>()

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
        val player = exoPlayer ?: playerFactory(context).also {
            exoPlayer = it
        }
        if (currentVideoUrl != videoUrl) {
            savePlaybackPosition(player)
            currentVideoUrl = videoUrl
            player.setMediaItem(MediaItem.fromUri(videoUrl))
            playbackPositions[videoUrl]?.let { player.seekTo(it) }
            player.prepare()
            player.playWhenReady = true
        }
        return player
    }

    private fun savePlaybackPosition(player: ExoPlayer) {
        val url = currentVideoUrl ?: return
        if (player.playbackState == Player.STATE_ENDED) {
            // Finished talks restart from the beginning next time.
            playbackPositions.remove(url)
        } else {
            playbackPositions[url] = player.currentPosition
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        private fun defaultPlayerFactory(context: Context): ExoPlayer =
            ExoPlayer.Builder(context.applicationContext)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    /* handleAudioFocus = */ true,
                )
                .setHandleAudioBecomingNoisy(true)
                .build()

        fun factory(repository: TedTalksRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TedTalksViewModel(repository) as T
            }
    }
}

sealed interface TedTalksUiState {
    data object Loading : TedTalksUiState
    data class Success(val talks: List<TalkItem>) : TedTalksUiState
    data class Error(val message: String) : TedTalksUiState
}
