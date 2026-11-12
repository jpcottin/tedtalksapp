package dev.jpcottin.tedtalksapp.data

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class DefaultTedTalksRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var repository: DefaultTedTalksRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
        repository = DefaultTedTalksRepository(
            feedUrl = server.url("/feed").toString(),
            client = client,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchTalks_success_returnsParsedTalks() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SAMPLE_FEED))

        val result = repository.fetchTalks()

        assertTrue("Expected success, got $result", result.isSuccess)
        val talks = result.getOrThrow()
        assertEquals(1, talks.size)
        assertEquals("12345", talks[0].id)
        assertEquals("This is what the future of media looks like", talks[0].title)
        assertEquals("Hamish McKenzie", talks[0].speaker)
    }

    private val bundledVideoFeed = """
        <rss xmlns:media="http://search.yahoo.com/mrss/" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
          <channel>
            <item><title>Old video | Vera</title><guid>v-old</guid><pubDate>Tue, 15 Oct 2024 10:00:00 +0000</pubDate>
              <media:content url="https://example.com/old.mp4" type="video/mp4"/></item>
            <item><title>New video | Victor</title><guid>v-new</guid><pubDate>Wed, 21 May 2025 10:00:00 +0000</pubDate>
              <media:content url="https://example.com/new.mp4" type="video/mp4"/></item>
          </channel>
        </rss>
    """.trimIndent()

    private val audioFeed = """
        <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
          <channel>
            <itunes:author>TED</itunes:author>
            <item><title>Newest audio | Ada</title><guid>a-new</guid><pubDate>Sun, 01 Nov 2026 07:00:00 GMT</pubDate>
              <enclosure url="https://example.com/new.mp3" type="audio/mpeg"/></item>
            <item><title>Between the videos | Alan</title><guid>a-mid</guid><pubDate>Wed, 01 Jan 2025 07:00:00 GMT</pubDate>
              <enclosure url="https://example.com/mid.mp3" type="audio/mpeg"/></item>
          </channel>
        </rss>
    """.trimIndent()

    private fun bundledRepository() = DefaultTedTalksRepository(
        feedUrl = server.url("/feed").toString(),
        client = client,
        bundledFeed = { bundledVideoFeed.byteInputStream() },
    )

    @Test
    fun fetchTalks_withABundledFeed_listsItsVideosWithoutTouchingTheNetwork() = runTest {
        val repo = bundledRepository()

        val talks = repo.fetchTalks().getOrThrow()

        assertEquals(listOf("v-old", "v-new"), talks.map { it.id })   // the feed's own order
        assertTrue(talks.none { it.isAudio })
        assertEquals(0, server.requestCount)
    }

    @Test
    fun fetchTalks_withAudio_addsTheLiveTalksNewestFirst() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(audioFeed))
        val repo = bundledRepository()

        val talks = repo.fetchTalks(includeAudio = true).getOrThrow()

        assertEquals(listOf("a-new", "v-new", "a-mid", "v-old"), talks.map { it.id })
        assertEquals(listOf(true, false, true, false), talks.map { it.isAudio })
        assertEquals("Ada", talks[0].speaker)
    }

    @Test
    fun fetchTalks_withAudio_aTalkInBothFeedsListsOnceAsItsVideo() = runTest {
        // The live feed republishes a bundled talk as audio under the same guid, and
        // both feeds have an item with nothing to key it by, which the parser keys by
        // position: the grid would meet each id twice and throw.
        val liveFeed = """
            <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <itunes:author>TED</itunes:author>
                <item><title>New video, again as audio | Victor</title><guid>v-new</guid><pubDate>Sun, 01 Nov 2026 07:00:00 GMT</pubDate>
                  <enclosure url="https://example.com/new.mp3" type="audio/mpeg"/></item>
                <item><title>Between the videos | Alan</title><guid>a-mid</guid><pubDate>Wed, 01 Jan 2025 07:00:00 GMT</pubDate>
                  <enclosure url="https://example.com/mid.mp3" type="audio/mpeg"/></item>
                <item><title>Nothing to key by | Nobody</title><description>No guid, link or media</description></item>
              </channel>
            </rss>
        """.trimIndent()
        val bundledWithAnUnkeyedItem = bundledVideoFeed.replace(
            "</channel>",
            "<item><title>Also nothing to key by | Nobody</title></item></channel>",
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(liveFeed))
        val repo = DefaultTedTalksRepository(
            feedUrl = server.url("/feed").toString(),
            client = client,
            bundledFeed = { bundledWithAnUnkeyedItem.byteInputStream() },
        )

        val talks = repo.fetchTalks(includeAudio = true).getOrThrow()

        assertEquals(talks.map { it.id }.distinct(), talks.map { it.id })
        val newest = talks.single { it.id == "v-new" }
        assertEquals("https://example.com/new.mp4", newest.mediaUrl)   // the bundled video, not the audio copy
        assertEquals(listOf("a-mid"), talks.filter { it.isAudio }.map { it.id })
        // Third in each feed, so both parse to "item-2": the bundled one stays.
        assertEquals("Also nothing to key by", talks.single { it.id == "item-2" }.title)
    }

    @Test
    fun fetchTalks_withAudio_parsesTheBundledFeedOnce() = runTest {
        var opened = 0
        val repo = DefaultTedTalksRepository(
            feedUrl = server.url("/feed").toString(),
            client = client,
            bundledFeed = { opened++; bundledVideoFeed.byteInputStream() },
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(audioFeed))
        server.enqueue(MockResponse().setResponseCode(200).setBody(audioFeed))

        repo.fetchTalks()
        repo.fetchTalks(includeAudio = true)
        repo.fetchTalks(includeAudio = true)

        assertEquals(1, opened)
    }

    @Test
    fun fetchTalks_withAudio_liveFailureIsAFailure() = runTest {
        // The caller keeps whatever list it has and reports; a silent fall back to the
        // videos would look like the audio talks had vanished.
        server.enqueue(MockResponse().setResponseCode(503))
        val repo = bundledRepository()

        val result = repo.fetchTalks(includeAudio = true)

        assertTrue(result.isFailure)
        assertEquals("HTTP 503: the feed's server could not serve it", result.exceptionOrNull()?.message)
    }

    @Test
    fun fetchTalks_withoutABundledFeed_isTheLiveFeedWhateverTheChoice() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(audioFeed))

        val talks = repository.fetchTalks(includeAudio = false).getOrThrow()

        assertEquals(listOf("a-new", "a-mid"), talks.map { it.id })
    }

    @Test
    fun fetchTalks_cancelled_abortsTheCallPromptly() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val job = launch(Dispatchers.IO) { repository.fetchTalks() }
        server.takeRequest(2, TimeUnit.SECONDS)            // the request is on the wire

        val started = System.nanoTime()
        job.cancelAndJoin()

        assertTrue("cancel took ${(System.nanoTime() - started) / 1_000_000} ms",
            System.nanoTime() - started < 1_000_000_000L)
        assertEquals(0, client.dispatcher.runningCallsCount())
    }

    @Test
    fun fetchTalks_httpError_returnsFailureWithStatusCode() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.fetchTalks()

        assertTrue("Expected failure, got $result", result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(
            "Expected message to contain HTTP status code, got '$message'",
            message.contains("500"),
        )
    }

    @Test
    fun fetchTalks_notFound_returnsFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val result = repository.fetchTalks()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("404"))
    }

    @Test
    fun fetchTalks_malformedXml_returnsFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<<not xml>>"))

        val result = repository.fetchTalks()

        assertTrue("Expected failure for malformed XML, got $result", result.isFailure)
    }

    // An unclosed response keeps its connection checked out of the pool.
    private fun assertNoLeakedConnection() {
        val pool = client.connectionPool
        assertEquals(pool.connectionCount(), pool.idleConnectionCount())
    }

    @Test
    fun fetchTalks_httpError_closesResponse() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("server exploded"))

        repository.fetchTalks()

        assertNoLeakedConnection()
    }

    @Test
    fun fetchTalks_malformedXml_closesResponse() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<rss><channel><item><title>cut off"))

        repository.fetchTalks()

        assertNoLeakedConnection()
    }

    @Test
    fun fetchTalks_emptyFeed_returnsEmptyList() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<rss version=\"2.0\"><channel></channel></rss>")
        )

        val result = repository.fetchTalks()

        assertTrue(result.isSuccess)
        assertEquals(emptyList<TalkItem>(), result.getOrThrow())
    }

    @Test
    fun fetchTalks_hitsConfiguredFeedUrl() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SAMPLE_FEED))

        repository.fetchTalks()

        val request = server.takeRequest()
        assertEquals("/feed", request.path)
        assertEquals("GET", request.method)
    }

    companion object {
        private val SAMPLE_FEED = """
            <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
                <channel>
                    <title>TEDTalks (video)</title>
                    <item>
                        <title>Hamish McKenzie | This is what the future of media looks like</title>
                        <description>Hamish McKenzie discusses the future of media.</description>
                        <link>https://www.ted.com/talks/hamish_mckenzie_this_is_what_the_future_of_media_looks_like</link>
                        <pubDate>Wed, 21 May 2025 15:00:00 +0000</pubDate>
                        <guid isPermaLink="false">12345</guid>
                        <itunes:author>Hamish McKenzie</itunes:author>
                        <itunes:duration>00:10:58</itunes:duration>
                        <media:thumbnail url="https://example.com/image.jpg"/>
                        <media:content url="https://example.com/video.mp4" type="video/mp4"/>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
    }

    @Test
    fun fetchTalks_connectionDropped_isANetworkFailure() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val result = repository.fetchTalks()

        assertTrue(result.exceptionOrNull() is FeedException.Network)
        assertEquals("Check your connection", result.exceptionOrNull()?.message)
    }

    @Test
    fun fetchTalks_serverError_isAnHttpFailureWithTheCode() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = repository.fetchTalks()

        val e = result.exceptionOrNull()
        assertTrue(e is FeedException.Http)
        assertEquals(503, (e as FeedException.Http).code)
    }

    @Test
    fun fetchTalks_malformedXml_isAMalformedFailure() = runTest {
        server.enqueue(MockResponse().setBody("<rss><channel><item><title>Oops"))

        val result = repository.fetchTalks()

        assertTrue(result.exceptionOrNull() is FeedException.Malformed)
    }
}
