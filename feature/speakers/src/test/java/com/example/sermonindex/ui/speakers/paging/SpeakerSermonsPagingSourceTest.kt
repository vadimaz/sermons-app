package com.example.sermonindex.ui.speakers.paging

import androidx.paging.PagingSource
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.model.SpeakerPagedSermons
import com.example.sermonindex.domain.usecase.GetSpeakerSermonsPageUseCase
import com.example.sermonindex.testing.FakeSermonRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerSermonsPagingSourceTest {

    private fun createFakeSermon(id: String) = Sermon(
        id = id,
        title = "Title $id",
        speaker = "Speaker",
        speakerImage = "",
        duration = "30:00",
        hasAudio = true,
        mp3Url = "https://example.com/$id.mp3",
        cdnMp3Url = ""
    )

    @Test
    fun load_pageOne_returnsPageWithNextKey() = runTest {
        val sermons = listOf(createFakeSermon("1"), createFakeSermon("2"))
        val repo = FakeSermonRepository().apply {
            speakerSermonsPageProvider = { slug, page ->
                assertEquals("test-speaker", slug)
                assertEquals(1, page)
                Result.success(
                    SpeakerPagedSermons(
                        currentPage = 1,
                        totalPages = 3,
                        totalSermons = 100,
                        sermons = sermons
                    )
                )
            }
        }
        val useCase = GetSpeakerSermonsPageUseCase(repo)
        val pagingSource = SpeakerSermonsPagingSource(useCase, "test-speaker")

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 50,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(sermons, page.data)
        assertEquals(null, page.prevKey)
        assertEquals(2, page.nextKey)
    }

    @Test
    fun load_lastPage_returnsPageWithNullNextKey() = runTest {
        val sermons = listOf(createFakeSermon("3"))
        val repo = FakeSermonRepository().apply {
            speakerSermonsPageProvider = { _, page ->
                Result.success(
                    SpeakerPagedSermons(
                        currentPage = page,
                        totalPages = 3,
                        totalSermons = 100,
                        sermons = sermons
                    )
                )
            }
        }
        val useCase = GetSpeakerSermonsPageUseCase(repo)
        val pagingSource = SpeakerSermonsPagingSource(useCase, "test-speaker")

        val result = pagingSource.load(
            PagingSource.LoadParams.Append(
                key = 3,
                loadSize = 50,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(sermons, page.data)
        assertEquals(2, page.prevKey)
        assertEquals(null, page.nextKey)
    }

    @Test
    fun load_failure_returnsError() = runTest {
        val expectedError = RuntimeException("Network error")
        val repo = FakeSermonRepository().apply {
            speakerSermonsPageProvider = { _, _ -> Result.failure(expectedError) }
        }
        val useCase = GetSpeakerSermonsPageUseCase(repo)
        val pagingSource = SpeakerSermonsPagingSource(useCase, "test-speaker")

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = 1,
                loadSize = 50,
                placeholdersEnabled = false
            )
        )

        assertTrue(result is PagingSource.LoadResult.Error)
        val error = result as PagingSource.LoadResult.Error
        assertEquals(expectedError, error.throwable)
    }
}
