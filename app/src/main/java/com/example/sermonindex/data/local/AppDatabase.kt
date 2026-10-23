package com.example.sermonindex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.dao.FavoritesDao
import com.example.sermonindex.data.local.dao.HistoryDao
import com.example.sermonindex.data.local.dao.SearchDao
import com.example.sermonindex.data.local.entities.CatalogMetadataEntity
import com.example.sermonindex.data.local.entities.DownloadedSermonEntity
import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.data.local.entities.SearchHistoryEntity
import com.example.sermonindex.data.local.entities.SermonFtsEntity

@Database(
    entities = [
        FavoriteSermonEntity::class,
        HistoryEntity::class,
        DownloadedSermonEntity::class,
        CatalogMetadataEntity::class,
        SearchHistoryEntity::class,
        SermonFtsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadsDao(): DownloadsDao
    abstract fun searchDao(): SearchDao

    val favoritesDao: FavoritesDao get() = favoritesDao()
    val historyDao: HistoryDao get() = historyDao()
    val downloadsDao: DownloadsDao get() = downloadsDao()
    val searchDao: SearchDao get() = searchDao()

    companion object {
        const val DATABASE_NAME = "sermonindex.db"
    }
}
