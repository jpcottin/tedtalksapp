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

interface TedTalksRepository {
    suspend fun fetchTalks(): Result<List<TalkItem>>
}

class DefaultTedTalksRepository(
    private val feedUrl: String = "https://feeds.feedburner.com/TedtalksHD",
    private val parser: RssFeedParser = RssFeedParser(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)   // a server dripping bytes cannot hold a read open forever
        .followRedirects(true)
        .build(),
) : TedTalksRepository {

    override suspend fun fetchTalks(): Result<List<TalkItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(feedUrl).build()
            client.newCall(request).await().use { response ->
                if (!response.isSuccessful) throw FeedException.Http(response.code)
                val body = response.body ?: throw FeedException.Malformed(IOException("Empty response body"))
                Result.success(parser.parse(body.byteStream()))
            }
        } catch (e: FeedException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(FeedException.Network(e))
        } catch (e: XmlPullParserException) {
            Result.failure(FeedException.Malformed(e))
        }
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
