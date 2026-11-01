package dev.jpcottin.tedtalksapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.TimeUnit
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

interface TedTalksRepository {
    /**
     * The talks to list: the video talks shipped with the app and, with [includeAudio],
     * the live feed's audio talks as well, newest first. Without a bundled feed the live
     * feed is all there is, audio or not.
     */
    suspend fun fetchTalks(includeAudio: Boolean = false): Result<List<TalkItem>>
}

class DefaultTedTalksRepository(
    private val feedUrl: String = FEED_URL,
    private val parser: RssFeedParser = RssFeedParser(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)   // a server dripping bytes cannot hold a read open forever
        .followRedirects(true)
        .build(),
    /**
     * Opens the feed shipped in the app: the HD video talks, kept since TED's feeds
     * turned audio-only. Null when the app ships none.
     */
    private val bundledFeed: (() -> InputStream)? = null,
) : TedTalksRepository {

    // Parsed once: the bundled feed never changes while the app runs.
    @Volatile private var bundledTalks: List<TalkItem>? = null

    override suspend fun fetchTalks(includeAudio: Boolean): Result<List<TalkItem>> = withContext(Dispatchers.IO) {
        try {
            val bundled = bundledFeed?.let { open -> bundledTalks ?: open().use(parser::parse).also { bundledTalks = it } }
            if (bundled != null && !includeAudio) return@withContext Result.success(bundled)
            val live = fetchLive()
            // Two sources: by date, the newest first. One source keeps its feed's order.
            Result.success(if (bundled == null) live else (bundled + live).sortedByDescending { it.publishedMillis })
        } catch (e: FeedException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(FeedException.Network(e))
        } catch (e: XmlPullParserException) {
            Result.failure(FeedException.Malformed(e))
        }
    }

    private suspend fun fetchLive(): List<TalkItem> {
        val request = Request.Builder().url(feedUrl).build()
        return client.newCall(request).await().use { response ->
            if (!response.isSuccessful) throw FeedException.Http(response.code)
            val body = response.body ?: throw FeedException.Malformed(IOException("Empty response body"))
            parser.parse(body.byteStream())
        }
    }

    companion object {
        const val FEED_URL = "https://feeds.feedburner.com/TedtalksHD"
        /** The HD video feed as it last stood, in the app's assets. */
        const val BUNDLED_VIDEO_FEED = "ted_talks_hd.xml"
    }
}

/** The call as a suspension that cancels with its coroutine, instead of a blocked IO thread. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) { cont.resume(response) }
        override fun onFailure(call: Call, e: IOException) { if (!cont.isCancelled) cont.resumeWithException(e) }
    })
    cont.invokeOnCancellation { cancel() }
}

/**
 * Why the feed could not be loaded, with a message fit for the screen: the raw
 * exception text ("Unable to resolve host …", "unexpected type (position:END_DOCUMENT …)")
 * used to be shown as is.
 */
sealed class FeedException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val code: Int) : FeedException("HTTP $code: the feed's server could not serve it")
    class Network(cause: Throwable) : FeedException("Check your connection", cause)
    class Malformed(cause: Throwable) : FeedException("The feed could not be read", cause)
}
