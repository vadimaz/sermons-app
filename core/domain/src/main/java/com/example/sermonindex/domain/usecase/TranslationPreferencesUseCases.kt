package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFirstTranslationUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<String> = preferencesRepository.firstTranslationId
}

class SetFirstTranslationUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(translationId: String) {
        preferencesRepository.setFirstTranslation(translationId)
    }
}

class GetSecondTranslationUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<String> = preferencesRepository.secondTranslationId
}

class SetSecondTranslationUseCase @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(translationId: String) {
        preferencesRepository.setSecondTranslation(translationId)
    }
}
