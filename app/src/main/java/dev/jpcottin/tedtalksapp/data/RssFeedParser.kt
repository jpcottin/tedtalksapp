package dev.jpcottin.tedtalksapp.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RssFeedParser {

    fun parse(inputStream: InputStream): List<TalkItem> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        val talks = mutableListOf<TalkItem>()
        // The podcast feed names its publisher as the author of every episode
        // ("TED"); an item whose author is the channel's own carries no speaker.
        var channelAuthor = ""
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "item" -> talks.add(readItem(parser, channelAuthor))
                    "itunes:author" -> channelAuthor = readText(parser).trim()
                }
            }
            eventType = parser.next()
        }
        // The list keys its rows by id: an item with no guid, link or video gets its
        // position, and a guid the feed repeats keeps only its first item (the grid
        // throws on a key used twice).
        return talks
            .mapIndexed { i, t -> if (t.id.isEmpty()) t.copy(id = "item-$i") else t }
            .distinctBy { it.id }
    }

    private fun readItem(parser: XmlPullParser, channelAuthor: String): TalkItem {
        var title = ""
        var description = ""
        var link = ""
        var pubDate = ""
        var author = ""
        var duration = ""
        var imageUrl = ""
        var mediaUrl: String? = null
        var isAudio = false
        var guid = ""

        // The talk's own file: an mp4 wins over an mp3, whichever element carries it.
        fun offerMedia(type: String, url: String) {
            if (url.isEmpty()) return
            when (type.substringBefore(';').trim()) {
                "video/mp4" -> { mediaUrl = url; isAudio = false }
                "audio/mpeg", "audio/mp3" -> if (mediaUrl == null) { mediaUrl = url; isAudio = true }
            }
        }

        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name == "item") break
            if (event == XmlPullParser.END_DOCUMENT) throw truncatedFeed()
            if (event != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "title" -> title = readText(parser)
                "link" -> link = readText(parser)
                "description" -> description = readText(parser)
                "pubDate" -> pubDate = readText(parser)
                "guid" -> guid = readText(parser)
                "itunes:author" -> author = readText(parser)
                "itunes:duration" -> duration = readText(parser)
                "itunes:image" -> {
                    val href = parser.getAttributeValue(null, "href") ?: ""
                    if (imageUrl.isEmpty()) imageUrl = href
                    skip(parser)
                }
                "media:thumbnail" -> {
                    val url = parser.getAttributeValue(null, "url") ?: ""
                    if (imageUrl.isEmpty()) imageUrl = url
                    skip(parser)
                }
                "media:content", "enclosure" -> {
                    offerMedia(parser.getAttributeValue(null, "type") ?: "", parser.getAttributeValue(null, "url") ?: "")
                    skip(parser)
                }
                "media:group" -> { /* children processed naturally by the outer loop */ }
                else -> skip(parser)
            }
        }

        // TED has published both "Speaker | Title" and, since 2026, "Title | Speaker".
        // When the author names the speaker, drop whichever end matches it. Without
        // one (the podcast feed credits every episode to the publisher), the title
        // is read in today's order, "Title | Speaker"; a lone title names no speaker,
        // or it would be shown twice, as title and as speaker.
        val parts = title.split(" | ").map { it.trim() }
        val speakerAuthor = author.trim().takeUnless { it.equals(channelAuthor, ignoreCase = true) }.orEmpty()
        var cleanSpeaker = speakerAuthor.ifEmpty { if (parts.size >= 2) parts.last() else "" }
        var cleanTitle = when {
            parts.size < 2 -> title.trim()
            parts.last().equals(cleanSpeaker, ignoreCase = true) -> parts.dropLast(1).joinToString(" | ")
            parts.first().equals(cleanSpeaker, ignoreCase = true) -> parts.drop(1).joinToString(" | ")
            else -> parts[1]
        }
        // "Kaeli Swift (re-release)": the note belongs to the talk, not to the speaker.
        SPEAKER_NOTE.find(cleanSpeaker)?.let { m ->
            cleanSpeaker = m.groupValues[1]
            cleanTitle = "$cleanTitle ${m.groupValues[2]}"
        }

        return TalkItem(
            id = guid.ifEmpty { link }.ifEmpty { mediaUrl ?: "" },
            title = cleanTitle,
            speaker = cleanSpeaker,
            description = stripHtml(description),
            pubDate = formatDate(pubDate),
            duration = formatDuration(duration),
            imageUrl = imageUrl,
            link = link,
            mediaUrl = mediaUrl,
            isAudio = isAudio,
            publishedMillis = parseDate(pubDate)?.time ?: 0L,
        )
    }

    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        while (true) {
            when (parser.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(parser.text)
                XmlPullParser.END_TAG -> break
                else -> break
            }
        }
        return sb.toString()
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_DOCUMENT -> throw truncatedFeed()
            }
        }
    }

    // A feed cut off mid-item never yields the closing tag; without this the
    // loops above would spin on END_DOCUMENT forever.
    private fun truncatedFeed() = XmlPullParserException("Feed ended inside an item")

    // The feed sends "00:10:58"; show "10:58", or "1:02:03" past the hour.
    private fun formatDuration(raw: String): String {
        val trimmed = raw.trim()
        val parts = trimmed.split(":")
        val hours = parts[0].toIntOrNull()
        if (parts.size != 3 || hours == null) return trimmed
        return if (hours == 0) "${parts[1]}:${parts[2]}" else "$hours:${parts[1]}:${parts[2]}"
    }

    private fun stripHtml(html: String): String {
        // One pass over the entities: replacing "&amp;" first would re-create "&lt;"
        // from "&amp;lt;" and decode it a second time.
        return html.replace(Regex("<[^>]*>"), "")
            .replace(ENTITY) { m ->
                val body = m.groupValues[1]
                when {
                    body.startsWith("#x", ignoreCase = true) -> body.substring(2).toIntOrNull(16)?.let(::codePoint) ?: m.value
                    body.startsWith("#") -> body.substring(1).toIntOrNull()?.let(::codePoint) ?: m.value
                    else -> NAMED_ENTITIES[body] ?: m.value
                }
            }
            .trim()
    }

    private fun codePoint(code: Int): String =
        if (Character.isValidCodePoint(code)) String(Character.toChars(code)) else "\uFFFD"

    private companion object {
        val SPEAKER_NOTE = Regex("""^(.*\S)\s+(\([^()]*\))$""")
        val ENTITY = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")
        val NAMED_ENTITIES = mapOf(
            "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
            "nbsp" to " ", "hellip" to "\u2026", "mdash" to "\u2014", "ndash" to "\u2013",
            "rsquo" to "\u2019", "lsquo" to "\u2018", "rdquo" to "\u201D", "ldquo" to "\u201C",
        )
    }

    // The feed's RFC 822 date, shown the way the device's locale writes a date:
    // "May 21, 2025" in the US, "21 mai 2025" in France. A fixed "MMM d, yyyy"
    // pattern used to put a translated month name in the American order.
    private fun formatDate(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        val date = parseDate(dateStr) ?: return dateStr
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(date)
    }

    private fun parseDate(dateStr: String): Date? = try {
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(dateStr.trim())
    } catch (_: Exception) {
        null
    }
}
