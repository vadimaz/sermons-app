package com.example.sermonindex.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    @Test
    fun migration1To2_createsBibleBookmarksTableAndIndices() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("test_migration.db")
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create version 1 tables (favorites, history, downloads)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `favorites` (
                            `id` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `speaker` TEXT NOT NULL,
                            `speaker_image` TEXT NOT NULL,
                            `duration` TEXT NOT NULL,
                            `added_at` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // Not called on initial creation
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Execute migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Verify table existence
        val tableCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='bible_bookmarks'"
        )
        assertTrue(tableCursor.moveToFirst())
        assertEquals("bible_bookmarks", tableCursor.getString(0))
        tableCursor.close()

        // Verify indices existence
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'index_bible_bookmarks_%'"
        )
        val indices = mutableListOf<String>()
        while (indexCursor.moveToNext()) {
            indices.add(indexCursor.getString(0))
        }
        indexCursor.close()

        assertTrue(indices.contains("index_bible_bookmarks_translation_id_book_id_chapter"))
        assertTrue(indices.contains("index_bible_bookmarks_created_at"))

        // Test insertion into newly migrated table
        db.execSQL(
            """
            INSERT INTO bible_bookmarks (id, translation_id, book_id, book_name, chapter, verse, text, color_name, created_at)
            VALUES ('BSB:GEN:1:1', 'BSB', 'GEN', 'Genesis', 1, 1, 'In the beginning...', 'YELLOW', 1000)
            """.trimIndent()
        )

        val rowCursor = db.query("SELECT COUNT(*) FROM bible_bookmarks")
        assertTrue(rowCursor.moveToFirst())
        assertEquals(1, rowCursor.getInt(0))
        rowCursor.close()

        db.close()
    }
}
