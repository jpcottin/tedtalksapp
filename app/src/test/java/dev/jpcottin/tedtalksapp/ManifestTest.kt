package dev.jpcottin.tedtalksapp

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** Predictive back is opt-in on Android 13 to 15; the manifest must say so. */
class ManifestTest {
    @Test
    fun application_optsIntoPredictiveBack() {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        File("src/main/AndroidManifest.xml").inputStream().use { input ->
            parser.setInput(input, "UTF-8")
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "application") {
                    assertEquals("true", parser.getAttributeValue(ANDROID_NS, "enableOnBackInvokedCallback"))
                    return
                }
            }
        }
        fail("no <application> element")
    }

    private companion object { const val ANDROID_NS = "http://schemas.android.com/apk/res/android" }
}
