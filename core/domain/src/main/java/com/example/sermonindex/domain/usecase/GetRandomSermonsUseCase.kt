package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.repository.SermonRepository
import javax.inject.Inject

class GetRandomSermonsUseCase @Inject constructor(
    private val repository: SermonRepository
) {
    suspend operator fun invoke(shard: Int? = null): Result<List<Sermon>> =
        repository.getRandomSermons(shard)
}
