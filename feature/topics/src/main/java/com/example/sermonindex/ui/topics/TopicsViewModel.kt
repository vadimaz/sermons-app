package com.example.sermonindex.ui.topics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.di.IoDispatcher
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.usecase.GetTopicsUseCase
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
data class TopicsUiState(
    val topics: List<Topic> = emptyList(),
    val filteredTopics: List<Topic> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface TopicsUiEvent {
    data class UpdateSearchQuery(val query: String) : TopicsUiEvent
    data object Retry : TopicsUiEvent
    data class SelectTopic(val slug: String) : TopicsUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TopicsViewModel @Inject constructor(
    private val getTopicsUseCase: GetTopicsUseCase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    private val allTopicsFlow = MutableStateFlow<List<Topic>>(emptyList())
    private val searchQueryFlow = MutableStateFlow("")

    init {
        observeFilteredTopics()
        loadTopics()
    }

    fun onEvent(event: TopicsUiEvent) {
        when (event) {
            is TopicsUiEvent.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                searchQueryFlow.value = event.query
            }
            is TopicsUiEvent.Retry -> loadTopics()
            is TopicsUiEvent.SelectTopic -> Unit
        }
    }

    private fun observeFilteredTopics() {
        viewModelScope.launch {
            combine(
                allTopicsFlow,
                searchQueryFlow
            ) { topics, query ->
                topics to query.trim()
            }.mapLatest { (topics, cleanQuery) ->
                if (cleanQuery.isEmpty()) {
                    topics
                } else {
                    withContext(ioDispatcher) {
                        topics.filter { topic ->
                            topic.name.contains(cleanQuery, ignoreCase = true)
                        }
                    }
                }
            }.collect { filtered ->
                _uiState.update { it.copy(filteredTopics = filtered) }
            }
        }
    }

    private fun loadTopics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getTopicsUseCase()
                .onSuccess { list ->
                    allTopicsFlow.value = list
                    _uiState.update {
                        it.copy(
                            topics = list,
                            filteredTopics = if (it.searchQuery.isBlank()) list else it.filteredTopics,
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
