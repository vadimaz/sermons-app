package com.example.sermonindex.data.repository

import com.example.sermonindex.data.local.AppDatabase
import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.data.model.ScriptureBookDto
import com.example.sermonindex.data.model.ScriptureChapterDto
import com.example.sermonindex.data.model.SermonDetailDto
import com.example.sermonindex.data.model.SermonDto
import com.example.sermonindex.data.model.SpeakerDto
import com.example.sermonindex.data.model.SpeakerPagedDto
import com.example.sermonindex.data.model.TopicDetailDto
import com.example.sermonindex.data.model.TopicDto
import com.example.sermonindex.data.network.SermonIndexApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SermonRepository(
    private val apiService: SermonIndexApiService,
    private val database: AppDatabase
) {

    val favorites: Flow<List<FavoriteSermonEntity>> = database.favoritesDao.getAllFavorites()
    val history: Flow<List<HistoryEntity>> = database.historyDao.getRecentHistory()

    fun isFavorite(id: String): Flow<Boolean> = database.favoritesDao.isFavorite(id)

    suspend fun toggleFavorite(sermon: SermonDto) {
        val currentlyFav = database.favoritesDao.isFavorite(sermon.id).first()
        if (currentlyFav) {
            database.favoritesDao.deleteFavorite(sermon.id)
        } else {
            database.favoritesDao.insertFavorite(
                FavoriteSermonEntity(
                    id = sermon.id,
                    title = sermon.title,
                    speaker = sermon.speaker,
                    speakerImage = sermon.speakerImage,
                    duration = sermon.duration,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updateHistory(
        sermonId: String,
        title: String,
        speaker: String,
        speakerImage: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val completed = durationMs > 0 && positionMs >= (durationMs - 5000L)
        database.historyDao.upsertHistory(
            HistoryEntity(
                sermonId = sermonId,
                title = title,
                speaker = speaker,
                speakerImage = speakerImage,
                lastPositionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis(),
                completed = completed
            )
        )
    }

    suspend fun getPopularSermons(): Result<List<SermonDto>> = runCatching {
        apiService.getPopularSermons().sermons
    }

    suspend fun getScriptureBooks(): Result<List<ScriptureBookDto>> = runCatching {
        apiService.getScriptureBooks().books
    }

    suspend fun getScriptureChapter(bookId: String, chapter: Int): Result<ScriptureChapterDto> = runCatching {
        apiService.getScriptureChapter(bookId, chapter)
    }

    suspend fun getScriptureVerse(bookId: String, chapter: Int, verse: Int): Result<List<SermonDto>> = runCatching {
        apiService.getScriptureVerse(bookId, chapter, verse).sermons
    }

    suspend fun getSpeakers(): Result<List<SpeakerDto>> = runCatching {
        apiService.getSpeakers().speakers
    }

    suspend fun getSpeakerDetails(slug: String): Result<SpeakerDto> = runCatching {
        apiService.getSpeakerDetails(slug)
    }

    suspend fun getSpeakerSermonsPage(slug: String, page: Int): Result<SpeakerPagedDto> = runCatching {
        apiService.getSpeakerSermonsPage(slug, page)
    }

    suspend fun getTopics(): Result<List<TopicDto>> = runCatching {
        apiService.getTopics().topics
    }

    suspend fun getTopicDetails(slug: String): Result<TopicDetailDto> = runCatching {
        apiService.getTopicDetails(slug)
    }

    suspend fun getSermonDetails(id: String): Result<SermonDetailDto> = runCatching {
        apiService.getSermonDetails(id)
    }

    val ftsIndexedCount: Flow<Int> = database.getFtsCountFlow()

    suspend fun searchOffline(query: String): List<com.example.sermonindex.data.local.entities.FtsSermonResult> {
        return database.searchFts(query)
    }

    suspend fun clearOfflineIndex() {
        database.clearFts()
    }
}
