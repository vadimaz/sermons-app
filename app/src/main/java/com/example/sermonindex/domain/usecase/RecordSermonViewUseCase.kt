package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.repository.SermonRepository
import javax.inject.Inject

class RecordSermonViewUseCase @Inject constructor(
    private val repository: SermonRepository
) {
    suspend operator fun invoke(
        sermonId: String,
        title: String,
        speaker: String,
        speakerImage: String
    ) {
        repository.recordSermonView(sermonId, title, speaker, speakerImage)
    }
}
