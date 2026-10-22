package com.example.sermonindex.ui.search

import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.domain.model.Sermon

sealed interface SearchUiEvent {
    data class UpdateQuery(val query: String) : SearchUiEvent
    data class SelectCategory(val category: SearchCategory) : SearchUiEvent
    data object ClearQuery : SearchUiEvent
    data object DownloadCatalog : SearchUiEvent
    data object CheckCatalogUpdate : SearchUiEvent
    data class DeleteRecentQuery(val query: String) : SearchUiEvent
    data object ClearRecentQueries : SearchUiEvent
    data class ExecuteSearch(val query: String) : SearchUiEvent
    data object DismissCatalogReadyBanner : SearchUiEvent
    data class ToggleFavorite(val sermon: Sermon) : SearchUiEvent
    data class PlaySermon(val sermon: Sermon) : SearchUiEvent
}
