package com.example.sermonindex.ui.library

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.RecentMediaItem
import com.example.sermonindex.domain.usecase.ClearHistoryUseCase
import com.example.sermonindex.domain.usecase.DeleteAllDownloadsUseCase
import com.example.sermonindex.domain.usecase.DeleteBibleBookmarkUseCase
import com.example.sermonindex.domain.usecase.DeleteDownloadUseCase
import com.example.sermonindex.domain.usecase.GetAllBibleBookmarksUseCase
import com.example.sermonindex.domain.usecase.GetDownloadsUseCase
import com.example.sermonindex.domain.usecase.GetFavoritesUseCase
import com.example.sermonindex.domain.usecase.GetHistoryUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.SaveBibleReadingPositionUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class LibraryUiState(
    val selectedTab: Int = 0, // 0: Favorites, 1: Downloads, 2: History, 3: Bookmarks
    val favorites: List<FavoriteSermon> = emptyList(),
    val downloads: List<DownloadedSermon> = emptyList(),
    val totalDownloadsSizeFormatted: String = "0 MB",
    val history: List<ListeningHistory> = emptyList(),
    val bookmarks: List<BibleBookmark> = emptyList(),
    val recentItems: List<RecentMediaItem> = emptyList()
)

sealed interface LibraryUiEvent {
    data class SelectTab(val tab: Int) : LibraryUiEvent
    data class PlayFavorite(val favorite: FavoriteSermon) : LibraryUiEvent
    data class RemoveFavorite(val favorite: FavoriteSermon) : LibraryUiEvent
    data class PlayDownload(val download: DownloadedSermon) : LibraryUiEvent
    data class DeleteDownload(val download: DownloadedSermon) : LibraryUiEvent
    data object DeleteAllDownloads : LibraryUiEvent
    data class ResumeHistory(val history: ListeningHistory) : LibraryUiEvent
    data object ClearHistory : LibraryUiEvent
    data class SelectSermon(val sermonId: String) : LibraryUiEvent
    data class DeleteBookmark(val id: String) : LibraryUiEvent
    data class OpenBookmarkInBible(val bookmark: BibleBookmark) : LibraryUiEvent
    data class PlayRecent(val item: RecentMediaItem) : LibraryUiEvent
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val deleteAllDownloadsUseCase: DeleteAllDownloadsUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getSermonDetailsUseCase: GetSermonDetailsUseCase,
    private val getAllBibleBookmarksUseCase: GetAllBibleBookmarksUseCase,
    private val deleteBibleBookmarkUseCase: DeleteBibleBookmarkUseCase,
    private val saveBibleReadingPositionUseCase: SaveBibleReadingPositionUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeFavoritesAndHistory()
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
                _uiState.update { current ->
                    current.copy(
                        downloads = dls,
                        totalDownloadsSizeFormatted = formatted,
                        recentItems = buildRecentItems(current.history, dls)
                    )
                }
            }
        }
        viewModelScope.launch {
            getHistoryUseCase().collect { hist ->
                _uiState.update { current ->
                    current.copy(
                        history = hist,
                        recentItems = buildRecentItems(hist, current.downloads)
                    )
                }
            }
        }
        viewModelScope.launch {
            getAllBibleBookmarksUseCase().collect { bms ->
                _uiState.update { it.copy(bookmarks = bms) }
            }
        }
    }

    private fun buildRecentItems(
        history: List<ListeningHistory>,
        downloads: List<DownloadedSermon>
    ): List<RecentMediaItem> {
        val downloadedIds = downloads.map { it.id }.toSet()
        val items = mutableListOf<RecentMediaItem>()
        val seenIds = mutableSetOf<String>()

        for (hist in history) {
            if (seenIds.add(hist.sermonId)) {
                items.add(
                    RecentMediaItem(
                        id = hist.sermonId,
                        title = hist.title,
                        speaker = hist.speaker,
                        speakerImage = hist.speakerImage,
                        isDownloaded = downloadedIds.contains(hist.sermonId),
                        lastPositionMs = hist.lastPositionMs,
                        durationMs = hist.durationMs
                    )
                )
            }
            if (items.size >= 12) break
        }

        return items
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
            is LibraryUiEvent.DeleteAllDownloads -> {
                viewModelScope.launch {
                    deleteAllDownloadsUseCase()
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
                    clearHistoryUseCase()
                }
            }
            is LibraryUiEvent.DeleteBookmark -> {
                viewModelScope.launch {
                    deleteBibleBookmarkUseCase(event.id)
                }
            }
            is LibraryUiEvent.OpenBookmarkInBible -> {
                viewModelScope.launch {
                    saveBibleReadingPositionUseCase(
                        translationId = event.bookmark.translationId,
                        bookId = event.bookmark.bookId,
                        chapter = event.bookmark.chapter,
                        verse = event.bookmark.verse
                    )
                }
            }
            is LibraryUiEvent.PlayRecent -> {
                viewModelScope.launch {
                    val downloaded = _uiState.value.downloads.find { it.id == event.item.id }
                    if (downloaded != null) {
                        playbackManager.playSermon(downloaded.toSermon(), startPositionMs = event.item.lastPositionMs)
                    } else {
                        getSermonDetailsUseCase(event.item.id).onSuccess { detail ->
                            playbackManager.playSermon(detail.toSermon(), startPositionMs = event.item.lastPositionMs)
                        }
                    }
                }
            }
            is LibraryUiEvent.SelectSermon -> Unit
        }
    }
}
