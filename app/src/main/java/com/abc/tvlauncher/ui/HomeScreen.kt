package com.abc.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abc.tvlauncher.LauncherModel
import com.abc.tvlauncher.Screen
import com.abc.tvlauncher.data.AppEntry
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(model: LauncherModel) {
    val firstFocus = remember { FocusRequester() }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    val gridState = rememberLazyGridState()

    // A launcher has nowhere to go "back" to; Back only cancels move mode.
    BackHandler { model.movingPkg = null }

    // Home pressed again => jump back to the top.
    LaunchedEffect(model.homeTick) { if (model.homeTick > 0) gridState.scrollToItem(0) }
    // Initial focus on the first app once the list is ready.
    LaunchedEffect(model.visibleApps.isNotEmpty()) {
        if (model.visibleApps.isNotEmpty()) firstFocus.requestWhenReady()
    }

    Box(Modifier.fillMaxSize().launcherBackground()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cols = ((maxWidth - 96.dp + 22.dp) / (120.dp + 22.dp)).toInt().coerceIn(4, 10)
            val env = remember(cols, model) { HomeEnv(model, cols, { menuApp = it }, firstFocus) }
            Column(Modifier.fillMaxSize()) {
                Header(model)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 0.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HomeSections.all.forEach { section -> with(section) { addTo(env) } }
                }
            }
        }

        model.movingPkg?.let { pkg ->
            val label = model.apps.firstOrNull { it.pkg == pkg }?.label ?: ""
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(50))
                    .padding(horizontal = 26.dp, vertical = 12.dp),
            ) {
                GText("Moving $label  -  use the arrow keys, press OK when done", size = 16.sp)
            }
        }
    }

    menuApp?.let { app -> AppMenu(app, model, onDismiss = { menuApp = null }) }
}

@Composable
private fun Header(model: LauncherModel) {
    val time = remember(model.now / 30_000) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(model.now)) }
    val date = remember(model.now / 3_600_000) { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(model.now)) }
    Row(
        Modifier.fillMaxWidth().padding(start = 54.dp, end = 48.dp, top = 16.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            GText(time, size = 34.sp, weight = FontWeight.Light)
            GText(date, size = 15.sp, color = Pal.TextDim)
        }
        PillButton("Optimize", Icon.Bolt) { model.screen = Screen.Optimize }
        Spacer(Modifier.width(16.dp))
        PillButton("Settings", Icon.Gear) { model.screen = Screen.Settings }
    }
}

enum class Icon { Bolt, Gear }

@Composable
fun PillButton(text: String, icon: Icon, onClick: () -> Unit) {
    GlassTile(
        onClick = onClick,
        glass = Modifier.height(48.dp),
        shape = RoundedCornerShape(50),
        zoom = 0.08f,
    ) {
        Row(Modifier.padding(horizontal = 22.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(20.dp)) {
                val w = size.width
                val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
                when (icon) {
                    Icon.Gear -> {
                        drawCircle(Color.White, radius = w * 0.2f, style = stroke)
                        for (i in 0 until 8) {
                            val a = Math.PI * 2 * i / 8
                            val c = center
                            drawLine(
                                Color.White,
                                Offset(c.x + (w * 0.32f * cos(a)).toFloat(), c.y + (w * 0.32f * sin(a)).toFloat()),
                                Offset(c.x + (w * 0.46f * cos(a)).toFloat(), c.y + (w * 0.46f * sin(a)).toFloat()),
                                w * 0.12f, StrokeCap.Round,
                            )
                        }
                    }
                    Icon.Bolt -> {
                        val p = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.58f, 0f); lineTo(w * 0.18f, w * 0.56f); lineTo(w * 0.48f, w * 0.56f)
                            lineTo(w * 0.40f, w); lineTo(w * 0.84f, w * 0.40f); lineTo(w * 0.54f, w * 0.40f); close()
                        }
                        drawPath(p, Color.White)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            GText(text, size = 16.sp, weight = FontWeight.SemiBold)
        }
    }
}
