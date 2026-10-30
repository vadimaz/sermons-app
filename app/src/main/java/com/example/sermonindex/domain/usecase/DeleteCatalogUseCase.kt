package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.repository.SermonRepository
import javax.inject.Inject

class DeleteCatalogUseCase @Inject constructor(
    private val repository: SermonRepository
) {
    suspend operator fun invoke() {
        repository.deleteCatalog()
    }
}
