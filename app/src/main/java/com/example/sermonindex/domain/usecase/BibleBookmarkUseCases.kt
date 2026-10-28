package com.example.sermonindex.domain.usecase

import com.example.sermonindex.domain.model.BibleBookmark
import com.example.sermonindex.domain.repository.SermonRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllBibleBookmarksUseCase @Inject constructor(
    private val repository: SermonRepository
) {
    operator fun invoke(): Flow<List<BibleBookmark>> = repository.bibleBookmarks
}

class GetBibleBookmarksForChapterUseCase @Inject constructor(
    private val repository: SermonRepository
) {
    operator fun invoke(translationId: String, bookId: String, chapter: Int): Flow<List<BibleBookmark>> =
        repository.getBibleBookmarksForChapter(translationId, bookId, chapter)
}

class SaveBibleBookmarkUseCase @Inject constructor(
    private val repository: SermonRepository
) {
    suspend operator fun invoke(bookmark: BibleBookmark) = repository.saveBibleBookmark(bookmark)
}

class DeleteBibleBookmarkUseCase @Inject constructor(
    private val repository: SermonRepository
) {
    suspend operator fun invoke(id: String) = repository.deleteBibleBookmark(id)
}

class DeleteBibleBookmarkForVerseUseCase @Inject constructor(
    private val repository: SermonRepository
) {
    suspend operator fun invoke(translationId: String, bookId: String, chapter: Int, verse: Int) =
        repository.deleteBibleBookmarkForVerse(translationId, bookId, chapter, verse)
}
