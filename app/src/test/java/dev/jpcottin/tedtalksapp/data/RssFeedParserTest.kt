package dev.jpcottin.tedtalksapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.After
import org.junit.Before
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import java.util.TimeZone

class RssFeedParserTest {

    private val parser = RssFeedParser()

    // formatDate renders in the JVM's default locale and time zone; pin both so
    // "May 21, 2025" holds on a French machine or east of UTC+9.
    private lateinit var defaultLocale: Locale
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun pinLocaleAndTimeZone() {
        defaultLocale = Locale.getDefault()
        defaultTimeZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreLocaleAndTimeZone() {
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultTimeZone)
    }

    private fun parseString(xml: String): List<TalkItem> {
        return parser.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun parse_validXml_returnsCorrectTalkItems() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("sample_feed.xml")
        assertNotNull("sample_feed.xml not found", inputStream)
        
        val talks = parser.parse(inputStream!!)
        assertEquals(1, talks.size)
        val talk = talks[0]
        assertEquals("12345", talk.id)
        assertEquals("This is what the future of media looks like", talk.title)
        assertEquals("Hamish McKenzie", talk.speaker)
        assertEquals("May 21, 2025", talk.pubDate)
        assertEquals("10:58", talk.duration)
        assertEquals("https://example.com/image.jpg", talk.imageUrl)
        assertEquals("https://example.com/video.mp4", talk.videoUrl)
    }

    private fun durationOf(raw: String): String = parseString(
        """
            <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" version="2.0">
                <channel>
                    <item>
                        <title>Some Title</title>
                        <itunes:duration>$raw</itunes:duration>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
    ).single().duration

    @Test
    fun parse_duration_dropsZeroHours() {
        assertEquals("10:58", durationOf("00:10:58"))
        assertEquals("09:46", durationOf("00:09:46"))
    }

    @Test
    fun parse_duration_keepsNonZeroHoursWithoutPadding() {
        assertEquals("1:02:03", durationOf("01:02:03"))
    }

    @Test
    fun parse_duration_leavesOtherFormatsAlone() {
        assertEquals("29:29", durationOf("29:29"))
        assertEquals("658", durationOf("658"))
        assertEquals("", durationOf(""))
    }

    @Test(timeout = 5_000)
    fun parse_feedTruncatedInsideItem_throwsInsteadOfHanging() {
        val truncated = listOf(
            "<rss><channel><item><title>cut off",
            "<rss><channel><item><title>Done</title><media:group><media:content",
            "<rss><channel><item><title>Done</title><unknown><child>",
        )
        for (xml in truncated) {
            val result = runCatching { parseString(xml) }
            assertTrue("Expected failure for '$xml', got $result", result.isFailure)
        }
    }

    private fun itemWith(media: String): String = """
        <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" xmlns:media="http://search.yahoo.com/mrss/">
          <channel>
            <item>
              <title>Talk</title>
              <guid>1</guid>
              $media
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun parse_image_firstImageElementWins() {
        val thumbnailFirst = parseString(itemWith("""
            <media:thumbnail url="https://example.com/thumb.jpg"/>
            <itunes:image href="https://example.com/itunes.jpg"/>
        """)).single()
        assertEquals("https://example.com/thumb.jpg", thumbnailFirst.imageUrl)

        val itunesFirst = parseString(itemWith("""
            <itunes:image href="https://example.com/itunes.jpg"/>
            <media:thumbnail url="https://example.com/thumb.jpg"/>
        """)).single()
        assertEquals("https://example.com/itunes.jpg", itunesFirst.imageUrl)
    }

    @Test
    fun parse_video_onlyMp4MediaContentWithAUrlIsUsed() {
        val talk = parseString(itemWith("""
            <media:content url="https://example.com/audio.mp3" type="audio/mpeg"/>
            <media:content type="video/mp4"/>
            <media:content url="https://example.com/talk.mp4" type="video/mp4"/>
        """)).single()
        assertEquals("https://example.com/talk.mp4", talk.videoUrl)

        val none = parseString(itemWith("""
            <media:content url="https://example.com/audio.mp3" type="audio/mpeg"/>
        """)).single()
        assertTrue(none.videoUrl.isNullOrEmpty())
    }

    @Test
    fun parse_mediaGroup_childrenAreRead() {
        val talk = parseString(itemWith("""
            <media:group>
              <media:thumbnail url="https://example.com/thumb.jpg"/>
              <media:content url="https://example.com/talk.mp4" type="video/mp4"/>
            </media:group>
        """)).single()
        assertEquals("https://example.com/thumb.jpg", talk.imageUrl)
        assertEquals("https://example.com/talk.mp4", talk.videoUrl)
    }

    @Test
    fun parse_emptyFeed_returnsEmptyList() {
        val xml = """
            <rss version="2.0">
                <channel>
                    <title>Empty Feed</title>
                </channel>
            </rss>
        """.trimIndent()
        val talks = parseString(xml)
        assertTrue(talks.isEmpty())
    }

    @Test
    fun parse_missingGuid_usesLinkAsId() {
        val xml = """
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Some Title</title>
                        <link>https://ted.com/talks/123</link>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
        val talks = parseString(xml)
        assertEquals(1, talks.size)
        assertEquals("https://ted.com/talks/123", talks[0].id)
    }

    @Test
    fun parse_htmlDescription_stripsHtmlTags() {
        val xml = """
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Title</title>
                        <description><![CDATA[<p>This is a <b>bold</b> description.</p> <br/> Also with some &amp; stuff.]]></description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
        val talks = parseString(xml)
        assertEquals(1, talks.size)
        // Note: The parser's stripHtml uses regex that strips tags and manually replaces some entities.
        // It might not be a perfect HTML parser but it should strip `<p>` and `<b>` and `<br/>`.
        assertEquals("This is a bold description.  Also with some & stuff.", talks[0].description)
    }

    @Test
    fun parse_invalidDate_returnsOriginalString() {
        val xml = """
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Title</title>
                        <pubDate>Invalid Date Format 2026</pubDate>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
        val talks = parseString(xml)
        assertEquals(1, talks.size)
        assertEquals("Invalid Date Format 2026", talks[0].pubDate)
    }

    @Test
    fun parse_missingAuthor_splitsTitleProperly() {
        val xml = """
            <rss version="2.0">
                <channel>
                    <item>
                        <title>Bill Gates | The future of computing</title>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
        val talks = parseString(xml)
        assertEquals(1, talks.size)
        assertEquals("Bill Gates", talks[0].speaker)
        assertEquals("The future of computing", talks[0].title)
    }

    @Test
    fun parse_titleThenSpeakerFormat_usesAuthorToSplit() {
        // Current TED feed format (since 2026): "Title | Speaker".
        val talks = parseString(
            """
            <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
                <channel>
                    <item>
                        <title>This is what the future of media looks like | Hamish McKenzie</title>
                        <itunes:author>Hamish McKenzie</itunes:author>
                        <link>https://example.com</link>
                    </item>
                </channel>
            </rss>
            """.trimIndent()
        )
        assertEquals("Hamish McKenzie", talks[0].speaker)
        assertEquals("This is what the future of media looks like", talks[0].title)
    }

    @Test
    fun parse_titleContainingSeparator_keepsWholeTitle() {
        val talks = parseString(
            """
            <rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
                <channel>
                    <item>
                        <title>Work | Life | Balance | Jane Doe</title>
                        <itunes:author>Jane Doe</itunes:author>
                        <link>https://example.com</link>
                    </item>
                </channel>
            </rss>
            """.trimIndent()
        )
        assertEquals("Jane Doe", talks[0].speaker)
        assertEquals("Work | Life | Balance", talks[0].title)
    }

    @Test
    fun parse_noVideoContent_returnsNullVideoUrl() {
        val xml = """
            <rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
                <channel>
                    <item>
                        <title>Title</title>
                        <media:content url="https://example.com/audio.mp3" type="audio/mp3"/>
                    </item>
                </channel>
            </rss>
        """.trimIndent()
        val talks = parseString(xml)
        assertEquals(1, talks.size)
        assertNull(talks[0].videoUrl)
    }

    private fun feed(items: String) = "<rss><channel>$items</channel></rss>"

    @Test
    fun parse_itemsWithoutGuidOrLink_getDistinctIds() {
        val talks = parseString(feed("<item><title>A</title></item><item><title>B</title></item>"))
        assertEquals(2, talks.size)
        assertEquals(2, talks.map { it.id }.toSet().size)
        assertTrue(talks.none { it.id.isEmpty() })
    }

    @Test
    fun parse_repeatedGuid_keepsOneItem() {
        val talks = parseString(feed("<item><guid>g1</guid><title>A</title></item><item><guid>g1</guid><title>B</title></item>"))
        assertEquals(1, talks.size)
        assertEquals("A", talks[0].title)
    }

    @Test
    fun parse_noAuthorNoSeparator_leavesSpeakerEmpty() {
        val talk = parseString(feed("<item><title>Some Title</title></item>")).single()
        assertEquals("Some Title", talk.title)
        assertEquals("", talk.speaker)
    }

    @Test
    fun parse_htmlDescription_decodesNumericAndNamedEntities() {
        val talk = parseString(feed("<item><title>T</title><description><![CDATA[<p>Tom &amp; Jerry&#39;s &hellip; &#x27;fine&#x27;</p>]]></description></item>")).single()
        assertEquals("Tom & Jerry's \u2026 'fine'", talk.description)
    }

    @Test
    fun parse_htmlDescription_doesNotDoubleDecode() {
        val talk = parseString(feed("<item><title>T</title><description><![CDATA[&amp;lt;b&amp;gt;]]></description></item>")).single()
        assertEquals("&lt;b&gt;", talk.description)
    }
}
