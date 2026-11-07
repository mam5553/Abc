package com.abc.tvlauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import com.abc.tvlauncher.LauncherModel
import com.abc.tvlauncher.Screen
import com.abc.tvlauncher.XboxMode
import com.abc.tvlauncher.system.Actions
import com.abc.tvlauncher.ui.intro.Intros

@Composable
fun SettingsScreen(model: LauncherModel) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestWhenReady() }
    val resume = rememberResumeTick()
    val serviceOn = remember(resume) { Actions.isHomeServiceEnabled(ctx) }
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?" }

    ScreenFrame("Settings", "Everything is saved automatically") {
        // ---- Intro ---------------------------------------------------------------------
        item { SectionLabel("Intro animation") }
        item {
            GlassRow(
                "Play a cartoon intro when the TV wakes up",
                subtitle = "Only the first time the launcher opens after standby - not on every Home press. Any button skips it.",
                value = if (model.introEnabled) "On" else "Off",
                focus = first,
            ) { model.updateIntroEnabled(!model.introEnabled) }
        }
        items(Intros.all.size) { i ->
            val intro = Intros.all[i]
            GlassRow("Preview: ${intro.title}", value = "Play") {
                model.screen = Screen.Home
                model.startIntro(intro)
            }
        }

        // ---- Sports --------------------------------------------------------------------
        item { SectionLabel("Atlanta Live") }
        item {
            GlassRow(
                "Sports app", subtitle = "Opens when you tap a game",
                value = model.sportsApp?.label ?: "Not set",
            ) { model.screen = Screen.SportsAppPicker }
        }

        // ---- Xbox ----------------------------------------------------------------------
        item { SectionLabel("XBOX tile") }
        item {
            GlassRow(
                "What the XBOX tile does",
                value = if (model.xboxMode == XboxMode.HDMI) "Switch HDMI input" else "Open Xbox app",
            ) { model.updateXboxMode(if (model.xboxMode == XboxMode.HDMI) XboxMode.APP else XboxMode.HDMI) }
        }
        if (model.xboxMode == XboxMode.HDMI) {
            item {
                val label = remember(model.hdmiInputId) {
                    Actions.hdmiInputs(ctx).firstOrNull { it.id == model.hdmiInputId }?.label
                }
                GlassRow(
                    "HDMI input", subtitle = "Switching input also asks the Xbox to wake up over HDMI-CEC",
                    value = label ?: if (model.hdmiInputId == null) "First HDMI input" else "Unknown input",
                ) { model.screen = Screen.HdmiPicker }
            }
            item { Hint("To let the TV wake your Xbox: on the Xbox, Settings > General > TV & display options > Device & connections > HDMI-CEC, turn on \"Power on from TV\". Also keep HDMI control (CEC) enabled in the TV's settings.") }
        } else {
            item {
                GlassRow("Xbox app", value = model.xboxApp?.label ?: "Not set") { model.screen = Screen.XboxAppPicker }
            }
        }
        item { GlassRow("Test the XBOX tile now", value = "Go") { model.openXbox(ctx) } }

        // ---- Home button ---------------------------------------------------------------
        item { SectionLabel("Home button") }
        item {
            GlassRow(
                "Make the remote's Home button open this launcher",
                subtitle = "Needs the helper below. Turn this off to get the stock Google TV home back without uninstalling.",
                value = if (model.homeTakeover) "On" else "Off",
            ) { model.updateHomeTakeover(!model.homeTakeover) }
        }
        item {
            GlassRow(
                "1. Turn on the Home-button helper",
                subtitle = "Accessibility settings > Abc Launcher Home button > On",
                value = if (serviceOn) "Enabled" else "Not enabled",
                valueColor = if (serviceOn) Pal.Good else Pal.Warn,
            ) { Actions.tryStart(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "This TV has no Accessibility settings page") }
        }
        item {
            GlassRow(
                "2. Allow \"display over other apps\"",
                subtitle = "Lets the helper bring the launcher forward from inside another app. Skip it if your TV has no such page.",
                value = "Open",
            ) {
                val ok = Actions.tryStart(ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                if (!ok) Actions.tryStart(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")), "Not available on this TV")
            }
        }
        item {
            GlassRow(
                "Default home app (if your TV offers it)",
                subtitle = "Some TVs let you pick the home app directly. Most Google TVs don't - that's what the helper is for.",
                value = "Open",
            ) { Actions.tryStart(ctx, Intent(Settings.ACTION_HOME_SETTINGS), "This TV has no default-home setting") }
        }

        // ---- Apps ----------------------------------------------------------------------
        item { SectionLabel("Apps") }
        item {
            GlassRow(
                "Hidden apps", subtitle = "Hold OK on an app tile to pin, move or hide it",
                value = "${model.hiddenApps.size} hidden",
            ) { model.screen = Screen.HiddenApps }
        }
        item { GlassRow("Reset app order, pins and hidden apps", value = "Reset") { model.resetLayout() } }

        item { SectionLabel("Speed") }
        item { GlassRow("Optimize this TV", subtitle = "Find bloat apps and cut background junk", value = "Open") { model.screen = Screen.Optimize } }

        item { SectionLabel("About") }
        item { GlassRow("Abc Launcher", value = "v$version") { } }
    }
}
