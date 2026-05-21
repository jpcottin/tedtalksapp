package com.jpcexample.tedtalks.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface TedTalksRepository {
    suspend fun fetchTalks(): Result<List<TalkItem>>
}

class DefaultTedTalksRepository(
    private val feedUrl: String = "https://feeds.feedburner.com/TedtalksHD",
    private val parser: RssFeedParser = RssFeedParser(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) : TedTalksRepository {

    override suspend fun fetchTalks(): Result<List<TalkItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(feedUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Empty response body")
            val talks = parser.parse(body.byteStream())
            response.close()
            talks
        }
    }
}
