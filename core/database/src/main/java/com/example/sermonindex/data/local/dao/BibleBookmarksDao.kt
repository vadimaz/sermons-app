package com.example.sermonindex.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.sermonindex.data.local.entities.BibleBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleBookmarksDao {

    @Query("SELECT * FROM bible_bookmarks ORDER BY created_at DESC")
    fun getAllBookmarks(): Flow<List<BibleBookmarkEntity>>

    @Query("SELECT * FROM bible_bookmarks WHERE translation_id = :translationId AND book_id = :bookId AND chapter = :chapter")
    fun getBookmarksForChapter(translationId: String, bookId: String, chapter: Int): Flow<List<BibleBookmarkEntity>>

    @Upsert
    suspend fun upsertBookmark(entity: BibleBookmarkEntity)

    @Query("DELETE FROM bible_bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String): Int

    @Query("DELETE FROM bible_bookmarks WHERE translation_id = :translationId AND book_id = :bookId AND chapter = :chapter AND verse = :verse")
    suspend fun deleteBookmarkForVerse(translationId: String, bookId: String, chapter: Int, verse: Int): Int
}
