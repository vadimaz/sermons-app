package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAutoCatalogUpdateCheckEnabledUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = preferencesRepository.isAutoCatalogUpdateCheckEnabled
}

class SetAutoCatalogUpdateCheckEnabledUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        preferencesRepository.setAutoCatalogUpdateCheckEnabled(enabled)
    }
}

class GetLastCatalogUpdateCheckTimestampUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<Long> = preferencesRepository.lastCatalogUpdateCheckTimestamp
}

class SetLastCatalogUpdateCheckTimestampUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(timestamp: Long) {
        preferencesRepository.setLastCatalogUpdateCheckTimestamp(timestamp)
    }
}
