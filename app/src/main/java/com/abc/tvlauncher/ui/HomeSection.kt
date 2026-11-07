package com.abc.tvlauncher.ui

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import com.abc.tvlauncher.LauncherModel
import com.abc.tvlauncher.data.AppEntry
import com.abc.tvlauncher.ui.sections.AppsSection
import com.abc.tvlauncher.ui.sections.QuickSection
import com.abc.tvlauncher.ui.sections.SportsSection
import kotlinx.coroutines.delay

/** What a section needs from the home screen. */
class HomeEnv(
    val model: LauncherModel,
    /** Number of grid columns (used by "move app" mode to step up/down a row). */
    val cols: Int,
    val openMenu: (AppEntry) -> Unit,
    /** Attached to the first app tile so the home screen can put initial focus there. */
    val firstFocus: FocusRequester,
)

/**
 * A block of the home screen (a row, a grid, a banner...). To add a feature:
 *   1. write an object implementing HomeSection,
 *   2. add it to [HomeSections.all].
 * Sections appear top-to-bottom in list order. A section that adds no items is simply hidden.
 */
interface HomeSection {
    val id: String
    fun LazyGridScope.addTo(env: HomeEnv)
}

object HomeSections {
    val all: List<HomeSection> = listOf(
        SportsSection,   // "Atlanta Live" - hidden when there are no games
        QuickSection,    // XBOX tile (+ any future quick tiles)
        AppsSection,     // installed apps
    )
}

/** Full-width grid item (rows, headers). */
val FullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    GText(
        text,
        modifier = modifier.padding(start = 6.dp, top = 10.dp),
        size = 20.sp,
        weight = FontWeight.SemiBold,
        color = Pal.TextDim,
    )
}

/** Focus can fail if the target isn't laid out yet; retry for a moment. */
suspend fun FocusRequester.requestWhenReady(tries: Int = 8) {
    repeat(tries) {
        if (runCatching { requestFocus() }.isSuccess) return
        delay(60)
    }
}
