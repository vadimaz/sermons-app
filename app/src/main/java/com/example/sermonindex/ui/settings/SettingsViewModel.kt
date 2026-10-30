package com.example.sermonindex.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sermonindex.data.local.CacheManager
import com.example.sermonindex.domain.model.AppThemeMode
import com.example.sermonindex.domain.model.CatalogSyncState
import com.example.sermonindex.domain.usecase.DeleteCatalogUseCase
import com.example.sermonindex.domain.usecase.DownloadOrUpdateCatalogUseCase
import com.example.sermonindex.domain.usecase.GetAppThemeModeUseCase
import com.example.sermonindex.domain.usecase.GetAutoCatalogUpdateCheckEnabledUseCase
import com.example.sermonindex.domain.usecase.GetCatalogSyncStateUseCase
import com.example.sermonindex.domain.usecase.SetAppThemeModeUseCase
import com.example.sermonindex.domain.usecase.SetAutoCatalogUpdateCheckEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val cacheSizeFormatted: String = "0 KB",
    val catalogState: CatalogSyncState = CatalogSyncState.NotDownloaded,
    val isAutoCatalogUpdateCheckEnabled: Boolean = false,
    val isClearingCache: Boolean = false,
    val isDeletingCatalog: Boolean = false
) {
    val isSystemTheme: Boolean get() = themeMode == AppThemeMode.SYSTEM
}

sealed interface SettingsUiEvent {
    data class SetThemeMode(val mode: AppThemeMode) : SettingsUiEvent
    data class SetSystemTheme(val enabled: Boolean) : SettingsUiEvent
    data object ClearCache : SettingsUiEvent
    data class SetAutoCatalogUpdateCheck(val enabled: Boolean) : SettingsUiEvent
    data object TriggerCatalogSync : SettingsUiEvent
    data object DeleteCatalog : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getAppThemeModeUseCase: GetAppThemeModeUseCase,
    private val setAppThemeModeUseCase: SetAppThemeModeUseCase,
    private val cacheManager: CacheManager,
    private val getCatalogSyncStateUseCase: GetCatalogSyncStateUseCase,
    private val downloadOrUpdateCatalogUseCase: DownloadOrUpdateCatalogUseCase,
    private val deleteCatalogUseCase: DeleteCatalogUseCase,
    private val getAutoCatalogUpdateCheckEnabledUseCase: GetAutoCatalogUpdateCheckEnabledUseCase,
    private val setAutoCatalogUpdateCheckEnabledUseCase: SetAutoCatalogUpdateCheckEnabledUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        refreshCacheSize()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getAppThemeModeUseCase().collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            getCatalogSyncStateUseCase().collect { catState ->
                _uiState.update { it.copy(catalogState = catState) }
            }
        }
        viewModelScope.launch {
            getAutoCatalogUpdateCheckEnabledUseCase().collect { enabled ->
                _uiState.update { it.copy(isAutoCatalogUpdateCheckEnabled = enabled) }
            }
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val formatted = cacheManager.getFormattedCacheSize()
            _uiState.update { it.copy(cacheSizeFormatted = formatted) }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SetThemeMode -> {
                viewModelScope.launch {
                    setAppThemeModeUseCase(event.mode)
                }
            }
            is SettingsUiEvent.SetSystemTheme -> {
                viewModelScope.launch {
                    val targetMode = if (event.enabled) AppThemeMode.SYSTEM else AppThemeMode.LIGHT
                    setAppThemeModeUseCase(targetMode)
                }
            }
            is SettingsUiEvent.ClearCache -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isClearingCache = true) }
                    cacheManager.clearCache()
                    val newSize = cacheManager.getFormattedCacheSize()
                    _uiState.update { it.copy(isClearingCache = false, cacheSizeFormatted = newSize) }
                }
            }
            is SettingsUiEvent.SetAutoCatalogUpdateCheck -> {
                viewModelScope.launch {
                    setAutoCatalogUpdateCheckEnabledUseCase(event.enabled)
                }
            }
            is SettingsUiEvent.TriggerCatalogSync -> {
                viewModelScope.launch {
                    downloadOrUpdateCatalogUseCase()
                }
            }
            is SettingsUiEvent.DeleteCatalog -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isDeletingCatalog = true) }
                    deleteCatalogUseCase()
                    _uiState.update { it.copy(isDeletingCatalog = false) }
                }
            }
        }
    }
}
