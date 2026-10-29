package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.CatalogSyncState
import com.example.sermonindex.domain.model.DownloadedSermon
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.SearchCategory
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.testing.FakeSermonRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UseCasesTest {

    private val fakeSermon = Sermon(
        id = "1",
        title = "Grace and Truth",
        speaker = "A.W. Tozer",
        mp3Url = "https://example.com/audio.mp3"
    )

    private val fakeDownloaded = DownloadedSermon(
        id = "1",
        title = "Grace and Truth",
        speaker = "A.W. Tozer",
        localPath = "/fake/path/1.mp3",
        fileSizeBytes = 15_000_000L,
        downloadedAt = 2000L
    )

    private lateinit var fakeRepository: FakeSermonRepository

    @Before
    fun setUp() {
        fakeRepository = FakeSermonRepository().apply {
            favoritesFlow.value = listOf(
                FavoriteSermon(
                    id = "1",
                    title = "Grace and Truth",
                    speaker = "A.W. Tozer",
                    speakerImage = "",
                    duration = "30:00",
                    addedAt = 1000L
                )
            )
            downloadsFlow.value = listOf(fakeDownloaded)
            downloadProgressMap["1"] = 50
            popularSermonsResult = Result.success(listOf(fakeSermon))
            randomSermonsResult = Result.success(listOf(fakeSermon))
            bibleVerseTextResult = Result.success("For God so loved the world")
            recentSearchQueriesFlow.value = listOf("tozer", "prayer")
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
        assertEquals(fakeSermon, fakeRepository.lastToggledFavorite)
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
        assertEquals(fakeSermon, fakeRepository.lastDownloadedSermon)
    }

    @Test
    fun deleteDownloadUseCase_delegatesToRepository() = runTest {
        val useCase = DeleteDownloadUseCase(fakeRepository)
        useCase("1")
        assertEquals("1", fakeRepository.lastDeletedDownloadId)
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
        assertFalse(audioSermon.isTextOnly)
        assertFalse(cdnOnlySermon.isTextOnly)
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
        assertFalse(videoSermon.isTextOnly)
    }

    @Test
    fun sermonDetail_isTextOnly_returnsTrueOnlyWhenAudioUnavailable() {
        val textDetail = SermonDetail(id = "1", title = "Text Sermon", speaker = "Spurgeon", hasAudio = false, mp3Url = "", cdnMp3Url = "")
        val audioDetail = SermonDetail(id = "2", title = "Audio Sermon", speaker = "Tozer", hasAudio = true, mp3Url = "https://example.com/audio.mp3")

        assertTrue(textDetail.isTextOnly)
        assertFalse(audioDetail.isTextOnly)
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
        assertFalse(videoDetail.isTextOnly)
    }

    @Test
    fun downloadedSermon_toSermon_mapsMp4ToVideoOfflinePlayback() {
        val videoDownload = DownloadedSermon(
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
        val audioDownload = DownloadedSermon(
            id = "aud_123",
            title = "Audio Sermon",
            speaker = "A.W. Tozer",
            localPath = "/data/user/0/com.example.sermonindex/files/audio_downloads/aud_123.mp3",
            fileSizeBytes = 15_000_000L,
            downloadedAt = 1000L
        )
        val sermon = audioDownload.toSermon()
        assertFalse(sermon.hasVideo)
        assertEquals("", sermon.mp4Url)
        assertEquals("file:///data/user/0/com.example.sermonindex/files/audio_downloads/aud_123.mp3", sermon.mp3Url)
    }

    @Test
    fun searchUseCase_returnsMatchingResults() = runTest {
        val useCase = SearchUseCase(fakeRepository)
        val result = useCase("tozer", SearchCategory.ALL).first()
        assertEquals("tozer", result.query)
    }

    @Test
    fun getCatalogSyncStateUseCase_returnsState() = runTest {
        val useCase = GetCatalogSyncStateUseCase(fakeRepository)
        val state = useCase().first()
        assertEquals(CatalogSyncState.NotDownloaded, state)
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
        fakeRepository.checkForCatalogUpdateResult = true
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
        assertEquals(listOf("holiness"), fakeRepository.addedSearchQueries)

        deleteQuery("prayer")
        assertEquals(listOf("prayer"), fakeRepository.deletedSearchQueries)

        clearQueries()
        assertTrue(fakeRepository.clearRecentQueriesCalled)
    }

    @Test
    fun clearHistoryUseCase_delegatesToRepository() = runTest {
        val useCase = ClearHistoryUseCase(fakeRepository)
        useCase()
        assertTrue(fakeRepository.clearHistoryCalled)
    }
}
