package com.example.sermonindex.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.sermonindex.data.local.dao.BibleDao
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.dao.FavoritesDao
import com.example.sermonindex.data.local.dao.HistoryDao
import com.example.sermonindex.data.local.entities.BibleReadingStateEntity
import com.example.sermonindex.data.local.entities.CatalogMetadataEntity
import com.example.sermonindex.data.local.entities.DownloadedSermonEntity
import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import com.example.sermonindex.data.local.entities.FtsSermonRecord
import com.example.sermonindex.data.local.entities.HistoryEntity
import com.example.sermonindex.domain.model.Sermon
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
    private val bibleChangeSignal = MutableStateFlow(0L)
    private val catalogChangeSignal = MutableStateFlow(0L)
    private val searchHistoryChangeSignal = MutableStateFlow(0L)

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

    val bibleDao: BibleDao = object : BibleDao {
        override fun getReadingState(): Flow<BibleReadingStateEntity?> {
            return bibleChangeSignal.asStateFlow().map {
                queryBibleReadingState()
            }
        }

        override suspend fun saveReadingPosition(
            translationId: String,
            bookId: String,
            chapter: Int,
            verse: Int
        ) = withContext(Dispatchers.IO) {
            val existing = queryBibleReadingState()
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", 1)
                put("translation_id", translationId)
                put("book_id", bookId)
                put("chapter", chapter)
                put("verse", verse)
                put("font_size_sp", existing?.fontSizeSp ?: 18.0f)
                put("is_serif", if (existing?.isSerif != false) 1 else 0)
                put("line_spacing_multiplier", existing?.lineSpacingMultiplier ?: 1.5f)
                put("selected_narrator", existing?.selectedNarrator)
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("bible_reading_state", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            bibleChangeSignal.value = System.currentTimeMillis()
        }

        override suspend fun saveTypography(
            fontSizeSp: Float,
            isSerif: Boolean,
            lineSpacingMultiplier: Float
        ) = withContext(Dispatchers.IO) {
            val existing = queryBibleReadingState()
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", 1)
                put("translation_id", existing?.translationId ?: "rus_syn")
                put("book_id", existing?.bookId ?: "JHN")
                put("chapter", existing?.chapter ?: 1)
                put("verse", existing?.verse ?: 1)
                put("font_size_sp", fontSizeSp)
                put("is_serif", if (isSerif) 1 else 0)
                put("line_spacing_multiplier", lineSpacingMultiplier)
                put("selected_narrator", existing?.selectedNarrator)
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("bible_reading_state", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            bibleChangeSignal.value = System.currentTimeMillis()
        }

        override suspend fun saveNarrator(narrator: String?) = withContext(Dispatchers.IO) {
            val existing = queryBibleReadingState()
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("id", 1)
                put("translation_id", existing?.translationId ?: "rus_syn")
                put("book_id", existing?.bookId ?: "JHN")
                put("chapter", existing?.chapter ?: 1)
                put("verse", existing?.verse ?: 1)
                put("font_size_sp", existing?.fontSizeSp ?: 18.0f)
                put("is_serif", if (existing?.isSerif != false) 1 else 0)
                put("line_spacing_multiplier", existing?.lineSpacingMultiplier ?: 1.5f)
                put("selected_narrator", narrator)
                put("updated_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("bible_reading_state", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            bibleChangeSignal.value = System.currentTimeMillis()
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

    private suspend fun queryBibleReadingState(): BibleReadingStateEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT translation_id, book_id, chapter, verse, font_size_sp, is_serif, line_spacing_multiplier, selected_narrator, updated_at FROM bible_reading_state WHERE id = 1",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                BibleReadingStateEntity(
                    translationId = it.getString(0),
                    bookId = it.getString(1),
                    chapter = it.getInt(2),
                    verse = it.getInt(3),
                    fontSizeSp = it.getFloat(4),
                    isSerif = it.getInt(5) == 1,
                    lineSpacingMultiplier = it.getFloat(6),
                    selectedNarrator = it.getString(7),
                    updatedAt = it.getLong(8)
                )
            } else null
        }
    }

    fun getRecentSearchQueries(limit: Int = 10): Flow<List<String>> {
        return searchHistoryChangeSignal.asStateFlow().map {
            queryRecentSearchQueries(limit)
        }
    }

    suspend fun addRecentSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("query", clean)
            put("timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict("search_history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        searchHistoryChangeSignal.value = System.currentTimeMillis()
    }

    suspend fun deleteRecentSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("search_history", "query = ?", arrayOf(query.trim()))
        searchHistoryChangeSignal.value = System.currentTimeMillis()
    }

    suspend fun clearRecentSearchQueries() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("search_history", null, null)
        searchHistoryChangeSignal.value = System.currentTimeMillis()
    }

    private suspend fun queryRecentSearchQueries(limit: Int): List<String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT query FROM search_history ORDER BY timestamp DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getString(0))
            }
        }
        list
    }

    fun getCatalogMetadataFlow(): Flow<CatalogMetadataEntity?> {
        return catalogChangeSignal.asStateFlow().map {
            getCatalogMetadata()
        }
    }

    suspend fun getCatalogMetadata(): CatalogMetadataEntity? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT sermon_count, generated_at, downloaded_at FROM catalog_metadata WHERE id = 1",
            null
        )
        val meta = cursor.use {
            if (it.moveToFirst()) {
                CatalogMetadataEntity(
                    sermonCount = it.getInt(0),
                    generatedAt = it.getString(1),
                    downloadedAt = it.getLong(2)
                )
            } else null
        }
        if (meta != null) {
            val ftsCount = getFtsSermonCount()
            if (ftsCount == 0) {
                val writable = dbHelper.writableDatabase
                writable.execSQL("DELETE FROM catalog_metadata")
                return@withContext null
            }
        }
        meta
    }

    suspend fun saveCatalogMetadata(
        sermonCount: Int,
        generatedAt: String,
        downloadedAt: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", 1)
            put("sermon_count", sermonCount)
            put("generated_at", generatedAt)
            put("downloaded_at", downloadedAt)
        }
        db.insertWithOnConflict("catalog_metadata", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        catalogChangeSignal.value = System.currentTimeMillis()
    }

    suspend fun insertSermonsFtsBatch(records: List<FtsSermonRecord>) = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT INTO sermon_fts (sermon_id, title, speaker, summary, duration, speaker_image, has_audio, mp3_url, cdn_mp3_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )
            for (rec in records) {
                stmt.bindString(1, rec.id)
                stmt.bindString(2, rec.title)
                stmt.bindString(3, rec.speaker)
                stmt.bindString(4, rec.summary)
                stmt.bindString(5, rec.duration)
                stmt.bindString(6, rec.speakerImage)
                stmt.bindLong(7, if (rec.hasAudio) 1L else 0L)
                stmt.bindString(8, rec.mp3Url)
                stmt.bindString(9, rec.cdnMp3Url)
                stmt.executeInsert()
                stmt.clearBindings()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun clearSermonsFts() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        DatabaseHelper.recreateSermonFtsTable(db)
        db.execSQL("DELETE FROM catalog_metadata")
        catalogChangeSignal.value = System.currentTimeMillis()
    }

    suspend fun getFtsSermonCount(): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM sermon_fts", null)
        cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    suspend fun countSermonsFts(query: String): Int = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext 0
        val tokens = clean.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@withContext 0
        val ftsMatch = tokens.joinToString(" ") { "$it*" }
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM sermon_fts WHERE sermon_fts MATCH ?",
                arrayOf(ftsMatch)
            )
            cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } catch (e: Exception) {
            android.util.Log.e("AppDatabase", "countSermonsFts failed for query: $query", e)
            0
        }
    }

    suspend fun searchSermonsFts(query: String, limit: Int = 50): List<Sermon> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext emptyList()
        val tokens = clean.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@withContext emptyList()
        val ftsMatch = tokens.joinToString(" ") { "$it*" }

        val list = mutableListOf<Sermon>()
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.rawQuery(
                """
                SELECT *
                FROM sermon_fts
                WHERE sermon_fts MATCH ?
                LIMIT ?
                """.trimIndent(),
                arrayOf(ftsMatch, limit.toString())
            )
            cursor.use {
                val idIdx = it.getColumnIndex("sermon_id")
                val titleIdx = it.getColumnIndex("title")
                val speakerIdx = it.getColumnIndex("speaker")
                val summaryIdx = it.getColumnIndex("summary")
                val durationIdx = it.getColumnIndex("duration")
                val imgIdx = it.getColumnIndex("speaker_image")
                val audioIdx = it.getColumnIndex("has_audio")
                val mp3Idx = it.getColumnIndex("mp3_url")
                val cdnIdx = it.getColumnIndex("cdn_mp3_url")

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx).orEmpty() else ""
                    val title = if (titleIdx >= 0) it.getString(titleIdx).orEmpty() else ""
                    val speaker = if (speakerIdx >= 0) it.getString(speakerIdx).orEmpty() else ""
                    val summary = if (summaryIdx >= 0) it.getString(summaryIdx).orEmpty() else ""
                    val duration = if (durationIdx >= 0) it.getString(durationIdx).orEmpty() else ""
                    val speakerImage = if (imgIdx >= 0) it.getString(imgIdx).orEmpty() else ""
                    val hasAudio = if (audioIdx >= 0) it.getInt(audioIdx) == 1 else false
                    val mp3 = if (mp3Idx >= 0) it.getString(mp3Idx).orEmpty() else ""
                    val cdn = if (cdnIdx >= 0) it.getString(cdnIdx).orEmpty() else ""

                    list.add(
                        Sermon(
                            id = id,
                            title = title,
                            speaker = speaker,
                            summary = summary,
                            duration = duration,
                            speakerImage = speakerImage,
                            hasAudio = hasAudio,
                            mp3Url = mp3,
                            cdnMp3Url = cdn
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppDatabase", "searchSermonsFts failed for query: $query", e)
        }
        list
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "sermonindex.db", null, 6) {
        override fun onOpen(db: SQLiteDatabase) {
            super.onOpen(db)
            try {
                val cursor = db.rawQuery("SELECT COUNT(*) FROM sermon_fts", null)
                val count = cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
                if (count == 0) {
                    db.execSQL("DELETE FROM catalog_metadata")
                }
            } catch (_: Exception) {
                // Ignore if table is being created
            }
        }
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

            db.execSQL(
                """
                CREATE TABLE bible_reading_state (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    translation_id TEXT NOT NULL,
                    book_id TEXT NOT NULL,
                    chapter INTEGER NOT NULL,
                    verse INTEGER NOT NULL DEFAULT 1,
                    font_size_sp REAL NOT NULL DEFAULT 18.0,
                    is_serif INTEGER NOT NULL DEFAULT 1,
                    line_spacing_multiplier REAL NOT NULL DEFAULT 1.5,
                    selected_narrator TEXT,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE catalog_metadata (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    sermon_count INTEGER NOT NULL,
                    generated_at TEXT NOT NULL,
                    downloaded_at INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE search_history (
                    query TEXT PRIMARY KEY NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )

            recreateSermonFtsTable(db)
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
            if (oldVersion < 3) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bible_reading_state (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        translation_id TEXT NOT NULL,
                        book_id TEXT NOT NULL,
                        chapter INTEGER NOT NULL,
                        verse INTEGER NOT NULL DEFAULT 1,
                        font_size_sp REAL NOT NULL DEFAULT 18.0,
                        is_serif INTEGER NOT NULL DEFAULT 1,
                        line_spacing_multiplier REAL NOT NULL DEFAULT 1.5,
                        selected_narrator TEXT,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
            if (oldVersion < 4) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS catalog_metadata (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        sermon_count INTEGER NOT NULL,
                        generated_at TEXT NOT NULL,
                        downloaded_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS search_history (
                        query TEXT PRIMARY KEY NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                recreateSermonFtsTable(db)
            }
            if (oldVersion < 5) {
                recreateSermonFtsTable(db)
            }
            if (oldVersion < 6) {
                try {
                    val cursor = db.rawQuery("SELECT COUNT(*) FROM sermon_fts", null)
                    val count = cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
                    if (count == 0) {
                        recreateSermonFtsTable(db)
                        db.execSQL("DELETE FROM catalog_metadata")
                    }
                } catch (_: Exception) {
                    recreateSermonFtsTable(db)
                    db.execSQL("DELETE FROM catalog_metadata")
                }
            }
        }

        companion object {
            fun recreateSermonFtsTable(db: SQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS sermon_fts")
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE sermon_fts USING fts4(
                        sermon_id TEXT,
                        title TEXT,
                        speaker TEXT,
                        summary TEXT,
                        duration TEXT,
                        speaker_image TEXT,
                        has_audio INTEGER,
                        mp3_url TEXT,
                        cdn_mp3_url TEXT,
                        tokenize=unicode61
                    )
                    """.trimIndent()
                )
            }
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
