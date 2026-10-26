package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.BibleTranslation
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.model.Speaker
import com.example.sermonindex.domain.model.SpeakerPagedSermons
import com.example.sermonindex.domain.model.Topic
import com.example.sermonindex.domain.model.TopicDetail
import com.example.sermonindex.domain.repository.SermonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class  UseCasesTest {

    private val fakeSermon = Sermon(
        id = "1",
        title = "Grace and Truth",
        speaker = "A.W. Tozer",
        mp3Url = "https://example.com/audio.mp3"
    )

    private val fakeRepository = object : SermonRepository {
        var toggleCalledWith: Sermon? = null

        override val favorites: Flow<List<FavoriteSermon>> = flowOf(
            listOf(
                FavoriteSermon(
                    id = "1",
                    title = "Grace and Truth",
                    speaker = "A.W. Tozer",
                    speakerImage = "",
                    duration = "30:00",
                    addedAt = 1000L
                )
            )
        )
        override val history: Flow<List<ListeningHistory>> = emptyFlow()

        override suspend fun getPopularSermons(): Result<List<Sermon>> =
            Result.success(listOf(fakeSermon))

        override suspend fun getSermonDetails(id: String): Result<SermonDetail> =
            Result.failure(NotImplementedError())

        override suspend fun toggleFavorite(sermon: Sermon) {
            toggleCalledWith = sermon
        }

        override fun isFavorite(id: String): Flow<Boolean> = flowOf(id == "1")

        override suspend fun updateHistory(
            sermonId: String,
            title: String,
            speaker: String,
            speakerImage: String,
            positionMs: Long,
            durationMs: Long
        ) = Unit

        override suspend fun clearHistory() = Unit
        val fakeDownloaded = com.example.sermonindex.domain.model.DownloadedSermon(
            id = "1",
            title = "Grace and Truth",
            speaker = "A.W. Tozer",
            localPath = "/fake/path/1.mp3",
            fileSizeBytes = 15_000_000L,
            downloadedAt = 2000L
        )

        var downloadedSermonPassed: Sermon? = null
        var deletedDownloadId: String? = null
        var deleteAllDownloadsCalled: Boolean = false

        override val downloads: Flow<List<com.example.sermonindex.domain.model.DownloadedSermon>> = flowOf(listOf(fakeDownloaded))
        override fun isDownloaded(sermonId: String): Flow<Boolean> = flowOf(sermonId == "1")
        override fun getDownloadProgress(sermonId: String): Flow<Int?> = flowOf(50)
        override suspend fun downloadSermon(sermon: Sermon) {
            downloadedSermonPassed = sermon
        }
        override suspend fun cancelDownload(sermonId: String) = Unit
        override suspend fun deleteDownload(sermonId: String) {
            deletedDownloadId = sermonId
        }
        override suspend fun deleteAllDownloads() {
            deleteAllDownloadsCalled = true
        }
        override suspend fun getLocalAudioPath(sermonId: String): String? = if (sermonId == "1") "/fake/path/1.mp3" else null

        override suspend fun getScriptureBooks(): Result<List<BibleBook>> = Result.success(emptyList())
        override suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails> = Result.failure(NotImplementedError())
        override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>> = Result.failure(NotImplementedError())
        override suspend fun getAvailableBibleTranslations(): Result<List<BibleTranslation>> = Result.success(emptyList())
        override suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int): Result<String> = Result.success("For God so loved the world")
        override suspend fun getSpeakers(): Result<List<Speaker>> = Result.success(emptyList())
        override suspend fun getSpeakerDetails(slug: String): Result<Speaker> = Result.failure(NotImplementedError())
        override suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons> = Result.failure(NotImplementedError())
        override suspend fun getTopics(): Result<List<Topic>> = Result.success(emptyList())
        override suspend fun getTopicDetails(slug: String): Result<TopicDetail> = Result.failure(NotImplementedError())
        override suspend fun getTranscriptCues(vttUrl: String): Result<List<com.example.sermonindex.domain.model.TranscriptCue>> = Result.success(emptyList())

        override val bibleReadingPreferences: Flow<com.example.sermonindex.domain.model.BibleReadingPreferences> =
            flowOf(com.example.sermonindex.domain.model.BibleReadingPreferences())
        override suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int): Result<com.example.sermonindex.domain.model.BibleChapter> =
            Result.failure(NotImplementedError())
        override suspend fun getBibleBooksForTranslation(translationId: String): Result<List<BibleBook>> =
            Result.success(emptyList())
        override suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int) = Unit
        override suspend fun saveBibleTypographyPreferences(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float) = Unit
        override suspend fun saveBibleNarrator(narrator: String?) = Unit

        override fun search(
            query: String,
            category: com.example.sermonindex.domain.model.SearchCategory
        ): Flow<com.example.sermonindex.domain.model.SearchAggregateResult> =
            flowOf(com.example.sermonindex.domain.model.SearchAggregateResult(query = query))

        override val catalogSyncState: Flow<com.example.sermonindex.domain.model.CatalogSyncState> =
            flowOf(com.example.sermonindex.domain.model.CatalogSyncState.NotDownloaded)

        var downloadCatalogCalled = false
        override suspend fun downloadOrUpdateCatalog() {
            downloadCatalogCalled = true
        }

        var updateAvailable = false
        override suspend fun checkForCatalogUpdate(): Boolean = updateAvailable

        var recentQueriesList = listOf("tozer", "prayer")
        override val recentSearchQueries: Flow<List<String>>
            get() = flowOf(recentQueriesList)

        var addedRecentQuery: String? = null
        override suspend fun addRecentSearchQuery(query: String) {
            addedRecentQuery = query
        }

        var deletedRecentQuery: String? = null
        override suspend fun deleteRecentSearchQuery(query: String) {
            deletedRecentQuery = query
        }

        var clearedRecentQueries = false
        override suspend fun clearRecentSearchQueries() {
            clearedRecentQueries = true
        }

        override val isCatalogReadyBannerDismissed: Flow<Boolean> = flowOf(false)
        override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {}

        var requestedRandomShard: Int? = null
        var randomSermonsResult: Result<List<Sermon>> = Result.success(listOf(fakeSermon))
        override suspend fun getRandomSermons(shard: Int?): Result<List<Sermon>> {
            requestedRandomShard = shard
            return randomSermonsResult
        }
    }

    @Test
    fun getRandomSermonsUseCase_delegatesToRepositoryWithShard() = runTest {
        val useCase = GetRandomSermonsUseCase(fakeRepository)
        val result = useCase(42)
        assertTrue(result.isSuccess)
        assertEquals(42, fakeRepository.requestedRandomShard)
        assertEquals(listOf(fakeSermon), result.getOrNull())
    }

    @Test
    fun getPopularSermonsUseCase_returnsSuccess() = runTest {
        val useCase = GetPopularSermonsUseCase(fakeRepository)
        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(listOf(fakeSermon), result.getOrNull())
    }

    @Test
    fun toggleFavoriteUseCase_delegatesToRepository() = runTest {
        val useCase = ToggleFavoriteUseCase(fakeRepository)
        useCase(fakeSermon)
        assertEquals(fakeSermon, fakeRepository.toggleCalledWith)
    }

    @Test
    fun getFavoritesUseCase_returnsFlowFromRepository() = runTest {
        val useCase = GetFavoritesUseCase(fakeRepository)
        val favorites = useCase().first()
        assertEquals(1, favorites.size)
        assertEquals("Grace and Truth", favorites.first().title)
    }

    @Test
    fun isFavoriteUseCase_emitsCorrectStatus() = runTest {
        val useCase = IsFavoriteUseCase(fakeRepository)
        assertTrue(useCase("1").first())
    }

    @Test
    fun getDownloadsUseCase_returnsDownloadsFlow() = runTest {
        val useCase = GetDownloadsUseCase(fakeRepository)
        val list = useCase().first()
        assertEquals(1, list.size)
        assertEquals("1", list.first().id)
        assertEquals("/fake/path/1.mp3", list.first().localPath)
    }

    @Test
    fun isDownloadedUseCase_returnsCorrectStatus() = runTest {
        val useCase = IsDownloadedUseCase(fakeRepository)
        assertTrue(useCase("1").first())
    }

    @Test
    fun downloadSermonUseCase_delegatesToRepository() = runTest {
        val useCase = DownloadSermonUseCase(fakeRepository)
        useCase(fakeSermon)
        assertEquals(fakeSermon, fakeRepository.downloadedSermonPassed)
    }

    @Test
    fun deleteDownloadUseCase_delegatesToRepository() = runTest {
        val useCase = DeleteDownloadUseCase(fakeRepository)
        useCase("1")
        assertEquals("1", fakeRepository.deletedDownloadId)
    }

    @Test
    fun deleteAllDownloadsUseCase_delegatesToRepository() = runTest {
        val useCase = DeleteAllDownloadsUseCase(fakeRepository)
        useCase()
        assertTrue(fakeRepository.deleteAllDownloadsCalled)
    }

    @Test
    fun getDownloadProgressUseCase_emitsProgress() = runTest {
        val useCase = GetDownloadProgressUseCase(fakeRepository)
        assertEquals(50, useCase("1").first())
    }

    @Test
    fun getBibleTranslationsUseCase_returnsSuccess() = runTest {
        val useCase = GetBibleTranslationsUseCase(fakeRepository)
        val result = useCase()
        assertTrue(result.isSuccess)
    }

    @Test
    fun getBibleVerseTextUseCase_returnsVerseText() = runTest {
        val useCase = GetBibleVerseTextUseCase(fakeRepository)
        val result = useCase("rus_syn", "JHN", 3, 16)
        assertTrue(result.isSuccess)
        assertEquals("For God so loved the world", result.getOrNull())
    }

    @Test
    fun sermon_isTextOnly_returnsTrueOnlyWhenAudioUnavailable() {
        val textSermon = Sermon(id = "1", title = "Text Sermon", speaker = "Spurgeon", hasAudio = false, mp3Url = "", cdnMp3Url = "")
        val audioSermon = Sermon(id = "2", title = "Audio Sermon", speaker = "Tozer", hasAudio = true, mp3Url = "https://example.com/audio.mp3")
        val cdnOnlySermon = Sermon(id = "3", title = "CDN Sermon", speaker = "Ravenhill", hasAudio = false, cdnMp3Url = "https://example.com/cdn.mp3")

        assertTrue(textSermon.isTextOnly)
        org.junit.Assert.assertFalse(audioSermon.isTextOnly)
        org.junit.Assert.assertFalse(cdnOnlySermon.isTextOnly)
    }

    @Test
    fun sermon_videoSermon_hasVideoIsTrueAndIsTextOnlyIsFalse() {
        val videoSermon = Sermon(
            id = "4",
            title = "Video Sermon",
            speaker = "Zac Poonen",
            hasAudio = false,
            mp3Url = "",
            cdnMp3Url = "",
            mp4Url = "https://example.com/video.mp4"
        )
        assertTrue(videoSermon.hasVideo)
        org.junit.Assert.assertFalse(videoSermon.isTextOnly)
    }

    @Test
    fun sermonDetail_isTextOnly_returnsTrueOnlyWhenAudioUnavailable() {
        val textDetail = SermonDetail(id = "1", title = "Text Sermon", speaker = "Spurgeon", hasAudio = false, mp3Url = "", cdnMp3Url = "")
        val audioDetail = SermonDetail(id = "2", title = "Audio Sermon", speaker = "Tozer", hasAudio = true, mp3Url = "https://example.com/audio.mp3")

        assertTrue(textDetail.isTextOnly)
        org.junit.Assert.assertFalse(audioDetail.isTextOnly)
    }

    @Test
    fun sermonDetail_videoSermon_hasVideoIsTrueAndIsTextOnlyIsFalse() {
        val videoDetail = SermonDetail(
            id = "3",
            title = "Video Sermon Detail",
            speaker = "Zac Poonen",
            hasAudio = false,
            mp3Url = "",
            cdnMp3Url = "",
            videoUrl = "https://youtube.com/watch?v=123"
        )
        assertTrue(videoDetail.hasVideo)
        org.junit.Assert.assertFalse(videoDetail.isTextOnly)
    }

    @Test
    fun downloadedSermon_toSermon_mapsMp4ToVideoOfflinePlayback() {
        val videoDownload = com.example.sermonindex.domain.model.DownloadedSermon(
            id = "vid_123",
            title = "Video Sermon",
            speaker = "Art Katz",
            localPath = "/data/user/0/com.example.sermonindex/files/audio_downloads/vid_123.mp4",
            fileSizeBytes = 50_000_000L,
            downloadedAt = 1000L
        )
        val sermon = videoDownload.toSermon()
        assertTrue(sermon.hasVideo)
        assertEquals("file:///data/user/0/com.example.sermonindex/files/audio_downloads/vid_123.mp4", sermon.mp4Url)
        assertEquals("", sermon.mp3Url)
    }

    @Test
    fun downloadedSermon_toSermon_mapsMp3ToAudioOfflinePlayback() {
        val audioDownload = com.example.sermonindex.domain.model.DownloadedSermon(
            id = "aud_123",
            title = "Audio Sermon",
            speaker = "A.W. Tozer",
            localPath = "/data/user/0/com.example.sermonindex/files/audio_downloads/aud_123.mp3",
            fileSizeBytes = 15_000_000L,
            downloadedAt = 1000L
        )
        val sermon = audioDownload.toSermon()
        org.junit.Assert.assertFalse(sermon.hasVideo)
        assertEquals("", sermon.mp4Url)
        assertEquals("file:///data/user/0/com.example.sermonindex/files/audio_downloads/aud_123.mp3", sermon.mp3Url)
    }

    @Test
    fun searchUseCase_returnsMatchingResults() = runTest {
        val useCase = SearchUseCase(fakeRepository)
        val result = useCase("tozer", com.example.sermonindex.domain.model.SearchCategory.ALL).first()
        assertEquals("tozer", result.query)
    }

    @Test
    fun getCatalogSyncStateUseCase_returnsState() = runTest {
        val useCase = GetCatalogSyncStateUseCase(fakeRepository)
        val state = useCase().first()
        assertEquals(com.example.sermonindex.domain.model.CatalogSyncState.NotDownloaded, state)
    }

    @Test
    fun downloadOrUpdateCatalogUseCase_triggersRepository() = runTest {
        val useCase = DownloadOrUpdateCatalogUseCase(fakeRepository)
        useCase()
        assertTrue(fakeRepository.downloadCatalogCalled)
    }

    @Test
    fun checkCatalogUpdateUseCase_returnsUpdateStatus() = runTest {
        val useCase = CheckCatalogUpdateUseCase(fakeRepository)
        fakeRepository.updateAvailable = true
        assertTrue(useCase())
    }

    @Test
    fun manageRecentQueriesUseCase_handlesAddDeleteClear() = runTest {
        val getQueries = GetRecentSearchQueriesUseCase(fakeRepository)
        val addQuery = AddRecentSearchQueryUseCase(fakeRepository)
        val deleteQuery = DeleteRecentSearchQueryUseCase(fakeRepository)
        val clearQueries = ClearRecentSearchQueriesUseCase(fakeRepository)

        assertEquals(listOf("tozer", "prayer"), getQueries().first())

        addQuery("holiness")
        assertEquals("holiness", fakeRepository.addedRecentQuery)

        deleteQuery("prayer")
        assertEquals("prayer", fakeRepository.deletedRecentQuery)

        clearQueries()
        assertTrue(fakeRepository.clearedRecentQueries)
    }
}
