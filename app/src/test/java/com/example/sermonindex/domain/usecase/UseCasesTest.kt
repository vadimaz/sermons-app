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

class UseCasesTest {

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
}
