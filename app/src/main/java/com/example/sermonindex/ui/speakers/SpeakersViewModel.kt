package com.example.sermonindex.ui.speakers

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.di.IoDispatcher
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.usecase.GetSpeakersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class SpeakersUiState(
    val speakers: List<Speaker> = emptyList(),
    val filteredSpeakers: List<Speaker> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface SpeakersUiEvent {
    data class UpdateSearchQuery(val query: String) : SpeakersUiEvent
    data object Retry : SpeakersUiEvent
    data class SelectSpeaker(val slug: String) : SpeakersUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SpeakersViewModel @Inject constructor(
    private val getSpeakersUseCase: GetSpeakersUseCase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakersUiState())
    val uiState: StateFlow<SpeakersUiState> = _uiState.asStateFlow()

    private val allSpeakersFlow = MutableStateFlow<List<Speaker>>(emptyList())
    private val searchQueryFlow = MutableStateFlow("")

    init {
        observeFilteredSpeakers()
        loadSpeakers()
    }

    fun onEvent(event: SpeakersUiEvent) {
        when (event) {
            is SpeakersUiEvent.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                searchQueryFlow.value = event.query
            }
            is SpeakersUiEvent.Retry -> loadSpeakers()
            is SpeakersUiEvent.SelectSpeaker -> Unit
        }
    }

    private fun observeFilteredSpeakers() {
        viewModelScope.launch {
            combine(
                allSpeakersFlow,
                searchQueryFlow
            ) { speakers, query ->
                speakers to query.trim()
            }.mapLatest { (speakers, cleanQuery) ->
                if (cleanQuery.isEmpty()) {
                    speakers
                } else {
                    withContext(ioDispatcher) {
                        speakers.filter { speaker ->
                            speaker.name.contains(cleanQuery, ignoreCase = true)
                        }
                    }
                }
            }.collect { filtered ->
                _uiState.update { it.copy(filteredSpeakers = filtered) }
            }
        }
    }

    private fun loadSpeakers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getSpeakersUseCase()
                .onSuccess { list ->
                    allSpeakersFlow.value = list
                    _uiState.update {
                        it.copy(
                            speakers = list,
                            filteredSpeakers = if (it.searchQuery.isBlank()) list else it.filteredSpeakers,
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
