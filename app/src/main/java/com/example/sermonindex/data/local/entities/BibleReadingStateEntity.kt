package com.example.sermonindex.data.local.entities

data class BibleReadingStateEntity(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val fontSizeSp: Float,
    val isSerif: Boolean,
    val lineSpacingMultiplier: Float,
    val selectedNarrator: String?,
    val updatedAt: Long
)
