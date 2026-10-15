package com.example.sermonindex.ui.speakers

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.usecase.GetSpeakersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SpeakersUiState(
    val speakers: List<Speaker> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val filteredSpeakers: List<Speaker>
        get() = if (searchQuery.isBlank()) {
            speakers
        } else {
            speakers.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
}

sealed interface SpeakersUiEvent {
    data class UpdateSearchQuery(val query: String) : SpeakersUiEvent
    data object Retry : SpeakersUiEvent
    data class SelectSpeaker(val slug: String) : SpeakersUiEvent
}

@HiltViewModel
class SpeakersViewModel @Inject constructor(
    private val getSpeakersUseCase: GetSpeakersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakersUiState())
    val uiState: StateFlow<SpeakersUiState> = _uiState.asStateFlow()

    init {
        loadSpeakers()
    }

    fun onEvent(event: SpeakersUiEvent) {
        when (event) {
            is SpeakersUiEvent.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is SpeakersUiEvent.Retry -> loadSpeakers()
            is SpeakersUiEvent.SelectSpeaker -> Unit
        }
    }

    private fun loadSpeakers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getSpeakersUseCase()
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            speakers = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load preachers directory."
                        )
                    }
                }
        }
    }
}
