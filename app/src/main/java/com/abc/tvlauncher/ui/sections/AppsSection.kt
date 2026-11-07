package com.abc.tvlauncher.ui.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.abc.tvlauncher.data.AppEntry
import com.abc.tvlauncher.data.IconCache
import com.abc.tvlauncher.system.Actions
import com.abc.tvlauncher.ui.FullSpan
import com.abc.tvlauncher.ui.GlassTile
import com.abc.tvlauncher.ui.HomeEnv
import com.abc.tvlauncher.ui.HomeSection
import com.abc.tvlauncher.ui.Pal
import com.abc.tvlauncher.ui.SectionTitle
import com.abc.tvlauncher.ui.requestWhenReady

/** The user's apps as glass tiles. Hold OK on a tile to pin / move / hide. */
object AppsSection : HomeSection {
    override val id = "apps"

    override fun LazyGridScope.addTo(env: HomeEnv) {
        val apps = env.model.visibleApps
        if (apps.isEmpty()) return
        item(key = "apps-title", span = FullSpan) { SectionTitle("My Apps") }
        items(apps, key = { it.pkg }) { app ->
            AppTile(app, index = apps.indexOf(app), env = env)
        }
    }
}

@Composable
private fun AppTile(app: AppEntry, index: Int, env: HomeEnv) {
    val ctx = LocalContext.current
    val m = env.model
    val moving = m.movingPkg == app.pkg
    val pinned = app.pkg in m.pinned

    val icon: ImageBitmap? by produceState(IconCache.peek(app.pkg, app.updated), app.pkg, app.updated) {
        if (value == null) value = IconCache.load(ctx, app.pkg, app.updated)
    }

    val own = remember { FocusRequester() }
    val requester = if (index == 0) env.firstFocus else own
    // While being moved the tile changes position; keep the remote's focus on it.
    if (moving) LaunchedEffect(index) { requester.requestWhenReady() }

    GlassTile(
        onClick = { if (moving) m.movingPkg = null else Actions.launchApp(ctx, app.pkg) },
        onLongClick = if (moving) null else ({ env.openMenu(app) }),
        onKey = if (moving) ({ ev -> handleMoveKey(ev, app, env) }) else null,
        focusRequester = requester,
        modifier = Modifier.fillMaxWidth(),
        glass = Modifier.fillMaxWidth().aspectRatio(1.35f),
        shape = RoundedCornerShape(26.dp),
        tint = if (moving) Pal.Glow else Color.White,
        glow = if (moving) Color.White else Pal.Glow,
        label = app.label,
    ) {
        val b = icon
        if (b != null) {
            Image(
                b, contentDescription = app.label,
                modifier = Modifier.align(Alignment.Center).fillMaxSize(0.6f),
            )
        }
        if (pinned) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(9.dp)
                    .clip(CircleShape).background(Pal.Glow),
            )
        }
    }
}

/** Arrow keys shuffle the tile; OK / Back finish. Returns true when it handled the key. */
private fun handleMoveKey(ev: KeyEvent, app: AppEntry, env: HomeEnv): Boolean {
    if (ev.type != KeyEventType.KeyDown) {
        // Swallow the matching key-ups so OK doesn't also "click" the tile.
        return ev.key == Key.DirectionCenter || ev.key == Key.Enter
    }
    val m = env.model
    when (ev.key) {
        Key.DirectionLeft -> m.move(app.pkg, -1)
        Key.DirectionRight -> m.move(app.pkg, +1)
        Key.DirectionUp -> m.move(app.pkg, -env.cols)
        Key.DirectionDown -> m.move(app.pkg, +env.cols)
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> m.movingPkg = null
        else -> return false
    }
    return true
}
