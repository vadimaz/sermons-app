package com.example.sermonindex.domain.usecase

import com.example.sermonindex.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CatalogPreferencesUseCasesTest {

    private lateinit var fakeRepository: FakeUserPreferencesRepository

    @Before
    fun setup() {
        fakeRepository = FakeUserPreferencesRepository()
    }

    @Test
    fun getAndSetAutoCatalogUpdateCheckEnabled_operatesCorrectly() = runTest {
        val getUseCase = GetAutoCatalogUpdateCheckEnabledUseCase(fakeRepository)
        val setUseCase = SetAutoCatalogUpdateCheckEnabledUseCase(fakeRepository)

        assertFalse(getUseCase().first())

        setUseCase(true)
        assertTrue(getUseCase().first())

        setUseCase(false)
        assertFalse(getUseCase().first())
    }

    @Test
    fun getAndSetLastCatalogUpdateCheckTimestamp_operatesCorrectly() = runTest {
        val getUseCase = GetLastCatalogUpdateCheckTimestampUseCase(fakeRepository)
        val setUseCase = SetLastCatalogUpdateCheckTimestampUseCase(fakeRepository)

        assertEquals(0L, getUseCase().first())

        val testTimestamp = 1729958400000L
        setUseCase(testTimestamp)
        assertEquals(testTimestamp, getUseCase().first())
    }
}
