package com.example.sermonindex.ui.sermon

import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SermonDetail
import com.example.sermonindex.domain.repository.SermonRepository
import com.example.sermonindex.domain.usecase.DeleteDownloadUseCase
import com.example.sermonindex.domain.usecase.DownloadSermonUseCase
import com.example.sermonindex.domain.usecase.GetDownloadProgressUseCase
import com.example.sermonindex.domain.usecase.GetSermonDetailsUseCase
import com.example.sermonindex.domain.usecase.IsDownloadedUseCase
import com.example.sermonindex.domain.usecase.IsFavoriteUseCase
import com.example.sermonindex.domain.usecase.ToggleFavoriteUseCase
import com.example.sermonindex.playback.PlaybackManager
import com.example.sermonindex.playback.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SermonDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val playbackStateFlow = MutableStateFlow(PlaybackState())

    private var togglePlayPauseCalled = false
    private var playedSermon: Sermon? = null

    private val fakePlaybackManager = object : PlaybackManager(null) {
        override val playbackState = playbackStateFlow.asStateFlow()

        override fun playSermon(sermon: Sermon, startPositionMs: Long) {
            playedSermon = sermon
        }

        override fun togglePlayPause() {
            togglePlayPauseCalled = true
        }
    }

    private val testSermonDetail = SermonDetail(
        id = "sermon-1",
        title = "Faith and Prayer",
        speaker = "A.W. Tozer",
        mp3Url = "https://example.com/sermon1.mp3"
    )

    private val fakeRepository = object : SermonRepository {
        override val favorites = emptyFlow<List<com.example.sermonindex.domain.model.FavoriteSermon>>()
        override val history = emptyFlow<List<com.example.sermonindex.domain.model.ListeningHistory>>()
        override val downloads = emptyFlow<List<com.example.sermonindex.domain.model.DownloadedSermon>>()
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
        override suspend fun getPopularSermons() = Result.success(emptyList<Sermon>())
        override suspend fun getScriptureBooks() = Result.success(emptyList<com.example.sermonindex.domain.model.BibleBook>())
        override suspend fun getScriptureChapter(bookId: String, chapter: Int) = Result.failure<com.example.sermonindex.domain.model.ChapterDetails>(Exception("Not implemented"))
        override suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int) = Result.success(emptyList<Sermon>())
        override suspend fun getAvailableBibleTranslations() = Result.success(emptyList<com.example.sermonindex.domain.model.BibleTranslation>())
        override suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int) = Result.success("")
        override suspend fun getSpeakers() = Result.success(emptyList<com.example.sermonindex.domain.model.Speaker>())
        override suspend fun getSpeakerDetails(slug: String) = Result.failure<com.example.sermonindex.domain.model.Speaker>(Exception("Not implemented"))
        override suspend fun getSpeakerSermonsPage(slug: String, page: Int) = Result.failure<com.example.sermonindex.domain.model.SpeakerPagedSermons>(Exception("Not implemented"))
        override suspend fun getTopics() = Result.success(emptyList<com.example.sermonindex.domain.model.Topic>())
        override suspend fun getTopicDetails(slug: String) = Result.failure<com.example.sermonindex.domain.model.TopicDetail>(Exception("Not implemented"))
        override suspend fun getSermonDetails(id: String) = Result.success(testSermonDetail)
        override suspend fun getTranscriptCues(vttUrl: String) = Result.success(emptyList<com.example.sermonindex.domain.model.TranscriptCue>())
        override val bibleReadingPreferences = emptyFlow<com.example.sermonindex.domain.model.BibleReadingPreferences>()
        override suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int) = Result.failure<com.example.sermonindex.domain.model.BibleChapter>(Exception("Not implemented"))
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
        override val isCatalogReadyBannerDismissed: Flow<Boolean> = flowOf(false)
        override suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean) {}
    }

    private lateinit var viewModel: SermonDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        togglePlayPauseCalled = false
        playedSermon = null
        playbackStateFlow.value = PlaybackState()

        viewModel = SermonDetailViewModel(
            sermonId = "sermon-1",
            getSermonDetailsUseCase = GetSermonDetailsUseCase(fakeRepository),
            isFavoriteUseCase = IsFavoriteUseCase(fakeRepository),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(fakeRepository),
            isDownloadedUseCase = IsDownloadedUseCase(fakeRepository),
            getDownloadProgressUseCase = GetDownloadProgressUseCase(fakeRepository),
            downloadSermonUseCase = DownloadSermonUseCase(fakeRepository),
            deleteDownloadUseCase = DeleteDownloadUseCase(fakeRepository),
            playbackManager = fakePlaybackManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDetailAndIsNotActiveInPlayer() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("sermon-1", state.detail?.id)
        assertFalse(state.isActiveInPlayer)
        assertFalse(state.isPlaying)
    }

    @Test
    fun whenMatchingSermonIsPlaying_isActiveInPlayerAndIsPlayingAreTrue() = runTest(testDispatcher) {
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(
            currentSermon = testSermonDetail.toSermon(),
            isPlaying = true
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isActiveInPlayer)
        assertTrue(state.isPlaying)
    }

    @Test
    fun whenMatchingSermonIsPaused_isActiveInPlayerIsTrueAndIsPlayingIsFalse() = runTest(testDispatcher) {
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(
            currentSermon = testSermonDetail.toSermon(),
            isPlaying = false
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isActiveInPlayer)
        assertFalse(state.isPlaying)
    }

    @Test
    fun whenDifferentSermonIsPlaying_isActiveInPlayerAndIsPlayingAreFalse() = runTest(testDispatcher) {
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(
            currentSermon = Sermon(id = "sermon-99", title = "Other", speaker = "Other"),
            isPlaying = true
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isActiveInPlayer)
        assertFalse(state.isPlaying)
    }

    @Test
    fun onEventPlay_whenNotActiveInPlayer_callsPlaySermon() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(SermonDetailUiEvent.Play)
        advanceUntilIdle()

        assertEquals("sermon-1", playedSermon?.id)
        assertFalse(togglePlayPauseCalled)
    }

    @Test
    fun onEventPlay_whenActiveInPlayer_callsTogglePlayPause() = runTest(testDispatcher) {
        advanceUntilIdle()

        playbackStateFlow.value = PlaybackState(
            currentSermon = testSermonDetail.toSermon(),
            isPlaying = false
        )
        advanceUntilIdle()

        viewModel.onEvent(SermonDetailUiEvent.Play)
        advanceUntilIdle()

        assertTrue(togglePlayPauseCalled)
    }
}
