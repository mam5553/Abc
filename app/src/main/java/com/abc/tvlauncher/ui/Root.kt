package com.abc.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.abc.tvlauncher.LauncherModel
import com.abc.tvlauncher.Screen
import com.abc.tvlauncher.ui.intro.Intro

/**
 * Top of the UI tree. Layers, back to front:
 *   intro background  ->  the actual screen (animated by the intro)  ->  intro foreground.
 * With no intro playing this is just the screen.
 */
@Composable
fun Root(model: LauncherModel) {
    val intro = model.activeIntro
    val t = remember(intro) { Animatable(0f) }

    LaunchedEffect(intro) {
        if (intro != null) {
            t.animateTo(1f, tween(intro.durationMs, easing = LinearEasing))
            model.finishIntro()
        }
    }

    BackHandler(enabled = model.screen != Screen.Home) {
        model.screen = when (model.screen) {
            Screen.SportsAppPicker, Screen.XboxAppPicker, Screen.HdmiPicker, Screen.HiddenApps -> Screen.Settings
            else -> Screen.Home
        }
    }

    IntroStage(intro, { t.value }) { Screens(model) }
}

/**
 * The intro layers, back to front: intro background -> [content] (moved by the intro) ->
 * intro foreground. With no intro this is just [content]. [t] is read only while drawing,
 * so animating it never recomposes anything.
 */
@Composable
fun IntroStage(intro: Intro?, t: () -> Float, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (intro != null) {
            Canvas(Modifier.fillMaxSize()) { with(intro) { drawBackground(t()) } }
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                if (intro != null) {
                    val l = intro.homeLayer(t())
                    alpha = l.alpha
                    translationX = l.dx * size.width
                    translationY = l.dy * size.height
                    scaleX = l.scale; scaleY = l.scale
                    rotationZ = l.rotation
                }
            },
        ) { content() }
        if (intro != null) {
            Canvas(Modifier.fillMaxSize()) { with(intro) { drawForeground(t()) } }
        }
    }
}

@Composable
private fun Screens(model: LauncherModel) {
    when (model.screen) {
        Screen.Home -> HomeScreen(model)
        Screen.Settings -> SettingsScreen(model)
        Screen.Optimize -> OptimizeScreen(model)
        Screen.SportsAppPicker -> AppPickerScreen(model, "Sports app", "Opens when you tap a game") { pkg ->
            model.updateSportsPkg(pkg); model.screen = Screen.Settings
        }
        Screen.XboxAppPicker -> AppPickerScreen(model, "Xbox app", "Opens when the XBOX tile is set to \"Open app\"") { pkg ->
            model.updateXboxPkg(pkg); model.screen = Screen.Settings
        }
        Screen.HdmiPicker -> HdmiPickerScreen(model)
        Screen.HiddenApps -> HiddenAppsScreen(model)
    }
}
