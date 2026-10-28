package com.example.sermonindex.data.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Intercepts HTTP 404 responses returned as HTML by the SermonIndex CDN for empty scriptures/slugs,
 * converting them into an empty JSON object response to enable graceful fallback to default DTO values.
 */
class Http404EmptyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 404) {
            response.body.close()
            val emptyJson = "{}"
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = emptyJson.toResponseBody(mediaType)
            return response.newBuilder()
                .code(200)
                .message("OK (Converted 404 to Empty)")
                .body(body)
                .build()
        }
        return response
    }
}
