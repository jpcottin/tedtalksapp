package dev.jpcottin.tedtalksapp

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** What the manifest has to declare for the platforms the app runs on. */
class ManifestTest {
    /** Predictive back is opt-in on Android 13 to 15; the manifest must say so. */
    @Test
    fun application_optsIntoPredictiveBack() {
        val application = elements("application").singleOrNull()
        assertNotNull("no <application> element", application)
        assertEquals("true", application!!["enableOnBackInvokedCallback"])
    }

    /**
     * The Jetpack XR SDK is in the one APK published on the mobile track, so Android XR
     * must be declared and not required: a phone or TV without it still installs the app.
     */
    @Test
    fun androidXrSpatialApiIsDeclaredAndOptional() {
        val xr = elements("uses-feature").singleOrNull { it["name"] == "android.software.xr.api.spatial" }
        assertNotNull("no <uses-feature android:name=\"android.software.xr.api.spatial\">", xr)
        assertEquals("false", xr!!["required"])
    }

    /** A TV has no touchscreen and a phone no leanback UI: neither may be required. */
    @Test
    fun tvFeaturesAreDeclaredAndOptional() {
        val features = elements("uses-feature").associate { it["name"] to it["required"] }
        assertEquals("false", features["android.software.leanback"])
        assertEquals("false", features["android.hardware.touchscreen"])
    }

    /** The android: attributes of every [tag] element in the main manifest. */
    private fun elements(tag: String): List<Map<String, String?>> {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        val found = mutableListOf<Map<String, String?>>()
        File("src/main/AndroidManifest.xml").inputStream().use { input ->
            parser.setInput(input, "UTF-8")
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == tag) {
                    found += (0 until parser.attributeCount)
                        .filter { parser.getAttributeNamespace(it) == ANDROID_NS }
                        .associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
                }
            }
        }
        return found
    }

    private companion object { const val ANDROID_NS = "http://schemas.android.com/apk/res/android" }
}
