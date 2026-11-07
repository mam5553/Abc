package com.abc.tvlauncher.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abc.tvlauncher.LauncherModel
import com.abc.tvlauncher.data.AppRepository
import com.abc.tvlauncher.data.PkgInfo
import com.abc.tvlauncher.data.Risk
import com.abc.tvlauncher.system.Actions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Tab(val title: String) { APPS("Apps"), CHECKLIST("Speed checklist") }
private enum class Filter(val title: String) {
    PREINSTALLED("Preinstalled"), BLOAT("Known bloat"), MINE("Installed by me"), ALL("All")
}

@Composable
fun OptimizeScreen(model: LauncherModel) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(Tab.APPS.name) }
    var filter by rememberSaveable { mutableStateOf(Filter.PREINSTALLED.name) }
    val firstTab = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstTab.requestWhenReady() }

    val all: List<PkgInfo>? by produceState<List<PkgInfo>?>(null) {
        value = withContext(Dispatchers.IO) { AppRepository(ctx).loadAllPackages() }
    }
    val shown = remember(all, filter) {
        val list = all ?: return@remember emptyList<PkgInfo>()
        val f = when (Filter.valueOf(filter)) {
            Filter.PREINSTALLED -> list.filter { it.preinstalled }
            Filter.BLOAT -> list.filter { it.knownBloat }
            Filter.MINE -> list.filter { !it.preinstalled }
            Filter.ALL -> list
        }
        f.sortedWith(compareBy<PkgInfo>({ rank(it) }, { it.label.lowercase() }))
    }

    ScreenFrame(
        "Optimize",
        "Open an app's system page to Disable or Force stop it. Anything marked CORE should be left alone.",
        headerExtra = {
            Column {
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Tab.values().forEachIndexed { i, t ->
                        FilterPill(t.title, selected = tab == t.name, focus = if (i == 0) firstTab else null) { tab = t.name }
                    }
                }
                if (tab == Tab.APPS.name) {
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Filter.values().forEach { f -> FilterPill(f.title, selected = filter == f.name, small = true) { filter = f.name } }
                    }
                }
            }
        },
    ) {
        if (tab == Tab.CHECKLIST.name) {
            checklistItems(model)
        } else {
            appItems(all, shown)
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, small: Boolean = false, focus: FocusRequester? = null, onClick: () -> Unit) {
    GlassTile(
        onClick = onClick,
        focusRequester = focus,
        glass = Modifier.height(if (small) 38.dp else 46.dp),
        shape = RoundedCornerShape(50),
        tint = if (selected) Pal.Glow else androidx.compose.ui.graphics.Color.White,
        zoom = 0.06f,
    ) {
        GText(
            text,
            Modifier.align(Alignment.Center).padding(horizontal = if (small) 16.dp else 22.dp),
            size = if (small) 14.sp else 17.sp,
            weight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

// ---- Apps tab -------------------------------------------------------------------------------

private fun LazyListScope.appItems(all: List<PkgInfo>?, shown: List<PkgInfo>) {
    if (all == null) {
        item { Hint("Reading installed apps...") }
        return
    }
    item { Hint("${shown.size} shown  -  ${all.count { it.preinstalled }} preinstalled, ${all.count { it.knownBloat }} known bloat on this TV") }
    if (shown.isEmpty()) item { Hint("Nothing to show here.") }
    items(shown.size, key = { shown[it].pkg }) { i -> PackageRow(shown[i]) }
}

private fun rank(p: PkgInfo) = when {
    p.knownBloat -> 0
    p.preinstalled && p.risk != Risk.CORE -> 1
    p.preinstalled -> 3
    else -> 2
}

@Composable
private fun PackageRow(p: PkgInfo) {
    val ctx = LocalContext.current
    GlassRow(
        title = p.label,
        subtitle = p.note?.note?.let { "${p.pkg}  -  $it" } ?: p.pkg,
        leading = { AppIcon(p.pkg, p.updated, 40.dp) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!p.enabled) Chip("DISABLED", Pal.TextDim)
                if (p.knownBloat) Chip("BLOAT", Pal.Good)
                when (p.risk) {
                    Risk.CORE -> Chip("CORE", Pal.Bad)
                    Risk.CAREFUL -> Chip("CAREFUL", Pal.Warn)
                    else -> {}
                }
                if (p.preinstalled) Chip("PREINSTALLED", Pal.Glow)
                Spacer(Modifier.width(6.dp))
                GText("Disable / Force stop  >", size = 14.sp, weight = FontWeight.SemiBold)
            }
        },
    ) { Actions.openAppSettings(ctx, p.pkg) }
}

// ---- Checklist tab --------------------------------------------------------------------------

private fun LazyListScope.checklistItems(model: LauncherModel) {
    item { Hint("Press OK to open a page (it's ticked off automatically). Hold OK to tick or untick by hand. If a page can't be opened on your TV, follow the path shown.") }
    items(Checklist.items.size, key = { Checklist.items[it].id }) { i ->
        ChecklistRow(Checklist.items[i], model)
    }
}

@Composable
private fun ChecklistRow(item: ChecklistItem, model: LauncherModel) {
    val ctx = LocalContext.current
    val done = item.id in model.checklistDone
    GlassRow(
        title = (if (done) "[x]  " else "[  ]  ") + item.title,
        subtitle = "${item.why}\nWhere: ${item.path}",
        subtitleLines = 3,
        value = if (item.open.isEmpty()) "Manual" else "Open  >",
        valueColor = if (done) Pal.Good else Pal.Text,
        onLongClick = { model.toggleChecklist(item.id) },
    ) {
        if (item.open.isEmpty()) {
            model.toggleChecklist(item.id)
        } else {
            val ok = item.open.any { o ->
                when (o) {
                    is Open.Action -> Actions.tryStart(ctx, o.intent())
                    is Open.App -> Actions.launchApp(ctx, o.pkg)
                }
            }
            if (ok) model.toggleChecklistOn(item.id) else Actions.toast(ctx, "Can't open that page on this TV - follow the path shown")
        }
    }
}
