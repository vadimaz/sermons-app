package com.example.sermonindex.ui.library

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.sermonindex.data.sync.CatalogSyncWorker
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.OfflineSearchResult
import com.example.sermonindex.domain.repository.SermonRepository
import com.example.sermonindex.domain.usecase.DeleteDownloadUseCase
import com.example.sermonindex.domain.usecase.GetDownloadsUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetHistoryUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.OfflineSearchUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class LibraryUiState(
    val selectedTab: Int = 0, // 0: Favorites, 1: Downloads, 2: History, 3: Offline Search
    val favorites: List<FavoriteSermon> = emptyList(),
    val downloads: List<DownloadedSermon> = emptyList(),
    val totalDownloadsSizeFormatted: String = "0 MB",
    val history: List<ListeningHistory> = emptyList(),
    val indexedCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncProgress: Int = 0,
    val syncCount: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<OfflineSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

sealed interface LibraryUiEvent {
    data class SelectTab(val tab: Int) : LibraryUiEvent
    data class PlayFavorite(val favorite: FavoriteSermon) : LibraryUiEvent
    data class RemoveFavorite(val favorite: FavoriteSermon) : LibraryUiEvent
    data class PlayDownload(val download: DownloadedSermon) : LibraryUiEvent
    data class DeleteDownload(val download: DownloadedSermon) : LibraryUiEvent
    data class ResumeHistory(val history: ListeningHistory) : LibraryUiEvent
    data object ClearHistory : LibraryUiEvent
    data class UpdateSearchQuery(val query: String) : LibraryUiEvent
    data object StartSync : LibraryUiEvent
    data object ClearIndex : LibraryUiEvent
    data class PlayOfflineResult(val result: OfflineSearchResult) : LibraryUiEvent
    data class SelectSermon(val sermonId: String) : LibraryUiEvent
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getSermonDetailsUseCase: GetSermonDetailsUseCase,
    private val offlineSearchUseCase: OfflineSearchUseCase,
    private val sermonRepository: SermonRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeFavoritesAndHistory()
        observeWorkManager()
    }

    private fun observeFavoritesAndHistory() {
        viewModelScope.launch {
            getFavoritesUseCase().collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
        viewModelScope.launch {
            getDownloadsUseCase().collect { dls ->
                val totalBytes = dls.sumOf { it.fileSizeBytes }
                val mb = totalBytes.toDouble() / (1024.0 * 1024.0)
                val formatted = if (mb >= 1.0) "${(mb * 10).toLong() / 10.0} MB" else "${totalBytes / 1024} KB"
                _uiState.update { it.copy(downloads = dls, totalDownloadsSizeFormatted = formatted) }
            }
        }
        viewModelScope.launch {
            getHistoryUseCase().collect { hist ->
                _uiState.update { it.copy(history = hist) }
            }
        }
        viewModelScope.launch {
            sermonRepository.ftsIndexedCount.collect { count ->
                _uiState.update { it.copy(indexedCount = count) }
            }
        }
    }

    private fun observeWorkManager() {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(CatalogSyncWorker.WORK_NAME)
                .collect { workInfos ->
                    val activeWorkInfo = workInfos.firstOrNull()
                    val isSyncing = activeWorkInfo?.state == WorkInfo.State.RUNNING
                    val progress = activeWorkInfo?.progress?.getInt(CatalogSyncWorker.KEY_PROGRESS, 0) ?: 0
                    val count = activeWorkInfo?.progress?.getInt(CatalogSyncWorker.KEY_COUNT, 0) ?: 0
                    _uiState.update {
                        it.copy(
                            isSyncing = isSyncing,
                            syncProgress = progress,
                            syncCount = count
                        )
                    }
                }
        }
    }

    fun onEvent(event: LibraryUiEvent) {
        when (event) {
            is LibraryUiEvent.SelectTab -> _uiState.update { it.copy(selectedTab = event.tab) }
            is LibraryUiEvent.PlayFavorite -> {
                viewModelScope.launch {
                    getSermonDetailsUseCase(event.favorite.id).onSuccess { detail ->
                        playbackManager.playSermon(detail.toSermon())
                    }
                }
            }
            is LibraryUiEvent.RemoveFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.favorite.toSermon())
                }
            }
            is LibraryUiEvent.PlayDownload -> {
                playbackManager.playSermon(event.download.toSermon())
            }
            is LibraryUiEvent.DeleteDownload -> {
                viewModelScope.launch {
                    deleteDownloadUseCase(event.download.id)
                }
            }
            is LibraryUiEvent.ResumeHistory -> {
                viewModelScope.launch {
                    getSermonDetailsUseCase(event.history.sermonId).onSuccess { detail ->
                        playbackManager.playSermon(detail.toSermon(), startPositionMs = event.history.lastPositionMs)
                    }
                }
            }
            is LibraryUiEvent.ClearHistory -> {
                viewModelScope.launch {
                    sermonRepository.clearHistory()
                }
            }
            is LibraryUiEvent.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                performSearch(event.query)
            }
            is LibraryUiEvent.StartSync -> {
                val workRequest = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    CatalogSyncWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
            is LibraryUiEvent.ClearIndex -> {
                viewModelScope.launch {
                    sermonRepository.clearOfflineIndex()
                }
            }
            is LibraryUiEvent.PlayOfflineResult -> {
                viewModelScope.launch {
                    getSermonDetailsUseCase(event.result.sermonId).onSuccess { detail ->
                        playbackManager.playSermon(detail.toSermon())
                    }
                }
            }
            is LibraryUiEvent.SelectSermon -> Unit
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank() || _uiState.value.indexedCount <= 0) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = offlineSearchUseCase(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }
}
