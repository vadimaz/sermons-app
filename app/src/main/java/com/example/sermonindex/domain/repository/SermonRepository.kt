package com.example.sermonindex.domain.repository

import com.example.sermonindex.domain.model.BibleBookmark
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Pure domain repository interface.
 * Decoupled from concrete network clients, Room databases, and data transfer objects.
 */
interface SermonRepository {
    val favorites: Flow<List<FavoriteSermon>>
    val history: Flow<List<ListeningHistory>>
    val downloads: Flow<List<DownloadedSermon>>

    fun isFavorite(id: String): Flow<Boolean>
    suspend fun toggleFavorite(sermon: Sermon)

    fun isDownloaded(sermonId: String): Flow<Boolean>
    fun getDownloadProgress(sermonId: String): Flow<Int?>
    suspend fun downloadSermon(sermon: Sermon)
    suspend fun cancelDownload(sermonId: String)
    suspend fun deleteDownload(sermonId: String)
    suspend fun deleteAllDownloads()
    suspend fun getLocalAudioPath(sermonId: String): String?

    suspend fun updateHistory(
        sermonId: String,
        title: String,
        speaker: String,
        speakerImage: String,
        positionMs: Long,
        durationMs: Long
    )

    suspend fun getPopularSermons(): Result<List<Sermon>>
    suspend fun getRandomSermons(shard: Int? = null): Result<List<Sermon>> = Result.success(emptyList())
    suspend fun getScriptureBooks(): Result<List<BibleBook>>
    suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ChapterDetails>
    suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>>
    suspend fun getAvailableBibleTranslations(): Result<List<BibleTranslation>>
    suspend fun getBibleVerseText(translationId: String, bookId: String, chapter: Int, verse: Int): Result<String>
    suspend fun getSpeakers(): Result<List<Speaker>>
    suspend fun getSpeakerDetails(slug: String): Result<Speaker>
    suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedSermons>
    suspend fun getTopics(): Result<List<Topic>>
    suspend fun getTopicDetails(slug: String): Result<TopicDetail>
    suspend fun getSermonDetails(id: String): Result<SermonDetail>
    suspend fun getTranscriptCues(vttUrl: String): Result<List<com.example.sermonindex.domain.model.TranscriptCue>>

    val bibleReadingPreferences: Flow<com.example.sermonindex.domain.model.BibleReadingPreferences>
    suspend fun getBibleChapter(translationId: String, bookId: String, chapter: Int): Result<com.example.sermonindex.domain.model.BibleChapter>
    suspend fun getBibleBooksForTranslation(translationId: String): Result<List<BibleBook>>
    suspend fun saveBibleReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int = 1)
    suspend fun saveBibleTypographyPreferences(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float)
    suspend fun saveBibleNarrator(narrator: String?)

    val bibleBookmarks: Flow<List<BibleBookmark>> get() = emptyFlow()
    fun getBibleBookmarksForChapter(translationId: String, bookId: String, chapter: Int): Flow<List<BibleBookmark>> = emptyFlow()
    suspend fun saveBibleBookmark(bookmark: BibleBookmark) {}
    suspend fun deleteBibleBookmark(id: String) {}
    suspend fun deleteBibleBookmarkForVerse(translationId: String, bookId: String, chapter: Int, verse: Int) {}

    suspend fun clearHistory()

    fun search(query: String, category: com.example.sermonindex.domain.model.SearchCategory): Flow<com.example.sermonindex.domain.model.SearchAggregateResult>
    val catalogSyncState: Flow<com.example.sermonindex.domain.model.CatalogSyncState>
    suspend fun downloadOrUpdateCatalog()
    suspend fun checkForCatalogUpdate(): Boolean
    val recentSearchQueries: Flow<List<String>>
    suspend fun addRecentSearchQuery(query: String)
    suspend fun deleteRecentSearchQuery(query: String)
    suspend fun clearRecentSearchQueries()
    val isCatalogReadyBannerDismissed: Flow<Boolean>
    suspend fun setCatalogReadyBannerDismissed(dismissed: Boolean)
}
