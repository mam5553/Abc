package com.abc.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abc.tvlauncher.LauncherModel
import com.abc.tvlauncher.data.AppEntry
import com.abc.tvlauncher.system.Actions

/** The menu shown when you hold OK on an app. */
@Composable
fun AppMenu(app: AppEntry, model: LauncherModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestWhenReady() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(440.dp).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GText(app.label, size = 26.sp, weight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
                val pinned = app.pkg in model.pinned
                MenuRow("Open", first) { onDismiss(); Actions.launchApp(ctx, app.pkg) }
                MenuRow(if (pinned) "Unpin" else "Pin to front") { model.togglePin(app.pkg); onDismiss() }
                MenuRow("Move") { model.movingPkg = app.pkg; onDismiss() }
                MenuRow("Hide from home") { model.hide(app.pkg); onDismiss() }
                MenuRow("App info  (Disable / Force stop)") { onDismiss(); Actions.openAppSettings(ctx, app.pkg) }
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, focus: FocusRequester? = null, onClick: () -> Unit) {
    GlassTile(
        onClick = onClick,
        focusRequester = focus,
        modifier = Modifier.fillMaxWidth(),
        glass = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        zoom = 0.04f,
    ) {
        GText(text, Modifier.align(Alignment.CenterStart).padding(horizontal = 22.dp), size = 18.sp)
    }
}
