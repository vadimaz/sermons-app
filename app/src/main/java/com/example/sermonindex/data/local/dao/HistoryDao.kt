package com.example.sermonindex.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.sermonindex.data.local.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY updated_at DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 30): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE sermon_id = :sermonId LIMIT 1")
    suspend fun getHistory(sermonId: String): HistoryEntity?

    @Upsert
    suspend fun upsertHistory(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE sermon_id = :sermonId")
    suspend fun deleteHistory(sermonId: String): Int

    @Query("DELETE FROM history")
    suspend fun clearHistory(): Int
}
