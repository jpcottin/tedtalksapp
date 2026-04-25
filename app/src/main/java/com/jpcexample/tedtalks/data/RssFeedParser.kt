package com.jpcexample.tedtalks.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class RssFeedParser {

    fun parse(inputStream: InputStream): List<TalkItem> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        val talks = mutableListOf<TalkItem>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                talks.add(readItem(parser))
            }
            eventType = parser.next()
        }
        return talks
    }

    private fun readItem(parser: XmlPullParser): TalkItem {
        var title = ""
        var description = ""
        var link = ""
        var pubDate = ""
        var author = ""
        var duration = ""
        var imageUrl = ""
        var videoUrl: String? = null
        var guid = ""

        while (true) {
            val event = parser.next()
            if (event == XmlPullParser.END_TAG && parser.name == "item") break
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
                "media:content" -> {
                    val type = parser.getAttributeValue(null, "type") ?: ""
                    val url = parser.getAttributeValue(null, "url") ?: ""
                    if (type == "video/mp4" && url.isNotEmpty()) {
                        videoUrl = url
                    }
                    skip(parser)
                }
                "media:group" -> { /* children processed naturally by the outer loop */ }
                else -> skip(parser)
            }
        }

        val parts = title.split(" | ")
        val cleanSpeaker = author.ifEmpty { parts.getOrElse(0) { "" }.trim() }
        val cleanTitle = parts.getOrElse(1) { title }.trim()

        return TalkItem(
            id = guid.ifEmpty { link },
            title = cleanTitle,
            speaker = cleanSpeaker,
            description = stripHtml(description),
            pubDate = formatDate(pubDate),
            duration = duration,
            imageUrl = imageUrl,
            link = link,
            videoUrl = videoUrl,
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
            }
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .trim()
    }

    private fun formatDate(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return try {
            val input = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val output = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            output.format(input.parse(dateStr) ?: return dateStr)
        } catch (_: Exception) {
            dateStr
        }
    }
}
