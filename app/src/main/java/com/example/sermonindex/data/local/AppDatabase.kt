package com.example.sermonindex.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.dao.FavoritesDao
import com.example.sermonindex.data.local.dao.HistoryDao
import com.example.sermonindex.data.local.entities.DownloadedSermonEntity
import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)
    private val favoritesChangeSignal = MutableStateFlow(0L)
    private val historyChangeSignal = MutableStateFlow(0L)
    private val downloadsChangeSignal = MutableStateFlow(0L)

    val favoritesDao: FavoritesDao = object : FavoritesDao {
        override fun getAllFavorites(): Flow<List<FavoriteSermonEntity>> {
            return favoritesChangeSignal.asStateFlow().map {
                queryFavorites()
            }
        }

        override fun isFavorite(id: String): Flow<Boolean> {
            return favoritesChangeSignal.asStateFlow().map {
                checkIsFavorite(id)
            }
        }

        override suspend fun insertFavorite(entity: FavoriteSermonEntity) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", entity.id)
                put("title", entity.title)
                put("speaker", entity.speaker)
                put("speaker_image", entity.speakerImage)
                put("duration", entity.duration)
                put("added_at", entity.addedAt)
            }
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            favoritesChangeSignal.value = System.currentTimeMillis()
        }

        override suspend fun deleteFavorite(id: String) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.delete("favorites", "id = ?", arrayOf(id))
            favoritesChangeSignal.value = System.currentTimeMillis()
        }
    }

    val historyDao: HistoryDao = object : HistoryDao {
        override fun getRecentHistory(limit: Int): Flow<List<HistoryEntity>> {
            return historyChangeSignal.asStateFlow().map {
                queryHistory(limit)
            }
        }

        override suspend fun getHistory(sermonId: String): HistoryEntity? = withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT sermon_id, title, speaker, speaker_image, last_position_ms, duration_ms, updated_at, completed FROM history WHERE sermon_id = ?",
                arrayOf(sermonId)
            )
            cursor.use {
                if (it.moveToFirst()) {
                    HistoryEntity(
                        sermonId = it.getString(0),
                        title = it.getString(1),
                        speaker = it.getString(2),
                        speakerImage = it.getString(3),
                        lastPositionMs = it.getLong(4),
                        durationMs = it.getLong(5),
                        updatedAt = it.getLong(6),
                        completed = it.getInt(7) == 1
                    )
                } else null
            }
        }

        override suspend fun upsertHistory(entity: HistoryEntity) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("sermon_id", entity.sermonId)
                put("title", entity.title)
                put("speaker", entity.speaker)
                put("speaker_image", entity.speakerImage)
                put("last_position_ms", entity.lastPositionMs)
                put("duration_ms", entity.durationMs)
                put("updated_at", entity.updatedAt)
                put("completed", if (entity.completed) 1 else 0)
            }
            db.insertWithOnConflict("history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            historyChangeSignal.value = System.currentTimeMillis()
        }

        override suspend fun deleteHistory(sermonId: String) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.delete("history", "sermon_id = ?", arrayOf(sermonId))
            historyChangeSignal.value = System.currentTimeMillis()
        }

        override suspend fun clearHistory() = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.delete("history", null, null)
            historyChangeSignal.value = System.currentTimeMillis()
        }
    }

    val downloadsDao: DownloadsDao = object : DownloadsDao {
        override fun getAllDownloads(): Flow<List<DownloadedSermonEntity>> {
            return downloadsChangeSignal.asStateFlow().map {
                queryDownloads()
            }
        }

        override suspend fun getDownload(id: String): DownloadedSermonEntity? = withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT id, title, speaker, speaker_image, duration, local_path, file_size_bytes, downloaded_at FROM downloads WHERE id = ?",
                arrayOf(id)
            )
            cursor.use {
                if (it.moveToFirst()) {
                    DownloadedSermonEntity(
                        id = it.getString(0),
                        title = it.getString(1),
                        speaker = it.getString(2),
                        speakerImage = it.getString(3),
                        duration = it.getString(4),
                        localPath = it.getString(5),
                        fileSizeBytes = it.getLong(6),
                        downloadedAt = it.getLong(7)
                    )
                } else null
            }
        }

        override fun isDownloaded(id: String): Flow<Boolean> {
            return downloadsChangeSignal.asStateFlow().map {
                checkIsDownloaded(id)
            }
        }

        override suspend fun insertDownload(entity: DownloadedSermonEntity) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", entity.id)
                put("title", entity.title)
                put("speaker", entity.speaker)
                put("speaker_image", entity.speakerImage)
                put("duration", entity.duration)
                put("local_path", entity.localPath)
                put("file_size_bytes", entity.fileSizeBytes)
                put("downloaded_at", entity.downloadedAt)
            }
            db.insertWithOnConflict("downloads", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            downloadsChangeSignal.value = System.currentTimeMillis()
        }

        override suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.delete("downloads", "id = ?", arrayOf(id))
            downloadsChangeSignal.value = System.currentTimeMillis()
        }

        override suspend fun deleteAllDownloads() = withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.delete("downloads", null, null)
            downloadsChangeSignal.value = System.currentTimeMillis()
        }

        override suspend fun getTotalStorageBytes(): Long = withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT SUM(file_size_bytes) FROM downloads", null)
            cursor.use {
                if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else 0L
            }
        }
    }

    private suspend fun queryFavorites(): List<FavoriteSermonEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FavoriteSermonEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, title, speaker, speaker_image, duration, added_at FROM favorites ORDER BY added_at DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FavoriteSermonEntity(
                        id = it.getString(0),
                        title = it.getString(1),
                        speaker = it.getString(2),
                        speakerImage = it.getString(3),
                        duration = it.getString(4),
                        addedAt = it.getLong(5)
                    )
                )
            }
        }
        list
    }

    private suspend fun checkIsFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM favorites WHERE id = ? LIMIT 1", arrayOf(id))
        cursor.use { it.moveToFirst() }
    }

    private suspend fun queryHistory(limit: Int): List<HistoryEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<HistoryEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT sermon_id, title, speaker, speaker_image, last_position_ms, duration_ms, updated_at, completed FROM history ORDER BY updated_at DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    HistoryEntity(
                        sermonId = it.getString(0),
                        title = it.getString(1),
                        speaker = it.getString(2),
                        speakerImage = it.getString(3),
                        lastPositionMs = it.getLong(4),
                        durationMs = it.getLong(5),
                        updatedAt = it.getLong(6),
                        completed = it.getInt(7) == 1
                    )
                )
            }
        }
        list
    }

    private suspend fun queryDownloads(): List<DownloadedSermonEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DownloadedSermonEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, title, speaker, speaker_image, duration, local_path, file_size_bytes, downloaded_at FROM downloads ORDER BY downloaded_at DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    DownloadedSermonEntity(
                        id = it.getString(0),
                        title = it.getString(1),
                        speaker = it.getString(2),
                        speakerImage = it.getString(3),
                        duration = it.getString(4),
                        localPath = it.getString(5),
                        fileSizeBytes = it.getLong(6),
                        downloadedAt = it.getLong(7)
                    )
                )
            }
        }
        list
    }

    private suspend fun checkIsDownloaded(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM downloads WHERE id = ? LIMIT 1", arrayOf(id))
        cursor.use { it.moveToFirst() }
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "sermonindex.db", null, 2) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE favorites (
                    id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    speaker TEXT NOT NULL,
                    speaker_image TEXT NOT NULL,
                    duration TEXT NOT NULL,
                    added_at INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE history (
                    sermon_id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    speaker TEXT NOT NULL,
                    speaker_image TEXT NOT NULL,
                    last_position_ms INTEGER NOT NULL,
                    duration_ms INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    completed INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE downloads (
                    id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    speaker TEXT NOT NULL,
                    speaker_image TEXT NOT NULL,
                    duration TEXT NOT NULL,
                    local_path TEXT NOT NULL,
                    file_size_bytes INTEGER NOT NULL,
                    downloaded_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloads (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        speaker TEXT NOT NULL,
                        speaker_image TEXT NOT NULL,
                        duration TEXT NOT NULL,
                        local_path TEXT NOT NULL,
                        file_size_bytes INTEGER NOT NULL,
                        downloaded_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
            db.execSQL("DROP TABLE IF EXISTS sermon_fts")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context).also { INSTANCE = it }
            }
        }
    }
}
