package com.example.sermonindex.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sermonindex.data.local.dao.BibleBookmarksDao
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.dao.FavoritesDao
import com.example.sermonindex.data.local.dao.HistoryDao
import com.example.sermonindex.data.local.dao.SearchDao
import com.example.sermonindex.data.local.entities.BibleBookmarkEntity
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
        SermonFtsEntity::class,
        BibleBookmarkEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadsDao(): DownloadsDao
    abstract fun searchDao(): SearchDao
    abstract fun bibleBookmarksDao(): BibleBookmarksDao

    companion object {
        const val DATABASE_NAME = "sermonindex.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bible_bookmarks` (
                        `id` TEXT NOT NULL,
                        `translation_id` TEXT NOT NULL,
                        `book_id` TEXT NOT NULL,
                        `book_name` TEXT NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `verse` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `color_name` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bible_bookmarks_translation_id_book_id_chapter` ON `bible_bookmarks` (`translation_id`, `book_id`, `chapter`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bible_bookmarks_created_at` ON `bible_bookmarks` (`created_at`)")
            }
        }
    }
}
