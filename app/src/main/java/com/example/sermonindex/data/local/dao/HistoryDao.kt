package com.example.sermonindex.data.local.dao

import com.example.sermonindex.data.local.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

interface HistoryDao {
    fun getRecentHistory(limit: Int = 30): Flow<List<HistoryEntity>>
    suspend fun getHistory(sermonId: String): HistoryEntity?
    suspend fun upsertHistory(entity: HistoryEntity)
    suspend fun deleteHistory(sermonId: String)
    suspend fun clearHistory()
}
