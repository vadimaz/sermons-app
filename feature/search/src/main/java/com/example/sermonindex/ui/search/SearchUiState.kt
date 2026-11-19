package com.example.sermonindex.ui.search

import androidx.compose.runtime.Immutable
import com.example.sermonindex.domain.model.CatalogSyncState
import com.example.sermonindex.domain.model.SearchAggregateResult
import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.Topic

@Immutable
data class SearchUiState(
    val query: String = "",
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val isSearching: Boolean = false,
    val searchResult: SearchAggregateResult = SearchAggregateResult(),
    val favoriteIds: Set<String> = emptySet(),
    val catalogState: CatalogSyncState = CatalogSyncState.NotDownloaded,
    val isCatalogReadyBannerDismissed: Boolean = false,
    val isCatalogPromptBannerDismissed: Boolean = false,
    val recentQueries: List<String> = emptyList(),
    val popularSpeakers: List<Speaker> = emptyList(),
    val popularTopics: List<Topic> = emptyList()
)
