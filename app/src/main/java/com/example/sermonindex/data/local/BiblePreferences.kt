package com.example.sermonindex.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the user's preferred Bible translations for dual-translation Scripture display.
 * Default translation 1 is always BSB.
 * Default translation 2 is determined by system locale, falling back to KJV if English or unsupported.
 */
@Singleton
class BiblePreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _firstTranslationId = MutableStateFlow(
        prefs.getString(KEY_FIRST_TRANSLATION, DEFAULT_FIRST) ?: DEFAULT_FIRST
    )
    val firstTranslationId: StateFlow<String> = _firstTranslationId.asStateFlow()

    private val _secondTranslationId = MutableStateFlow(
        prefs.getString(KEY_SECOND_TRANSLATION, getDefaultSecondaryTranslation()) ?: getDefaultSecondaryTranslation()
    )
    val secondTranslationId: StateFlow<String> = _secondTranslationId.asStateFlow()

    fun setFirstTranslation(id: String) {
        prefs.edit().putString(KEY_FIRST_TRANSLATION, id).apply()
        _firstTranslationId.value = id
    }

    fun setSecondTranslation(id: String) {
        prefs.edit().putString(KEY_SECOND_TRANSLATION, id).apply()
        _secondTranslationId.value = id
    }

    companion object {
        private const val PREFS_NAME = "bible_preferences"
        private const val KEY_FIRST_TRANSLATION = "first_translation_v2"
        private const val KEY_SECOND_TRANSLATION = "second_translation_v2"

        const val DEFAULT_FIRST = "BSB"

        fun getDefaultSecondaryTranslation(): String {
            val lang = Locale.getDefault().language.lowercase()
            return when (lang) {
                "ru" -> "rus_syn"
                "uk" -> "ukr_1996"
                "es" -> "spa_r09"
                "de" -> "deu_elbbk"
                else -> "eng_kjv"
            }
        }
    }
}
