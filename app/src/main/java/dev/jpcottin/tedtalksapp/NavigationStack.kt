package dev.jpcottin.tedtalksapp

import androidx.navigation3.runtime.NavKey

/**
 * Opens [talkId]: a detail replaces the detail already on top (in a two-pane
 * scene the list stays visible, so every tap would otherwise push one more
 * entry and Back would need as many presses to leave), or goes on top of the
 * list.
 */
internal fun MutableList<NavKey>.openTalk(talkId: String) {
    if (lastOrNull() is TalkDetail) this[lastIndex] = TalkDetail(talkId) else add(TalkDetail(talkId))
}

/** Pops the top entry and returns the talk still shown underneath, if any. */
internal fun MutableList<NavKey>.popDetail(): String? {
    removeLastOrNull()
    return (lastOrNull() as? TalkDetail)?.talkId
}
