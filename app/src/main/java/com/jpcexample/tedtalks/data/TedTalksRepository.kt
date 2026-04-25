package com.jpcexample.tedtalks.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TedTalksRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val feedUrl = "https://feeds.feedburner.com/TedtalksHD"

    suspend fun fetchTalks(): Result<List<TalkItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(feedUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Empty response body")
            val talks = RssFeedParser().parse(body.byteStream())
            response.close()
            talks
        }
    }
}
