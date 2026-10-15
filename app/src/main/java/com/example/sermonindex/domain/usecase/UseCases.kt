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

class OfflineSearchUseCase @Inject constructor(private val repository: SermonRepository) {
    suspend operator fun invoke(query: String): List<OfflineSearchResult> =
        repository.searchOffline(query)
}
