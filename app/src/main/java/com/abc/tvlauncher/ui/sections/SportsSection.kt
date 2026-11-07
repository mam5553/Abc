package com.abc.tvlauncher.ui.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.abc.tvlauncher.data.Game
import com.abc.tvlauncher.data.GameState
import com.abc.tvlauncher.data.RemoteImages
import com.abc.tvlauncher.data.Team
import com.abc.tvlauncher.ui.FullSpan
import com.abc.tvlauncher.ui.GText
import com.abc.tvlauncher.ui.GlassTile
import com.abc.tvlauncher.ui.HomeEnv
import com.abc.tvlauncher.ui.HomeSection
import com.abc.tvlauncher.ui.Pal
import com.abc.tvlauncher.ui.SectionTitle
import java.text.DateFormat
import java.util.Date

/** Top row: Atlanta teams that are live or play today. Not shown at all when there are none. */
object SportsSection : HomeSection {
    override val id = "sports"

    @OptIn(ExperimentalComposeUiApi::class)
    override fun LazyGridScope.addTo(env: HomeEnv) {
        val games = env.model.games
        if (games.isEmpty()) return
        item(key = "sports", span = FullSpan) {
            Column {
                SectionTitle("Atlanta Live")
                LazyRow(
                    modifier = Modifier.focusRestorer(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(games, key = { it.league + it.id }) { g -> GameCard(g, env) }
                }
            }
        }
    }
}

@Composable
private fun GameCard(g: Game, env: HomeEnv) {
    val ctx = LocalContext.current
    GlassTile(
        onClick = { env.model.openSports(ctx) },
        glass = Modifier.width(250.dp).height(100.dp),
        shape = RoundedCornerShape(24.dp),
        zoom = 0.08f,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            val awayLeads = (g.away.score ?: 0) > (g.home.score ?: 0)
            val homeLeads = (g.home.score ?: 0) > (g.away.score ?: 0)
            TeamLine(g.away, g.state, dim = g.state == GameState.POST && homeLeads)
            TeamLine(g.home, g.state, dim = g.state == GameState.POST && awayLeads)
            StatusLine(g)
        }
    }
}

@Composable
private fun TeamLine(t: Team, state: GameState, dim: Boolean) {
    val color = if (dim) Pal.TextFaint else Pal.Text
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(t, Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        GText(t.name, Modifier.weight(1f), size = 16.sp, weight = FontWeight.SemiBold, color = color)
        if (state != GameState.PRE) {
            GText(t.score?.toString() ?: "-", size = 19.sp, weight = FontWeight.Bold, color = color, align = TextAlign.End)
        }
    }
}

@Composable
private fun StatusLine(g: Game) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        when (g.state) {
            GameState.LIVE -> {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Pal.Live))
                Spacer(Modifier.width(7.dp))
                GText(g.statusText.ifBlank { "LIVE" }, Modifier.weight(1f), size = 14.sp, color = Pal.Live, weight = FontWeight.SemiBold)
            }
            GameState.PRE -> {
                val t = remember(g.startMillis) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(g.startMillis)) }
                GText("Today  $t", Modifier.weight(1f), size = 14.sp, color = Pal.TextDim)
            }
            GameState.POST -> GText(g.statusText.ifBlank { "Final" }, Modifier.weight(1f), size = 14.sp, color = Pal.TextDim)
        }
        GText(g.league, size = 12.sp, color = Pal.TextFaint, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun TeamLogo(t: Team, modifier: Modifier) {
    val ctx = LocalContext.current
    val url = t.logo
    val bmp: ImageBitmap? by produceState(url?.let { RemoteImages.peek(it) }, url) {
        if (url != null && value == null) value = RemoteImages.load(ctx, url)
    }
    val b = bmp
    if (b != null) {
        Image(b, contentDescription = t.name, modifier = modifier)
    } else {
        Box(modifier.clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            GText(t.abbr.take(3), size = 10.sp, weight = FontWeight.Bold, align = TextAlign.Center)
        }
    }
}
