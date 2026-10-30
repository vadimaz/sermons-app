package com.example.sermonindex.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.sermonindex.data.local.CacheManager
import com.example.sermonindex.domain.model.AppThemeMode
import com.example.sermonindex.domain.model.CatalogSyncState
import com.example.sermonindex.domain.usecase.DeleteCatalogUseCase
import com.example.sermonindex.domain.usecase.DownloadOrUpdateCatalogUseCase
import com.example.sermonindex.domain.usecase.GetAppThemeModeUseCase
import com.example.sermonindex.domain.usecase.GetCatalogSyncStateUseCase
import com.example.sermonindex.domain.usecase.SetAppThemeModeUseCase
import com.example.sermonindex.testing.FakeSermonRepository
import com.example.sermonindex.testing.FakeUserPreferencesRepository
import com.example.sermonindex.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private lateinit var fakeSermonRepository: FakeSermonRepository
    private lateinit var cacheManager: CacheManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        fakePreferencesRepository = FakeUserPreferencesRepository()
        fakeSermonRepository = FakeSermonRepository()
        val context = ApplicationProvider.getApplicationContext<Context>()
        cacheManager = CacheManager(context, testDispatcher)

        viewModel = SettingsViewModel(
            getAppThemeModeUseCase = GetAppThemeModeUseCase(fakePreferencesRepository),
            setAppThemeModeUseCase = SetAppThemeModeUseCase(fakePreferencesRepository),
            cacheManager = cacheManager,
            getCatalogSyncStateUseCase = GetCatalogSyncStateUseCase(fakeSermonRepository),
            downloadOrUpdateCatalogUseCase = DownloadOrUpdateCatalogUseCase(fakeSermonRepository),
            deleteCatalogUseCase = DeleteCatalogUseCase(fakeSermonRepository)
        )
    }

    @Test
    fun initialState_hasExpectedDefaults() {
        val state = viewModel.uiState.value
        assertEquals(AppThemeMode.SYSTEM, state.themeMode)
        assertTrue(state.isSystemTheme)
    }

    @Test
    fun setThemeMode_updatesStateAndRepository() = runTest {
        viewModel.onEvent(SettingsUiEvent.SetThemeMode(AppThemeMode.DARK))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(AppThemeMode.DARK, state.themeMode)
            assertFalse(state.isSystemTheme)
            assertEquals(AppThemeMode.DARK, fakePreferencesRepository.themeModeFlow.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSystemTheme_updatesModeToSystemOrLight() = runTest {
        viewModel.onEvent(SettingsUiEvent.SetSystemTheme(false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.isSystemTheme)

        viewModel.onEvent(SettingsUiEvent.SetSystemTheme(true))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AppThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
        assertTrue(viewModel.uiState.value.isSystemTheme)
    }

    @Test
    fun deleteCatalog_callsRepositoryAndResetsState() = runTest {
        fakeSermonRepository.catalogSyncStateFlow.value = CatalogSyncState.Ready(sermonCount = 70000, generatedAt = "today", hasUpdate = false)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsUiEvent.DeleteCatalog)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeSermonRepository.deleteCatalogCalled)
        assertEquals(CatalogSyncState.NotDownloaded, viewModel.uiState.value.catalogState)
    }

    @Test
    fun clearCache_clearsFilesAndRefreshesCacheSize() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyCacheFile = File(context.cacheDir, "test_cache.tmp")
        dummyCacheFile.writeText("test data")

        viewModel.onEvent(SettingsUiEvent.ClearCache)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(dummyCacheFile.exists())
    }
}
