package com.example.sermonindex.ui.topics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.usecase.GetTopicsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class TopicsUiState(
    val topics: List<Topic> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val filteredTopics: List<Topic>
        get() = if (searchQuery.isBlank()) {
            topics
        } else {
            topics.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
}

sealed interface TopicsUiEvent {
    data class UpdateSearchQuery(val query: String) : TopicsUiEvent
    data object Retry : TopicsUiEvent
    data class SelectTopic(val slug: String) : TopicsUiEvent
}

@HiltViewModel
class TopicsViewModel @Inject constructor(
    private val getTopicsUseCase: GetTopicsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    init {
        loadTopics()
    }

    fun onEvent(event: TopicsUiEvent) {
        when (event) {
            is TopicsUiEvent.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is TopicsUiEvent.Retry -> loadTopics()
            is TopicsUiEvent.SelectTopic -> Unit
        }
    }

    private fun loadTopics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getTopicsUseCase()
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            topics = list,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load topics catalog."
                        )
                    }
                }
        }
    }
}
