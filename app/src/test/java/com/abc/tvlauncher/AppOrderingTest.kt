package com.abc.tvlauncher

import com.abc.tvlauncher.data.AppOrdering
import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderingTest {
    private val installed = listOf("a", "b", "c", "d", "e")

    @Test fun defaultsToInstalledOrder() {
        assertEquals(installed, AppOrdering.visible(installed, emptyList(), emptySet(), emptySet()))
    }

    @Test fun savedOrderWinsAndNewAppsGoLast() {
        assertEquals(listOf("c", "a", "b", "d", "e"), AppOrdering.visible(installed, listOf("c", "a", "gone"), emptySet(), emptySet()))
    }

    @Test fun pinnedFirstHiddenDropped() {
        assertEquals(listOf("d", "a", "c", "e"), AppOrdering.visible(installed, emptyList(), setOf("d"), setOf("b")))
    }

    @Test fun moveStaysInsideItsGroup() {
        val vis = listOf("p", "q", "a", "b", "c")
        val pinned = setOf("p", "q")
        assertEquals(listOf("q", "p", "a", "b", "c"), AppOrdering.move(vis, "p", +1, pinned))
        assertEquals(vis, AppOrdering.move(vis, "q", +5, pinned))  // already last pinned: stays
        assertEquals(listOf("p", "q", "a", "b", "c"), AppOrdering.move(vis, "a", -5, pinned))  // can't leave group
        assertEquals(listOf("p", "q", "b", "c", "a"), AppOrdering.move(vis, "a", +9, pinned))  // clamped to end
    }

    @Test fun moveByRow() {
        val vis = listOf("a", "b", "c", "d", "e", "f")
        assertEquals(listOf("a", "c", "d", "b", "e", "f"), AppOrdering.move(vis, "b", +2, emptySet()))
    }
}
