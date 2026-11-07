package com.abc.tvlauncher.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.abc.tvlauncher.LauncherModel
import com.abc.tvlauncher.Screen
import com.abc.tvlauncher.data.IconCache
import com.abc.tvlauncher.system.Actions

/** Shared page frame for Settings, Optimize and the pickers. */
@Composable
fun ScreenFrame(
    title: String,
    subtitle: String? = null,
    headerExtra: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().launcherBackground()) {
        Column(Modifier.fillMaxSize().padding(start = 64.dp, end = 64.dp, top = 38.dp)) {
            GText(title, size = 38.sp, weight = FontWeight.Light)
            if (subtitle != null) GText(subtitle, size = 16.sp, color = Pal.TextDim, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
            headerExtra?.invoke()
            LazyColumn(
                modifier = Modifier.widthIn(max = 1040.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    GText(text.uppercase(), size = 13.sp, weight = FontWeight.Bold, color = Pal.TextFaint, modifier = Modifier.padding(start = 6.dp, top = 14.dp))
}

@Composable
fun Hint(text: String) {
    GText(text, size = 14.sp, color = Pal.TextDim, maxLines = 4, modifier = Modifier.padding(horizontal = 6.dp))
}

/** A full-width glass row: title (+ optional subtitle) on the left, value on the right. */
@Composable
fun GlassRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = Pal.TextDim,
    focus: FocusRequester? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    subtitleLines: Int = 2,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    GlassTile(
        onClick = onClick,
        onLongClick = onLongClick,
        focusRequester = focus,
        modifier = modifier.fillMaxWidth(),
        glass = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = RoundedCornerShape(20.dp),
        zoom = 0.025f,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) { leading(); Spacer(Modifier.width(16.dp)) }
            Column(Modifier.weight(1f)) {
                GText(title, size = 19.sp)
                if (subtitle != null) GText(subtitle, size = 13.sp, color = Pal.TextDim, maxLines = subtitleLines)
            }
            if (value != null) {
                Spacer(Modifier.width(16.dp))
                GText(value, size = 17.sp, color = valueColor, align = TextAlign.End)
            }
            if (trailing != null) { Spacer(Modifier.width(14.dp)); trailing() }
        }
    }
}

/** Small coloured label ("BLOAT", "CORE" ...). */
@Composable
fun Chip(text: String, color: Color) {
    GText(
        text,
        size = 11.sp,
        weight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .padding(end = 6.dp)
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun AppIcon(pkg: String, stamp: Long, size: androidx.compose.ui.unit.Dp) {
    val ctx = LocalContext.current
    val icon: ImageBitmap? by produceState(IconCache.peek(pkg, stamp), pkg, stamp) {
        if (value == null) value = IconCache.load(ctx, pkg, stamp)
    }
    val b = icon
    if (b != null) Image(b, null, Modifier.size(size)) else Spacer(Modifier.size(size))
}

/** Bumps every time the launcher comes back to the foreground (e.g. after a system settings page). */
@Composable
fun rememberResumeTick(): Int {
    val activity = LocalContext.current as? ComponentActivity
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(activity) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        activity?.lifecycle?.addObserver(obs)
        onDispose { activity?.lifecycle?.removeObserver(obs) }
    }
    return tick
}

// ---------------------------------------------------------------------------------------------
// Pickers
// ---------------------------------------------------------------------------------------------

@Composable
fun AppPickerScreen(model: LauncherModel, title: String, subtitle: String, onPick: (String?) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestWhenReady() }
    ScreenFrame(title, subtitle) {
        item { GlassRow("Automatic", subtitle = "Use the first matching app that is installed", focus = first) { onPick(null) } }
        val all = model.apps
        items(all.size, key = { all[it].pkg }) { i ->
            val a = all[i]
            GlassRow(a.label, subtitle = a.pkg, leading = { AppIcon(a.pkg, a.updated, 40.dp) }) { onPick(a.pkg) }
        }
    }
}

@Composable
fun HdmiPickerScreen(model: LauncherModel) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    val inputs = remember { Actions.hdmiInputs(ctx) }
    LaunchedEffect(Unit) { first.requestWhenReady() }
    ScreenFrame("HDMI input for XBOX", "Pick the input your Xbox is plugged into") {
        item {
            GlassRow("First HDMI input", subtitle = "Let the TV choose", focus = first,
                value = if (model.hdmiInputId == null) "Selected" else null) {
                model.setHdmiInput(null); model.screen = Screen.Settings
            }
        }
        items(inputs.size, key = { inputs[it].id }) { i ->
            val inp = inputs[i]
            GlassRow(inp.label, subtitle = inp.id, value = if (model.hdmiInputId == inp.id) "Selected" else null) {
                model.setHdmiInput(inp.id); model.screen = Screen.Settings
            }
        }
        if (inputs.isEmpty()) item { Hint("This TV didn't report any HDMI inputs to apps. Use \"Open Xbox app\" mode instead, or leave this on automatic.") }
    }
}

@Composable
fun HiddenAppsScreen(model: LauncherModel) {
    val first = remember { FocusRequester() }
    LaunchedEffect(model.hiddenApps.isNotEmpty()) { if (model.hiddenApps.isNotEmpty()) first.requestWhenReady() }
    ScreenFrame("Hidden apps", "Press OK on an app to show it on the home screen again") {
        val hidden = model.hiddenApps
        if (hidden.isEmpty()) item { Hint("Nothing is hidden. Hold OK on an app tile and choose \"Hide from home\" to hide it.") }
        items(hidden.size, key = { hidden[it].pkg }) { i ->
            val a = hidden[i]
            GlassRow(
                a.label, subtitle = a.pkg, value = "Show",
                focus = if (i == 0) first else null,
                leading = { AppIcon(a.pkg, a.updated, 40.dp) },
            ) { model.unhide(a.pkg) }
        }
    }
}
