package com.abc.tvlauncher.ui.intro

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * How the real home screen is moved/faded while an intro plays.
 * dx / dy are fractions of the screen width / height; rotation is in degrees.
 */
data class HomeLayer(
    val alpha: Float = 1f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

/**
 * A funny cartoon intro, drawn entirely in code. The animation runs on one clock, [t] = 0..1
 * over [durationMs]. Three optional hooks, from back to front:
 *
 *   drawBackground(t)  - painted BEHIND the home screen
 *   homeLayer(t)       - where the home screen itself is (default: sitting still, fully visible)
 *   drawForeground(t)  - painted IN FRONT of the home screen (covers, characters, comic text)
 *
 * HOW TO ADD A NEW INTRO
 *   1. Copy any file in this folder (CarCrash.kt is the simplest to follow).
 *   2. Change the id / title / drawing.
 *   3. Add the object to [Intros.all] below. That's it - it shows up in Settings > Preview
 *      and joins the random rotation automatically.
 * Keep each intro between ~2 and 3 seconds, and make sure it ends with the home screen fully
 * visible (alpha 1, scale 1, no offset).
 */
abstract class Intro(val id: String, val title: String, val durationMs: Int = 2700) {
    open fun homeLayer(t: Float): HomeLayer = HomeLayer()
    open fun DrawScope.drawBackground(t: Float) {}
    open fun DrawScope.drawForeground(t: Float) {}
}

object Intros {
    val all: List<Intro> = listOf(
        CarCrash,
        NinjaSlice,
        BowlingStrike,
        WreckingBall,
    )

    /** Random intro, never the same one twice in a row. */
    fun pick(lastId: String?): Intro = all.filter { it.id != lastId }.ifEmpty { all }.random()
}
