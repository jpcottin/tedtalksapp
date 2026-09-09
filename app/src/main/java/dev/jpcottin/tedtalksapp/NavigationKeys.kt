package dev.jpcottin.tedtalksapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object TalksList : NavKey

@Serializable data class TalkDetail(val talkId: String) : NavKey
