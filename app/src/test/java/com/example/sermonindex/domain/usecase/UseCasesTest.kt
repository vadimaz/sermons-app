package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.BibleBook
import com.example.sermonindex.domain.model.ChapterDetails
import com.example.sermonindex.domain.model.FavoriteSermon
import com.example.sermonindex.domain.model.ListeningHistory
import com.example.sermonindex.domain.model.OfflineSearchResult
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
        override val ftsIndexedCount: Flow<Int> = flowOf(0)

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
        override suspend fun getLocalAudioPath(sermonId: String): String? = if (sermonId == "1") "/fake/path/1.mp3" else null

        override suspend fun getScriptureBooks(): Result<List<BibleBook>> = Result.success(emptyList())
        override suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails> = Result.failure(NotImplementedError())
        override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>> = Result.failure(NotImplementedError())
        override suspend fun getSpeakers(): Result<List<Speaker>> = Result.success(emptyList())
        override suspend fun getSpeakerDetails(slug: String): Result<Speaker> = Result.failure(NotImplementedError())
        override suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons> = Result.failure(NotImplementedError())
        override suspend fun getTopics(): Result<List<Topic>> = Result.success(emptyList())
        override suspend fun getTopicDetails(slug: String): Result<TopicDetail> = Result.failure(NotImplementedError())
        override suspend fun searchOffline(query: String): List<OfflineSearchResult> = emptyList()
        override suspend fun clearOfflineIndex() = Unit
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
    fun getDownloadProgressUseCase_emitsProgress() = runTest {
        val useCase = GetDownloadProgressUseCase(fakeRepository)
        assertEquals(50, useCase("1").first())
    }
}
