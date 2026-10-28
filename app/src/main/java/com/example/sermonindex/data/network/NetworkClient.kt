package com.example.sermonindex.data.network

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkClient {

    private const val BASE_URL = "https://api.sermonindex.net/v2/"
    private const val CACHE_SIZE = 50L * 1024 * 1024 // 50 MB

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun createOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "sermonindex_http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(Http404EmptyInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createApiService(
        okHttpClient: OkHttpClient
    ): SermonIndexApiService {
        val contentType = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create<SermonIndexApiService>()
    }

    fun createApiService(context: Context): SermonIndexApiService {
        return createApiService(createOkHttpClient(context))
    }
}
