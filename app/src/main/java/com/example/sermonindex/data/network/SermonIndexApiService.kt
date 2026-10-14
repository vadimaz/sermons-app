package com.example.sermonindex.data.network

import com.example.sermonindex.data.model.PopularSermonsResponse
import com.example.sermonindex.data.model.ScriptureBooksResponse
import com.example.sermonindex.data.model.ScriptureChapterDto
import com.example.sermonindex.data.model.ScriptureVerseResponse
import com.example.sermonindex.data.model.SermonDetailDto
import com.example.sermonindex.data.model.SpeakerDto
import com.example.sermonindex.data.model.SpeakerPagedDto
import com.example.sermonindex.data.model.SpeakersResponse
import com.example.sermonindex.data.model.TopicDetailDto
import com.example.sermonindex.data.model.TopicsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface SermonIndexApiService {

    @GET("sermons/popular")
    suspend fun getPopularSermons(): PopularSermonsResponse

    @GET("scripture")
    suspend fun getScriptureBooks(): ScriptureBooksResponse

    @GET("scripture/{bookId}/{chapter}")
    suspend fun getScriptureChapter(
        @Path("bookId") bookId: String,
        @Path("chapter") chapter: Int
    ): ScriptureChapterDto

    @GET("scripture/{bookId}/{chapter}/{verse}")
    suspend fun getScriptureVerse(
        @Path("bookId") bookId: String,
        @Path("chapter") chapter: Int,
        @Path("verse") verse: Int
    ): ScriptureVerseResponse

    @GET("speakers")
    suspend fun getSpeakers(): SpeakersResponse

    @GET("speakers/{slug}")
    suspend fun getSpeakerDetails(
        @Path("slug") slug: String
    ): SpeakerDto

    @GET("speakers/{slug}/page/{page}")
    suspend fun getSpeakerSermonsPage(
        @Path("slug") slug: String,
        @Path("page") page: Int
    ): SpeakerPagedDto

    @GET("topics")
    suspend fun getTopics(): TopicsResponse

    @GET("topics/{slug}")
    suspend fun getTopicDetails(
        @Path("slug") slug: String
    ): TopicDetailDto

    @GET("sermons/{id}")
    suspend fun getSermonDetails(
        @Path("id") id: String
    ): SermonDetailDto
}
