package dev.jpcottin.tedtalksapp.data

import android.content.Context

/** Which talks the list shows: the bundled videos alone, or the live feed's audio talks too. */
interface TalkSourceSettings {
    var includeAudio: Boolean
}

/** For tests and previews: the choice lives as long as the object. */
class InMemoryTalkSourceSettings(override var includeAudio: Boolean = false) : TalkSourceSettings

/** The choice survives a restart; off until the user turns audio talks on. */
class PreferencesTalkSourceSettings(context: Context) : TalkSourceSettings {
    private val prefs = context.applicationContext.getSharedPreferences("talk_sources", Context.MODE_PRIVATE)

    override var includeAudio: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_AUDIO, false)
        set(value) { prefs.edit().putBoolean(KEY_INCLUDE_AUDIO, value).apply() }

    private companion object {
        const val KEY_INCLUDE_AUDIO = "include_audio"
    }
}
