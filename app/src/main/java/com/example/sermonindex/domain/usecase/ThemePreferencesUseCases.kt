package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.AppThemeMode
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAppThemeModeUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<AppThemeMode> = preferencesRepository.themeMode
}

class SetAppThemeModeUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(mode: AppThemeMode) {
        preferencesRepository.setThemeMode(mode)
    }
}
