package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleChapter
import com.example.sermonindex.domain.model.BibleChapterItem
import com.example.sermonindex.domain.model.BibleReadingPreferences
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleUseCasesTest {

    private val fakeChapter = BibleChapter(
        translationId = "BSB",
        bookId = "JHN",
        bookName = "John",
        chapterNumber = 3,
        numberOfVerses = 36,
        items = listOf(
            BibleChapterItem.Heading("Jesus and Nicodemus"),
            BibleChapterItem.Verse(number = 1, text = "Now there was a man of the Pharisees...", footnoteIds = emptyList()),
            BibleChapterItem.Verse(number = 16, text = "For God so loved the world...", footnoteIds = listOf(17))
        ),
        footnotes = emptyList(),
        audioLinks = mapOf("gilbert" to "https://example.com/audio.mp3"),
        hasNextChapter = true,
        hasPreviousChapter = true
    )

    private val fakeBooks = listOf(
        BibleBook(id = "GEN", name = "Бытие", testament = "OT", chapterCount = 50, order = 1),
        BibleBook(id = "JHN", name = "Евангелие от Иоанна", testament = "NT", chapterCount = 21, order = 43)
    )

    private var savedPosition: Triple<String, String, Int>? = null
    private var savedTypography: Triple<Float, Boolean, Float>? = null
    private var savedNarrator: String? = null

    private val fakeRepository = object : SermonRepository {
        override val favorites: Flow<List<FavoriteSermon>> = flowOf(emptyList())
        override val history: Flow<List<ListeningHistory>> = flowOf(emptyList())
        override val downloads: Flow<List<DownloadedSermon>> = flowOf(emptyList())

        override fun isFavorite(id: String): Flow<Boolean> = flowOf(false)
        override suspend fun toggleFavorite(sermon: Sermon) {}
        override fun isDownloaded(sermonId: String): Flow<Boolean> = flowOf(false)
        override fun getDownloadProgress(sermonId: String): Flow<Int?> = flowOf(null)
        override suspend fun downloadSermon(sermon: Sermon) {}
        override suspend fun cancelDownload(sermonId: String) {}
        override suspend fun deleteDownload(sermonId: String) {}
        override suspend fun deleteAllDownloads() {}
        override suspend fun getLocalAudioPath(sermonId: String): String? = null
        override suspend fun updateHistory(sermonId: String, title: String, speaker: String, speakerImage: String, positionMs: Long, durationMs: Long) {}
        override suspend fun getPopularSermons(): Result<List<Sermon>> = Result.success(emptyList())
        override suspend fun getScriptureBooks(): Result<List<BibleBook>> = Result.success(fakeBooks)
        override suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails> = Result.failure(NotImplementedError())
        override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>> = Result.success(emptyList())
        override suspend fun getAvailableBibleTranslations(): Result<List<BibleTranslation>> = Result.success(emptyList())
        override suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int): Result<String> = Result.success("")
        override suspend fun getSpeakers(): Result<List<Speaker>> = Result.success(emptyList())
        override suspend fun getSpeakerDetails(slug: String): Result<Speaker> = Result.failure(NotImplementedError())
        override suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons> = Result.failure(NotImplementedError())
        override suspend fun getTopics(): Result<List<Topic>> = Result.success(emptyList())
        override suspend fun getTopicDetails(slug: String): Result<TopicDetail> = Result.failure(NotImplementedError())
        override suspend fun getSermonDetails(id: String): Result<SermonDetail> = Result.failure(NotImplementedError())
        override suspend fun getTranscriptCues(vttUrl: String): Result<List<TranscriptCue>> = Result.success(emptyList())
        override suspend fun clearHistory() {}

        override val bibleReadingPreferences: Flow<BibleReadingPreferences> =
            flowOf(BibleReadingPreferences(translationId = "BSB", bookId = "JHN", chapter = 3))

        override suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int): Result<BibleChapter> =
            Result.success(fakeChapter)

        override suspend fun getBibleBooksForTranslation(translationId: String): Result<List<BibleBook>> =
            Result.success(fakeBooks)

        override suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int) {
            savedPosition = Triple(translationId, bookId, chapter)
        }

        override suspend fun saveBibleTypographyPreferences(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float) {
            savedTypography = Triple(fontSizeSp, isSerif, lineSpacingMultiplier)
        }

        override suspend fun saveBibleNarrator(narrator: String?) {
            savedNarrator = narrator
        }
    }

    @Test
    fun getBibleChapterUseCase_returnsSuccess() = runTest {
        val useCase = GetBibleChapterUseCase(fakeRepository)
        val result = useCase("BSB", "JHN", 3)
        assertTrue(result.isSuccess)
        val chapter = result.getOrNull()
        assertEquals("John", chapter?.bookName)
        assertEquals(3, chapter?.chapterNumber)
        assertEquals(3, chapter?.items?.size)
        assertEquals(mapOf("gilbert" to "https://example.com/audio.mp3"), chapter?.audioLinks)
    }

    @Test
    fun getBibleBooksForTranslationUseCase_returnsBooks() = runTest {
        val useCase = GetBibleBooksForTranslationUseCase(fakeRepository)
        val result = useCase("rus_syn")
        assertTrue(result.isSuccess)
        val books = result.getOrNull()
        assertEquals(2, books?.size)
        assertEquals("GEN", books?.first()?.id)
        assertEquals("Бытие", books?.first()?.name)
    }

    @Test
    fun getBibleReadingPreferencesUseCase_emitsPreferences() = runTest {
        val useCase = GetBibleReadingPreferencesUseCase(fakeRepository)
        val prefs = useCase().first()
        assertEquals("BSB", prefs.translationId)
        assertEquals("JHN", prefs.bookId)
        assertEquals(3, prefs.chapter)
    }

    @Test
    fun saveBibleReadingPositionUseCase_delegatesToRepository() = runTest {
        val useCase = SaveBibleReadingPositionUseCase(fakeRepository)
        useCase("rus_syn", "ROM", 8, 1)
        assertEquals(Triple("rus_syn", "ROM", 8), savedPosition)
    }

    @Test
    fun saveBibleTypographyPreferencesUseCase_delegatesToRepository() = runTest {
        val useCase = SaveBibleTypographyPreferencesUseCase(fakeRepository)
        useCase(20f, false, 1.8f)
        assertEquals(Triple(20f, false, 1.8f), savedTypography)
    }

    @Test
    fun saveBibleNarratorUseCase_delegatesToRepository() = runTest {
        val useCase = SaveBibleNarratorUseCase(fakeRepository)
        useCase("hays")
        assertEquals("hays", savedNarrator)
    }

    @Test
    fun defaultBibleReadingPreferences_startsAtGenesis() {
        val defaultPrefs = BibleReadingPreferences()
        assertEquals("GEN", defaultPrefs.bookId)
        assertEquals(1, defaultPrefs.chapter)
        assertEquals(1, defaultPrefs.verse)
    }
}
