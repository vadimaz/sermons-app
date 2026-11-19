package com.example.sermonindex.ui.speakers.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.sermonindex.domain.model.Sermon
import com.example.sermonindex.domain.usecase.GetSpeakerSermonsPageUseCase

/**
 * PagingSource for paginated sermons by a specific speaker.
 * Bridges Paging 3 with pure domain [GetSpeakerSermonsPageUseCase].
 */
class SpeakerSermonsPagingSource(
    private val getSpeakerSermonsPageUseCase: GetSpeakerSermonsPageUseCase,
    private val slug: String
) : PagingSource<Int, Sermon>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Sermon> {
        val page = params.key ?: 1
        return try {
            val result = getSpeakerSermonsPageUseCase(slug, page)
            result.fold(
                onSuccess = { paged ->
                    val nextKey = if (page < paged.totalPages && paged.sermons.isNotEmpty()) {
                        page + 1
                    } else {
                        null
                    }
                    val prevKey = if (page > 1) page - 1 else null
                    LoadResult.Page(
                        data = paged.sermons,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
                },
                onFailure = { error ->
                    LoadResult.Error(error)
                }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Sermon>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
