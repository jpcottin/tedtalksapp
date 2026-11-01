package dev.jpcottin.tedtalksapp.data

data class TalkItem(
    val id: String,
    val title: String,
    val speaker: String,
    val description: String,
    val pubDate: String,
    val duration: String,
    val imageUrl: String,
    val link: String,
    /** The talk itself: an mp4 video, or an mp3 when the feed carries audio only. */
    val mediaUrl: String?,
    val isAudio: Boolean = false,
)
