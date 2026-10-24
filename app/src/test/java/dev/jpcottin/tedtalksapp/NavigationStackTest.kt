package dev.jpcottin.tedtalksapp

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationStackTest {

    @Test
    fun openingTalksInARow_keepsOneDetailOnTheStack() {
        val stack = mutableListOf<NavKey>(TalksList)
        stack.openTalk("a")
        stack.openTalk("b")
        stack.openTalk("c")
        assertEquals(listOf<NavKey>(TalksList, TalkDetail("c")), stack)
    }

    @Test
    fun openingTheSameTalkAgain_addsNothing() {
        val stack = mutableListOf<NavKey>(TalksList)
        stack.openTalk("a")
        stack.openTalk("a")
        assertEquals(listOf<NavKey>(TalksList, TalkDetail("a")), stack)
    }

    @Test
    fun poppingTheOnlyDetail_leavesTheListAndNoSelection() {
        val stack = mutableListOf<NavKey>(TalksList)
        stack.openTalk("a")
        assertNull(stack.popDetail())
        assertEquals(listOf<NavKey>(TalksList), stack)
    }

    @Test
    fun poppingReportsTheDetailLeftUnderneath() {
        // A stack an older build could have saved, with several details stacked.
        val stack = mutableListOf<NavKey>(TalksList, TalkDetail("a"), TalkDetail("b"))
        assertEquals("a", stack.popDetail())
        assertNull(stack.popDetail())
    }
}
