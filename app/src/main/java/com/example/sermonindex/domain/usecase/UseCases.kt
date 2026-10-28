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
import javax.inject.Inject

class GetPopularSermonsUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(): Result<List<Sermon>> = repository.getPopularSermons()
}

class GetSermonDetailsUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(id: String): Result<SermonDetail> = repository.getSermonDetails(id)
}

class ToggleFavoriteUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(sermon: Sermon) = repository.toggleFavorite(sermon)
}

class GetFavoritesUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(): Flow<List<FavoriteSermon>> = repository.favorites
}

class IsFavoriteUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(id: String): Flow<Boolean> = repository.isFavorite(id)
}

class GetHistoryUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(): Flow<List<ListeningHistory>> = repository.history
}

class ClearHistoryUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke() = repository.clearHistory()
}

class GetScriptureBooksUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(): Result<List<BibleBook>> = repository.getScriptureBooks()
}

class GetScriptureChapterUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(bookId: String, chapter: Int): Result<ChapterDetails> =
        repository.getScriptureChapter(bookId, chapter)
}

class GetScriptureVerseUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(bookId: String, chapter: Int, verse: Int): Result<List<Sermon>> =
        repository.getScriptureVerse(bookId, chapter, verse)
}

class GetBibleTranslationsUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(): Result<List<BibleTranslation>> = repository.getAvailableBibleTranslations()
}

class GetBibleVerseTextUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int
    ): Result<String> = repository.getBibleVerseText(translationId, bookId, chapter, verse)
}

class GetSpeakersUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(): Result<List<Speaker>> = repository.getSpeakers()
}

class GetSpeakerDetailsUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(slug: String): Result<Speaker> = repository.getSpeakerDetails(slug)
}

class GetSpeakerSermonsPageUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(slug: String, page: Int): Result<SpeakerPagedSermons> =
        repository.getSpeakerSermonsPage(slug, page)
}

class GetTopicsUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(): Result<List<Topic>> = repository.getTopics()
}

class GetTopicDetailsUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(slug: String): Result<TopicDetail> = repository.getTopicDetails(slug)
}

class GetTranscriptCuesUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(vttUrl: String): Result<List<com.example.sermonindex.domain.model.TranscriptCue>> =
        repository.getTranscriptCues(vttUrl)
}

class GetDownloadsUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(): Flow<List<com.example.sermonindex.domain.model.DownloadedSermon>> = repository.downloads
}

class IsDownloadedUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(sermonId: String): Flow<Boolean> = repository.isDownloaded(sermonId)
}

class GetDownloadProgressUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(sermonId: String): Flow<Int?> = repository.getDownloadProgress(sermonId)
}

class DownloadSermonUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(sermon: Sermon) = repository.downloadSermon(sermon)
}

class CancelDownloadUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(sermonId: String) = repository.cancelDownload(sermonId)
}

class DeleteDownloadUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(sermonId: String) = repository.deleteDownload(sermonId)
}

class DeleteAllDownloadsUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke() = repository.deleteAllDownloads()
}

class GetBibleChapterUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(translationId: String, bookId: String, chapter: Int): Result<com.example.sermonindex.domain.model.BibleChapter> =
        repository.getBibleChapter(translationId, bookId, chapter)
}

class GetBibleBooksForTranslationUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(translationId: String): Result<List<BibleBook>> =
        repository.getBibleBooksForTranslation(translationId)
}

class GetBibleReadingPreferencesUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(): Flow<com.example.sermonindex.domain.model.BibleReadingPreferences> =
        repository.bibleReadingPreferences
}

class SaveBibleReadingPositionUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(translationId: String, bookId: String, chapter: Int, verse: Int = 1) =
        repository.saveBibleReadingPosition(translationId, bookId, chapter, verse)
}

class SaveBibleTypographyPreferencesUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float) =
        repository.saveBibleTypographyPreferences(fontSizeSp, isSerif, lineSpacingMultiplier)
}

class SaveBibleNarratorUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(narrator: String?) =
        repository.saveBibleNarrator(narrator)
}

class SearchUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(
        query: String,
        category: com.example.sermonindex.domain.model.SearchCategory
    ): Flow<com.example.sermonindex.domain.model.SearchAggregateResult> =
        repository.search(query, category)
}

class GetCatalogSyncStateUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(): Flow<com.example.sermonindex.domain.model.CatalogSyncState> =
        repository.catalogSyncState
}

class DownloadOrUpdateCatalogUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke() = repository.downloadOrUpdateCatalog()
}

class CheckCatalogUpdateUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(): Boolean = repository.checkForCatalogUpdate()
}

class GetRecentSearchQueriesUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(): Flow<List<String>> = repository.recentSearchQueries
}

class AddRecentSearchQueryUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(query: String) = repository.addRecentSearchQuery(query)
}

class DeleteRecentSearchQueryUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(query: String) = repository.deleteRecentSearchQuery(query)
}

class ClearRecentSearchQueriesUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke() = repository.clearRecentSearchQueries()
}

class GetCatalogReadyBannerDismissedUseCase @Inject constructor(private val repository: SermonRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isCatalogReadyBannerDismissed
}

class SetCatalogReadyBannerDismissedUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(dismissed: Boolean) = repository.setCatalogReadyBannerDismissed(dismissed)
}


