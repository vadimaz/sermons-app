package com.example.sermonindex.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.SearchAggregateResult
import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.domain.usecase.AddRecentSearchQueryUseCase
import com.example.sermonindex.domain.usecase.CheckCatalogUpdateUseCase
import com.example.sermonindex.domain.usecase.ClearRecentSearchQueriesUseCase
import com.example.sermonindex.domain.usecase.DeleteRecentSearchQueryUseCase
import com.example.sermonindex.domain.usecase.DownloadOrUpdateCatalogUseCase
import com.example.sermonindex.domain.usecase.GetCatalogPromptBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.GetCatalogReadyBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.GetCatalogSyncStateUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetRecentSearchQueriesUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.GetSpeakersUseCase
import com.example.sermonindex.domain.usecase.GetTopicsUseCase
import com.example.sermonindex.domain.usecase.SearchUseCase
import com.example.sermonindex.domain.usecase.SetCatalogPromptBannerDismissedUseCase
import com.example.sermonindex.domain.usecase.SetCatalogReadyBannerDismissedUseCase
import com.example.sermonindex.di.IoDispatcher
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase,
    private val getCatalogSyncStateUseCase: GetCatalogSyncStateUseCase,
    private val downloadOrUpdateCatalogUseCase: DownloadOrUpdateCatalogUseCase,
    private val checkCatalogUpdateUseCase: CheckCatalogUpdateUseCase,
    private val getRecentSearchQueriesUseCase: GetRecentSearchQueriesUseCase,
    private val addRecentSearchQueryUseCase: AddRecentSearchQueryUseCase,
    private val deleteRecentSearchQueryUseCase: DeleteRecentSearchQueryUseCase,
    private val clearRecentSearchQueriesUseCase: ClearRecentSearchQueriesUseCase,
    private val getSpeakersUseCase: GetSpeakersUseCase,
    private val getTopicsUseCase: GetTopicsUseCase,
    private val getCatalogReadyBannerDismissedUseCase: GetCatalogReadyBannerDismissedUseCase,
    private val setCatalogReadyBannerDismissedUseCase: SetCatalogReadyBannerDismissedUseCase,
    private val getCatalogPromptBannerDismissedUseCase: GetCatalogPromptBannerDismissedUseCase,
    private val setCatalogPromptBannerDismissedUseCase: SetCatalogPromptBannerDismissedUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val playbackManager: PlaybackManager,
    private val getSermonDetailsUseCase: GetSermonDetailsUseCase,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryInputFlow = MutableStateFlow("")
    private val categoryInputFlow = MutableStateFlow(SearchCategory.ALL)

    init {
        // Observe banner dismissal state
        viewModelScope.launch {
            getCatalogReadyBannerDismissedUseCase().collect { isDismissed ->
                _uiState.update { it.copy(isCatalogReadyBannerDismissed = isDismissed) }
            }
        }
        viewModelScope.launch {
            getCatalogPromptBannerDismissedUseCase().collect { isDismissed ->
                _uiState.update { it.copy(isCatalogPromptBannerDismissed = isDismissed) }
            }
        }

        // Observe favorites
        viewModelScope.launch {
            getFavoritesUseCase().collect { favorites ->
                val favIds = favorites.map { it.id }.toSet()
                _uiState.update { it.copy(favoriteIds = favIds) }
            }
        }

        // Observe catalog sync state
        viewModelScope.launch {
            getCatalogSyncStateUseCase().collect { state ->
                _uiState.update { it.copy(catalogState = state) }
            }
        }

        // Observe recent searches
        viewModelScope.launch {
            getRecentSearchQueriesUseCase().collect { queries ->
                _uiState.update { it.copy(recentQueries = queries) }
            }
        }

        // Preload suggestions (popular speakers and topics) off the Main thread
        viewModelScope.launch {
            val (speakers, topics) = withContext(ioDispatcher) {
                val topSpeakers = getSpeakersUseCase().getOrNull()
                    ?.sortedByDescending { it.sermonCount }
                    ?.take(10)
                    ?: emptyList()
                val topTopics = getTopicsUseCase().getOrNull()
                    ?.sortedByDescending { it.count }
                    ?.take(12)
                    ?: emptyList()
                topSpeakers to topTopics
            }
            _uiState.update {
                it.copy(
                    popularSpeakers = speakers,
                    popularTopics = topics
                )
            }
        }

        // Check for catalog update in background
        viewModelScope.launch {
            checkCatalogUpdateUseCase()
        }

        // Debounced search query flow (instant on clear or category switch)
        viewModelScope.launch {
            combine(
                queryInputFlow
                    .debounce { if (it.isBlank()) 0L else 200L }
                    .distinctUntilChanged(),
                categoryInputFlow
            ) { query, category ->
                query to category
            }.flatMapLatest { (query, category) ->
                if (query.trim().isBlank()) {
                    _uiState.update { it.copy(isSearching = false, searchResult = SearchAggregateResult()) }
                    flowOf(SearchAggregateResult())
                } else {
                    _uiState.update { it.copy(isSearching = true) }
                    searchUseCase(query, category)
                }
            }.collect { result ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResult = result
                    )
                }
            }
        }
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.UpdateQuery -> {
                _uiState.update { it.copy(query = event.query) }
                queryInputFlow.value = event.query
            }

            is SearchUiEvent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
                categoryInputFlow.value = event.category
            }

            is SearchUiEvent.ClearQuery -> {
                _uiState.update { it.copy(query = "", searchResult = SearchAggregateResult()) }
                queryInputFlow.value = ""
            }

            is SearchUiEvent.ExecuteSearch -> {
                val clean = event.query.trim()
                if (clean.isNotBlank()) {
                    viewModelScope.launch {
                        addRecentSearchQueryUseCase(clean)
                    }
                }
                _uiState.update { it.copy(query = event.query) }
                queryInputFlow.value = event.query
            }

            is SearchUiEvent.DownloadCatalog -> {
                viewModelScope.launch {
                    downloadOrUpdateCatalogUseCase()
                }
            }

            is SearchUiEvent.CheckCatalogUpdate -> {
                viewModelScope.launch {
                    checkCatalogUpdateUseCase()
                }
            }

            is SearchUiEvent.DeleteRecentQuery -> {
                viewModelScope.launch {
                    deleteRecentSearchQueryUseCase(event.query)
                }
            }

            is SearchUiEvent.ClearRecentQueries -> {
                viewModelScope.launch {
                    clearRecentSearchQueriesUseCase()
                }
            }

            is SearchUiEvent.DismissCatalogReadyBanner -> {
                viewModelScope.launch {
                    setCatalogReadyBannerDismissedUseCase(true)
                }
            }

            is SearchUiEvent.DismissCatalogPromptBanner -> {
                viewModelScope.launch {
                    setCatalogPromptBannerDismissedUseCase(true)
                }
            }

            is SearchUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.sermon)
                }
            }

            is SearchUiEvent.PlaySermon -> {
                val sermon = event.sermon
                if (sermon.mp3Url.isNotBlank() || sermon.cdnMp3Url.isNotBlank()) {
                    playbackManager.playSermon(sermon)
                } else {
                    viewModelScope.launch {
                        getSermonDetailsUseCase(sermon.id)
                            .onSuccess { detail ->
                                playbackManager.playSermon(detail.toSermon())
                            }
                            .onFailure {
                                playbackManager.playSermon(sermon)
                            }
                    }
                }
            }
        }
    }
}
