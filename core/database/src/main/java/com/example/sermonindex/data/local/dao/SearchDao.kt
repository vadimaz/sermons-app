package com.example.sermonindex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.sermonindex.data.local.entities.CatalogMetadataEntity
import com.example.sermonindex.data.local.entities.SearchHistoryEntity
import com.example.sermonindex.data.local.entities.SermonFtsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {

    @Query("SELECT rowid, * FROM sermon_fts WHERE sermon_fts MATCH :query LIMIT :limit")
    suspend fun searchSermonsFts(query: String, limit: Int): List<SermonFtsEntity>

    @Query("SELECT COUNT(*) FROM sermon_fts WHERE sermon_fts MATCH :query")
    suspend fun countSermonsFts(query: String): Int

    @Query("SELECT COUNT(*) FROM sermon_fts")
    suspend fun getFtsSermonCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSermonsFtsBatch(records: List<SermonFtsEntity>)

    @Query("DELETE FROM sermon_fts")
    suspend fun clearSermonsFts(): Int

    @Query("SELECT * FROM catalog_metadata WHERE id = 1")
    fun getCatalogMetadataFlow(): Flow<CatalogMetadataEntity?>

    @Query("SELECT * FROM catalog_metadata WHERE id = 1")
    suspend fun getCatalogMetadata(): CatalogMetadataEntity?

    @Upsert
    suspend fun saveCatalogMetadata(metadata: CatalogMetadataEntity)

    @Query("DELETE FROM catalog_metadata")
    suspend fun clearCatalogMetadata(): Int

    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearchQueries(limit: Int = 10): Flow<List<String>>

    @Upsert
    suspend fun insertSearchQuery(query: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String): Int

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory(): Int
}
