package com.abc.tvlauncher.ui.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abc.tvlauncher.XboxMode
import com.abc.tvlauncher.system.Actions
import com.abc.tvlauncher.ui.FullSpan
import com.abc.tvlauncher.ui.GText
import com.abc.tvlauncher.ui.GlassTile
import com.abc.tvlauncher.ui.HomeEnv
import com.abc.tvlauncher.ui.HomeSection
import com.abc.tvlauncher.ui.Pal

/** A big tile in the "quick launch" row. Implement this + add to [QuickTiles.all] for a new shortcut. */
interface QuickTile {
    val id: String
    @Composable fun Content(env: HomeEnv)
}

object QuickTiles {
    val all: List<QuickTile> = listOf(XboxTile)
}

object QuickSection : HomeSection {
    override val id = "quick"

    @OptIn(ExperimentalComposeUiApi::class)
    override fun LazyGridScope.addTo(env: HomeEnv) {
        item(key = "quick", span = FullSpan) {
            LazyRow(
                modifier = Modifier.focusRestorer(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(QuickTiles.all, key = { it.id }) { it.Content(env) }
            }
        }
    }
}

/** XBOX: switches the TV to the Xbox's HDMI input (waking it over CEC) or opens the Xbox app. */
object XboxTile : QuickTile {
    override val id = "xbox"

    @Composable
    override fun Content(env: HomeEnv) {
        val ctx = LocalContext.current
        val m = env.model
        val subtitle = when (m.xboxMode) {
            XboxMode.HDMI -> {
                val label = remember(m.hdmiInputId) {
                    Actions.hdmiInputs(ctx).let { l -> l.firstOrNull { it.id == m.hdmiInputId } ?: l.firstOrNull() }?.label
                }
                "Switch to ${label ?: "HDMI"}"
            }
            XboxMode.APP -> "Open Xbox app"
        }
        GlassTile(
            onClick = { m.openXbox(ctx) },
            glass = Modifier.width(250.dp).height(64.dp),
            shape = RoundedCornerShape(26.dp),
            tint = Pal.Xbox,
            glow = Pal.Xbox,
            zoom = 0.08f,
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                XboxMark(Modifier.size(40.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    GText("XBOX", size = 19.sp, weight = FontWeight.Bold)
                    GText(subtitle, size = 13.sp, color = Pal.TextDim)
                }
            }
        }
    }
}

/** A generic green sphere with an X, drawn in code (no bitmap, no trademark art). */
@Composable
private fun XboxMark(modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF7CF08A), Color(0xFF1E9E3A), Color(0xFF0E5A22)),
                center = Offset(size.width * 0.38f, size.height * 0.3f), radius = r * 1.4f,
            ),
        )
        val a = r * 0.46f
        val c = center
        val sw = r * 0.26f
        drawLine(Color.White, Offset(c.x - a, c.y - a), Offset(c.x + a, c.y + a), sw, StrokeCap.Round)
        drawLine(Color.White, Offset(c.x + a, c.y - a), Offset(c.x - a, c.y + a), sw, StrokeCap.Round)
        drawCircle(Color.White.copy(alpha = 0.45f), r - 1f, style = Stroke(2f))
    }
}
