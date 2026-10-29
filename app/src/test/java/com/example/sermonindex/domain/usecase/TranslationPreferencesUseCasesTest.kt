package com.example.sermonindex.domain.usecase

import com.example.sermonindex.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TranslationPreferencesUseCasesTest {

    private lateinit var fakeRepository: FakeUserPreferencesRepository

    @Before
    fun setup() {
        fakeRepository = FakeUserPreferencesRepository()
    }

    @Test
    fun getAndSetFirstTranslation_operatesCorrectly() = runTest {
        val getUseCase = GetFirstTranslationUseCase(fakeRepository)
        val setUseCase = SetFirstTranslationUseCase(fakeRepository)

        assertEquals("BSB", getUseCase().first())

        setUseCase("eng_kjv")
        assertEquals("eng_kjv", getUseCase().first())
    }

    @Test
    fun getAndSetSecondTranslation_operatesCorrectly() = runTest {
        val getUseCase = GetSecondTranslationUseCase(fakeRepository)
        val setUseCase = SetSecondTranslationUseCase(fakeRepository)

        assertEquals("rus_syn", getUseCase().first())

        setUseCase("spa_r09")
        assertEquals("spa_r09", getUseCase().first())
    }
}
