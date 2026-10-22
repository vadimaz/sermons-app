package com.example.sermonindex.ui.reader

import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.repository.SermonRepository
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.IsFavoriteUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TextSermonViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeDetail = SermonDetail(
        id = "spurgeon-1",
        title = "Faith and Life",
        speaker = "Charles Spurgeon",
        transcript = "Faith is the victory that overcomes the world.\n\nWithout faith it is impossible to please God.\n\nLiving faith produces holy works."
    )

    private val fakeRepo = object : SermonRepository {
        override val favorites = emptyFlow<List<com.example.sermonindex.domain.model.FavoriteSermon>>()
        override val history = emptyFlow<List<com.example.sermonindex.domain.model.ListeningHistory>>()
        override val downloads = emptyFlow<List<com.example.sermonindex.domain.model.DownloadedSermon>>()
        override fun isFavorite(id: String) = flowOf(false)
        override suspend fun toggleFavorite(sermon: Sermon) {}
        override fun isDownloaded(sermonId: String) = flowOf(false)
        override fun getDownloadProgress(sermonId: String) = flowOf(null)
        override suspend fun downloadSermon(sermon: Sermon) {}
        override suspend fun cancelDownload(sermonId: String) {}
        override suspend fun deleteDownload(sermonId: String) {}
        override suspend fun deleteAllDownloads() {}
        override suspend fun getLocalAudioPath(sermonId: String) = null
        override suspend fun updateHistory(sermonId: String, title: String, speaker: String, speakerImage: String, positionMs: Long, durationMs: Long) {}
        override suspend fun getPopularSermons() = Result.success(emptyList<Sermon>())
        override suspend fun getScriptureBooks() = Result.success(emptyList<com.example.sermonindex.domain.model.BibleBook>())
        override suspend fun getScriptureChapter(bookId: String, chapter: Int) = Result.failure<com.example.sermonindex.domain.model.ChapterDetails>(NotImplementedError())
        override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int) = Result.success(emptyList<Sermon>())
        override suspend fun getAvailableBibleTranslations() = Result.success(emptyList<com.example.sermonindex.domain.model.BibleTranslation>())
        override suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int) = Result.success("")
        override suspend fun getSpeakers() = Result.success(emptyList<com.example.sermonindex.domain.model.Speaker>())
        override suspend fun getSpeakerDetails(slug: String) = Result.failure<com.example.sermonindex.domain.model.Speaker>(NotImplementedError())
        override suspend fun getSpeakerSermonsPage(slug: String, page: Int) = Result.failure<com.example.sermonindex.domain.model.SpeakerPagedSermons>(NotImplementedError())
        override suspend fun getTopics() = Result.success(emptyList<com.example.sermonindex.domain.model.Topic>())
        override suspend fun getTopicDetails(slug: String) = Result.failure<com.example.sermonindex.domain.model.TopicDetail>(NotImplementedError())
        override suspend fun getSermonDetails(id: String) = Result.success(fakeDetail)
        override suspend fun getTranscriptCues(vttUrl: String) = Result.success(emptyList<com.example.sermonindex.domain.model.TranscriptCue>())
        override val bibleReadingPreferences = emptyFlow<com.example.sermonindex.domain.model.BibleReadingPreferences>()
        override suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int) = Result.failure<com.example.sermonindex.domain.model.BibleChapter>(NotImplementedError())
        override suspend fun getBibleBooksForTranslation(translationId: String) = Result.success(emptyList<com.example.sermonindex.domain.model.BibleBook>())
        override suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int) {}
        override suspend fun saveBibleTypographyPreferences(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float) {}
        override suspend fun saveBibleNarrator(narrator: String?) {}
        override suspend fun clearHistory() {}
        override fun search(query: String, category: com.example.sermonindex.domain.model.SearchCategory) = emptyFlow<com.example.sermonindex.domain.model.SearchAggregateResult>()
        override val catalogSyncState = emptyFlow<com.example.sermonindex.domain.model.CatalogSyncState>()
        override suspend fun downloadOrUpdateCatalog() {}
        override suspend fun checkForCatalogUpdate(): Boolean = false
        override val recentSearchQueries = emptyFlow<List<String>>()
        override suspend fun addRecentSearchQuery(query: String) {}
        override suspend fun deleteRecentSearchQuery(query: String) {}
        override suspend fun clearRecentSearchQueries() {}
        override val isCatalogReadyBannerDismissed = emptyFlow<Boolean>()
        override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDetails_splitsParagraphsAndPreservesHighlightQuery() = runTest(testDispatcher) {
        val viewModel = TextSermonViewModel(
            sermonId = "spurgeon-1",
            initialHighlightQuery = "faith",
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepo),
            isFavoriteUseCase = IsFavoriteUseCase(fakeRepo),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepo)
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(3, state.paragraphs.size)
        assertEquals("faith", state.highlightQuery)
        assertEquals("Faith is the victory that overcomes the world.", state.paragraphs[0])
    }
}
