package com.abc.tvlauncher.ui.intro

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Small toolbox shared by the intros: easing, comic text, starbursts.

/** Normalises t into 0..1 between [a] and [b]. */
internal fun seg(t: Float, a: Float, b: Float): Float = ((t - a) / (b - a)).coerceIn(0f, 1f)
internal fun mix(a: Float, b: Float, x: Float): Float = a + (b - a) * x
internal fun easeOut(x: Float): Float { val i = 1f - x; return 1f - i * i * i }
internal fun easeIn(x: Float): Float = x * x * x
internal fun easeInOut(x: Float): Float =
    if (x < 0.5f) 4f * x * x * x else { val i = -2f * x + 2f; 1f - i * i * i / 2f }

/** Ball-dropped-on-the-floor curve: 0 -> 1 with decaying bounces. */
internal fun bounceOut(x: Float): Float {
    val n = 7.5625f
    val d = 2.75f
    return when {
        x < 1f / d -> n * x * x
        x < 2f / d -> { val k = x - 1.5f / d; n * k * k + 0.75f }
        x < 2.5f / d -> { val k = x - 2.25f / d; n * k * k + 0.9375f }
        else -> { val k = x - 2.625f / d; n * k * k + 0.984375f }
    }
}

/** Overshoot-and-settle for pop-in text. */
internal fun popScale(x: Float): Float {
    val c = 1.9f
    val k = x - 1f
    return 1f + (c + 1f) * k * k * k + c * k * k
}

private fun comicPaint(stroke: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
    textAlign = Paint.Align.CENTER
    if (stroke) { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
}
private val fillPaint = comicPaint(false)
private val strokePaint = comicPaint(true)

/** Fat outlined comic-book lettering ("CRASH!", "STRIKE!"). */
internal fun DrawScope.comicText(
    text: String, cx: Float, cy: Float, sizePx: Float,
    angle: Float = 0f, scale: Float = 1f, alpha: Float = 1f,
    fill: Int = 0xFFFFE135.toInt(), stroke: Int = 0xFFB00020.toInt(),
) {
    if (alpha <= 0.01f || scale <= 0.01f) return
    drawIntoCanvas { canvas ->
        val c = canvas.nativeCanvas
        c.save()
        c.translate(cx, cy)
        c.rotate(angle)
        c.scale(scale, scale)
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        strokePaint.textSize = sizePx; strokePaint.strokeWidth = sizePx * 0.17f
        strokePaint.color = stroke; strokePaint.alpha = a
        fillPaint.textSize = sizePx; fillPaint.color = fill; fillPaint.alpha = a
        val baseline = sizePx * 0.35f
        c.drawText(text, 0f, baseline, strokePaint)
        c.drawText(text, 0f, baseline, fillPaint)
        c.restore()
    }
}

/** Spiky comic "POW" shape. */
internal fun DrawScope.starburst(
    center: Offset, outer: Float, inner: Float, points: Int, rotationDeg: Float,
    fill: Color, edge: Color, edgeWidth: Float,
) {
    if (outer <= 1f) return
    val p = Path()
    val rot = rotationDeg * PI.toFloat() / 180f
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = rot + i * PI.toFloat() / points
        val x = center.x + r * cos(a)
        val y = center.y + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, fill)
    drawPath(p, edge, style = Stroke(edgeWidth, join = StrokeJoin.Round))
}

internal fun rand01(seed: Int, i: Int): Float {
    // cheap deterministic hash -> 0..1, so intros look identical every frame without allocating Randoms
    var h = seed * 374761393 + i * 668265263
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777215f
}

internal fun sinf(x: Float) = sin(x)
internal fun cosf(x: Float) = cos(x)
