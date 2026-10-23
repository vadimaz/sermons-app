package com.example.sermonindex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sermonindex.data.local.entities.DownloadedSermonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadsDao {
    @Query("SELECT * FROM downloads ORDER BY downloaded_at DESC")
    fun getAllDownloads(): Flow<List<DownloadedSermonEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownload(id: String): DownloadedSermonEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE id = :id LIMIT 1)")
    fun isDownloaded(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(entity: DownloadedSermonEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String): Int

    @Query("DELETE FROM downloads")
    suspend fun deleteAllDownloads(): Int

    @Query("SELECT COALESCE(SUM(file_size_bytes), 0) FROM downloads")
    suspend fun getTotalStorageBytes(): Long
}
