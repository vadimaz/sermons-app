package com.example.sermonindex.data.local.dao

import com.example.sermonindex.data.local.entities.BibleReadingStateEntity
import kotlinx.coroutines.flow.Flow

interface BibleDao {
    fun getReadingState(): Flow<BibleReadingStateEntity?>
    suspend fun saveReadingPosition(translationId: String, bookId: String, chapter: Int, verse: Int)
    suspend fun saveTypography(fontSizeSp: Float, isSerif: Boolean, lineSpacingMultiplier: Float)
    suspend fun saveNarrator(narrator: String?)
}
