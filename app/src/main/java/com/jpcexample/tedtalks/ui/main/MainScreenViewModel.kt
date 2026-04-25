package com.jpcexample.tedtalks.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    }
}

sealed interface TedTalksUiState {
    data object Loading : TedTalksUiState
    data class Success(val talks: List<TalkItem>) : TedTalksUiState
    data class Error(val message: String) : TedTalksUiState
}
