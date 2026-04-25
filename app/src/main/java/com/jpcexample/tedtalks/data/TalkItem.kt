package com.jpcexample.tedtalks.data

data class TalkItem(
    val id: String,
    val title: String,
    val speaker: String,
    val description: String,
    val pubDate: String,
    val duration: String,
    val imageUrl: String,
    val link: String,
    val videoUrl: String?,
)
