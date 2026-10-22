package com.example.sermonindex.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isCatalogReadyBannerDismissed = MutableStateFlow(
        prefs.getBoolean(KEY_CATALOG_READY_DISMISSED, false)
    )
    val isCatalogReadyBannerDismissed: StateFlow<Boolean> = _isCatalogReadyBannerDismissed.asStateFlow()

    fun setCatalogReadyBannerDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_CATALOG_READY_DISMISSED, dismissed).apply()
        _isCatalogReadyBannerDismissed.value = dismissed
    }

    companion object {
        private const val PREFS_NAME = "search_preferences"
        private const val KEY_CATALOG_READY_DISMISSED = "catalog_ready_dismissed"
    }
}
