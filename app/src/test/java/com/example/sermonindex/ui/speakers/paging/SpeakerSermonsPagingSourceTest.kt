package com.example.sermonindex.ui.speakers.paging

import androidx.paging.PagingSource
import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.SpeakerPagedSermons
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.model.TranscriptCue
import com.example.sermonindex.domain.repository.SermonRepository
import com.example.sermonindex.domain.usecase.GetSpeakerSermonsPageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerSermonsPagingSourceTest {

    private fun createFakeSermon(id: String) = Sermon(
        id = id,
        title = "Title $id",
        speaker = "Speaker",
        speakerImage = "",
        duration = "30:00",
        hasAudio = true,
        mp3Url = "https://example.com/$id.mp3",
        cdnMp3Url = ""
    )

    private fun createRepository(
        onGetSpeakerSermonsPage: (String, Int) -> Result<SpeakerPagedSermons>
    ): SermonRepository {
        return object : SermonRepository {
            override val favorites: Flow<List<FavoriteSermon>> = emptyFlow()
            override val history: Flow<List<ListeningHistory>> = emptyFlow()
            override val downloads: Flow<List<DownloadedSermon>> = emptyFlow()

            override fun isFavorite(id: String): Flow<Boolean> = emptyFlow()
            override suspend fun toggleFavorite(sermon: Sermon) {}
            override fun isDownloaded(sermonId: String): Flow<Boolean> = emptyFlow()
            override fun getDownloadProgress(sermonId: String): Flow<Int?> = emptyFlow()
            override suspend fun downloadSermon(sermon: Sermon) {}
            override suspend fun cancelDownload(sermonId: String) {}
            override suspend fun deleteDownload(sermonId: String) {}
            override suspend fun deleteAllDownloads() {}
            override suspend fun getLocalAudioPath(sermonId: String): String? = null
            override suspend fun updateHistory(sermonId: String, title: String, speaker: String, speakerImage: String, positionMs: Long, durationMs: Long) {}
            override suspend fun getPopularSermons(): Result<List<Sermon>> = Result.success(emptyList())
            override suspend fun getScriptureBooks(): Result<List<BibleBook>> = Result.success(emptyList())
            override suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails> = Result.failure(NotImplementedError())
            override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>> = Result.success(emptyList())
            override suspend fun getAvailableBibleTranslations(): Result<List<BibleTranslation>> = Result.success(emptyList())
            override suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int): Result<String> = Result.success("")
            override suspend fun getSpeakers(): Result<List<Speaker>> = Result.success(emptyList())
            override suspend fun getSpeakerDetails(slug: String): Result<Speaker> = Result.failure(NotImplementedError())
            override suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons> = onGetSpeakerSermonsPage(slug, page)
            override suspend fun getTopics(): Result<List<Topic>> = Result.success(emptyList())
            override suspend fun getTopicDetails(slug: String): Result<TopicDetail> = Result.failure(NotImplementedError())
            override suspend fun getSermonDetails(id: String): Result<SermonDetail> = Result.failure(NotImplementedError())
            override suspend fun getTranscriptCues(vttUrl: String): Result<List<TranscriptCue>> = Result.success(emptyList())
            override val bibleReadingPreferences: Flow<com.example.sermonindex.domain.model.BibleReadingPreferences> = emptyFlow()
            override suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int): Result<com.example.sermonindex.domain.model.BibleChapter> = Result.failure(NotImplementedError())
            override suspend fun getBibleBooksForTranslation(translationId: String): Result<List<BibleBook>> = Result.success(emptyList())
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
            override val isCatalogReadyBannerDismissed: Flow<Boolean> = emptyFlow()
            override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {}
        }
    }

    @Test
    fun load_pageOne_returnsPageWithNextKey() = runTest {
        val sermons = listOf(createFakeSermon("1"), createFakeSermon("2"))
        val repo = createRepository { slug, page ->
            assertEquals("test-speaker", slug)
            assertEquals(1, page)
            Result.success(
                SpeakerPagedSermons(
                    currentPage = 1,
                    totalPages = 3,
                    totalSermons = 100,
                    sermons = sermons
                )
            )
        }
        val useCase = GetSpeakerSermonsPageUseCase(repo)
        val pagingSource = SpeakerSermonsPagingSource(useCase, "test-speaker")

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 50,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(sermons, page.data)
        assertEquals(null, page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun load_lastPage_returnsPageWithNullNextKey() = runTest {
        val sermons = listOf(createFakeSermon("3"))
        val repo = createRepository { _, page ->
            Result.success(
                SpeakerPagedSermons(
                    currentPage = page,
                    totalPages = 3,
                    totalSermons = 100,
                    sermons = sermons
                )
            )
        }
        val useCase = GetSpeakerSermonsPageUseCase(repo)
        val pagingSource = SpeakerSermonsPagingSource(useCase, "test-speaker")

        val result = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = 3,
                loadSize = 50,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(sermons, page.data)
        assertEquals(2, page.prevKey)
        assertEquals(null, page.nextKey)
    }

    @Test
    fun load_failure_returnsError() = runTest {
        val expectedError = RuntimeException("Network error")
        val repo = createRepository { _, _ ->
            Result.failure(expectedError)
        }
        val useCase = GetSpeakerSermonsPageUseCase(repo)
        val pagingSource = SpeakerSermonsPagingSource(useCase, "test-speaker")

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 50,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Error)
        val error = result as PagingSource.LoadResult.Error
        assertEquals(expectedError, error.throwable)
    }
}
