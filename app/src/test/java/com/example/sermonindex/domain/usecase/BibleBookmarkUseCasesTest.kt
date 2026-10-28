package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.model.HighlightColor
import com.example.sermonindex.domain.repository.SermonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleBookmarkUseCasesTest {

    private val sampleBookmark = BibleBookmark(
        id = "rus_syn:JHN:3:16",
        translationId = "rus_syn",
        bookId = "JHN",
        bookName = "Иоанна",
        chapter = 3,
        verse = 16,
        text = "Ибо так возлюбил Бог мир...",
        color = HighlightColor.YELLOW,
        createdAt = 1700000000000L
    )

    private val bookmarksFlow = MutableStateFlow<List<BibleBookmark>>(listOf(sampleBookmark))
    private val savedBookmarks = mutableListOf<BibleBookmark>()
    private val deletedIds = mutableListOf<String>()
    private val deletedVerses = mutableListOf<Triple<String, String, Pair<Int, Int>>>()

    private val fakeRepository = object : SermonRepository {
        override val bibleBookmarks: Flow<List<BibleBookmark>> = bookmarksFlow

        override fun getBibleBookmarksForChapter(
            translationId: String,
            bookId: String,
            chapter: Int
        ): Flow<List<BibleBookmark>> {
            return bookmarksFlow.map { list ->
                list.filter { it.translationId == translationId && it.bookId == bookId && it.chapter == chapter }
            }
        }

        override suspend fun saveBibleBookmark(bookmark: BibleBookmark) {
            savedBookmarks.add(bookmark)
            bookmarksFlow.value = bookmarksFlow.value + bookmark
        }

        override suspend fun deleteBibleBookmark(id: String) {
            deletedIds.add(id)
            bookmarksFlow.value = bookmarksFlow.value.filterNot { it.id == id }
        }

        override suspend fun deleteBibleBookmarkForVerse(
            translationId: String,
            bookId: String,
            chapter: Int,
            verse: Int
        ) {
            deletedVerses.add(Triple(translationId, bookId, Pair(chapter, verse)))
            bookmarksFlow.value = bookmarksFlow.value.filterNot {
                it.translationId == translationId && it.bookId == bookId && it.chapter == chapter && it.verse == verse
            }
        }

        override val favorites = flowOf(emptyList<com.example.sermonindex.domain.model.FavoriteSermon>())
        override val history = flowOf(emptyList<com.example.sermonindex.domain.model.ListeningHistory>())
        override val downloads = flowOf(emptyList<com.example.sermonindex.domain.model.DownloadedSermon>())
        override fun isFavorite(id: String) = flowOf(false)
        override suspend fun toggleFavorite(sermon: com.example.sermonindex.domain.model.Sermon) {}
        override fun isDownloaded(sermonId: String) = flowOf(false)
        override fun getDownloadProgress(sermonId: String) = flowOf(null)
        override suspend fun downloadSermon(sermon: com.example.sermonindex.domain.model.Sermon) {}
        override suspend fun cancelDownload(sermonId: String) {}
        override suspend fun deleteDownload(sermonId: String) {}
        override suspend fun deleteAllDownloads() {}
        override suspend fun getLocalAudioPath(sermonId: String): String? = null
        override suspend fun updateHistory(sermonId: String, title: String, speaker: String, speakerImage: String, positionMs: Long, durationMs: Long) {}
        override suspend fun clearHistory() {}
        override suspend fun getPopularSermons() = Result.success(emptyList<com.example.sermonindex.domain.model.Sermon>())
        override suspend fun getRandomSermons(shard: Int?) = Result.success(emptyList<com.example.sermonindex.domain.model.Sermon>())
        override suspend fun getScriptureBooks() = Result.success(emptyList<com.example.sermonindex.domain.model.BibleBook>())
        override suspend fun getScriptureChapter(bookId: String, chapter: Int) = Result.failure<com.example.sermonindex.domain.model.ChapterDetails>(NotImplementedError())
        override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int) = Result.success(emptyList<com.example.sermonindex.domain.model.Sermon>())
        override suspend fun getAvailableBibleTranslations() = Result.success(emptyList<com.example.sermonindex.domain.model.BibleTranslation>())
        override suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int) = Result.success("")
        override suspend fun getSpeakers() = Result.success(emptyList<com.example.sermonindex.domain.model.Speaker>())
        override suspend fun getSpeakerDetails(slug: String) = Result.failure<com.example.sermonindex.domain.model.Speaker>(NotImplementedError())
        override suspend fun getSpeakerSermonsPage(slug: String, page: Int) = Result.failure<com.example.sermonindex.domain.model.SpeakerPagedSermons>(NotImplementedError())
        override suspend fun getTopics() = Result.success(emptyList<com.example.sermonindex.domain.model.Topic>())
        override suspend fun getTopicDetails(slug: String) = Result.failure<com.example.sermonindex.domain.model.TopicDetail>(NotImplementedError())
        override suspend fun getSermonDetails(id: String) = Result.failure<com.example.sermonindex.domain.model.SermonDetail>(NotImplementedError())
        override suspend fun getTranscriptCues(vttUrl: String) = Result.success(emptyList<com.example.sermonindex.domain.model.TranscriptCue>())
        override val bibleReadingPreferences = flowOf(com.example.sermonindex.domain.model.BibleReadingPreferences())
        override suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int) = Result.failure<com.example.sermonindex.domain.model.BibleChapter>(NotImplementedError())
        override suspend fun getBibleBooksForTranslation(translationId: String) = Result.success(emptyList<com.example.sermonindex.domain.model.BibleBook>())
        override suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int) {}
        override suspend fun saveBibleTypographyPreferences(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float) {}
        override suspend fun saveBibleNarrator(narrator: String?) {}
        override fun search(query: String, category: com.example.sermonindex.domain.model.SearchCategory) = flowOf(com.example.sermonindex.domain.model.SearchAggregateResult())
        override val catalogSyncState = flowOf(com.example.sermonindex.domain.model.CatalogSyncState.NotDownloaded)
        override suspend fun downloadOrUpdateCatalog() {}
        override suspend fun checkForCatalogUpdate() = false
        override val recentSearchQueries = flowOf(emptyList<String>())
        override suspend fun addRecentSearchQuery(query: String) {}
        override suspend fun deleteRecentSearchQuery(query: String) {}
        override suspend fun clearRecentSearchQueries() {}
        override val isCatalogReadyBannerDismissed = flowOf(false)
        override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {}
    }

    @Test
    fun getAllBibleBookmarksUseCase_returnsBookmarksFlow() = runTest {
        val useCase = GetAllBibleBookmarksUseCase(fakeRepository)
        val result = useCase().first()
        assertEquals(1, result.size)
        assertEquals("rus_syn:JHN:3:16", result.first().id)
        assertEquals(16, result.first().verse)
        assertEquals(HighlightColor.YELLOW, result.first().color)
    }

    @Test
    fun getBibleBookmarksForChapterUseCase_filtersByChapter() = runTest {
        val useCase = GetBibleBookmarksForChapterUseCase(fakeRepository)
        val matching = useCase("rus_syn", "JHN", 3).first()
        assertEquals(1, matching.size)

        val nonMatching = useCase("rus_syn", "JHN", 4).first()
        assertTrue(nonMatching.isEmpty())
    }

    @Test
    fun saveBibleBookmarkUseCase_delegatesToRepository() = runTest {
        val useCase = SaveBibleBookmarkUseCase(fakeRepository)
        val newBookmark = sampleBookmark.copy(
            id = "rus_syn:ROM:8:28",
            bookId = "ROM",
            chapter = 8,
            verse = 28,
            color = HighlightColor.GREEN
        )
        useCase(newBookmark)
        assertEquals(1, savedBookmarks.size)
        assertEquals("rus_syn:ROM:8:28", savedBookmarks.first().id)
    }

    @Test
    fun deleteBibleBookmarkUseCase_delegatesToRepository() = runTest {
        val useCase = DeleteBibleBookmarkUseCase(fakeRepository)
        useCase("rus_syn:JHN:3:16")
        assertEquals(listOf("rus_syn:JHN:3:16"), deletedIds)
    }

    @Test
    fun deleteBibleBookmarkForVerseUseCase_delegatesToRepository() = runTest {
        val useCase = DeleteBibleBookmarkForVerseUseCase(fakeRepository)
        useCase("rus_syn", "JHN", 3, 16)
        assertEquals(1, deletedVerses.size)
        assertEquals(Triple("rus_syn", "JHN", Pair(3, 16)), deletedVerses.first())
    }
}
