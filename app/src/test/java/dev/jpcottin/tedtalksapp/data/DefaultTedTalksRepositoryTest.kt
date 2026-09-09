package dev.jpcottin.tedtalksapp.data

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest

class DefaultTedTalksRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultTedTalksRepository

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        repository = DefaultTedTalksRepository(
            feedUrl = server.url("/feed").toString(),
            client = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build(),
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
}
