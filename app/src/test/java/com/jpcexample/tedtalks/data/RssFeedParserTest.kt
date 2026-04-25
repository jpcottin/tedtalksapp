package com.jpcexample.tedtalks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class RssFeedParserTest {

    private val parser = RssFeedParser()

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
        assertEquals("00:10:58", talk.duration)
        assertEquals("https://example.com/image.jpg", talk.imageUrl)
        assertEquals("https://example.com/video.mp4", talk.videoUrl)
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
}
