package com.example.sermonindex.data.local.dao

import com.example.sermonindex.data.local.entities.DownloadedSermonEntity
import kotlinx.coroutines.flow.Flow

interface DownloadsDao {
    fun getAllDownloads(): Flow<List<DownloadedSermonEntity>>
    suspend fun getDownload(id: String): DownloadedSermonEntity?
    fun isDownloaded(id: String): Flow<Boolean>
    suspend fun insertDownload(entity: DownloadedSermonEntity)
    suspend fun deleteDownload(id: String)
    suspend fun deleteAllDownloads()
    suspend fun getTotalStorageBytes(): Long
}
