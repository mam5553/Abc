package com.abc.tvlauncher.ui.intro

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

/**
 * A bowling ball rolls down a lane and smashes ten glass "app tiles" set up like pins.
 * STRIKE! The whole alley fades out to reveal the home screen.
 */
object BowlingStrike : Intro("bowling", "Bowling strike", 2800) {
    private const val HIT = 0.44f

    private val tileColors = listOf(
        0xFF4C8DFF, 0xFFFF5C8A, 0xFF3DDC97, 0xFFFFB83D, 0xFFB26BFF,
        0xFF4CD9FF, 0xFFFF7A59, 0xFF7BE05A, 0xFFFF4DD2, 0xFFFFD84A,
    ).map { Color(it) }

    // (row, xs) for the 4-3-2-1 triangle, front pin first.
    private class Pin(val y: Float, val scale: Float, val xs: Float, val color: Color, val seed: Int)

    private val pins: List<Pin> = buildList {
        val rows = listOf(
            Triple(0.540f, 1.00f, listOf(0f)),
            Triple(0.468f, 0.92f, listOf(-0.5f, 0.5f)),
            Triple(0.410f, 0.85f, listOf(-1f, 0f, 1f)),
            Triple(0.365f, 0.79f, listOf(-1.5f, -0.5f, 0.5f, 1.5f)),
        )
        var i = 0
        rows.forEach { (y, s, xs) -> xs.forEach { add(Pin(y, s, it, tileColors[i % tileColors.size], i)); i++ } }
    }

    override fun homeLayer(t: Float): HomeLayer = HomeLayer(scale = mix(1.12f, 1f, easeOut(seg(t, 0.55f, 0.95f))))

    override fun DrawScope.drawForeground(t: Float) {
        val sceneAlpha = 1f - easeInOut(seg(t, 0.62f, 0.86f))
        if (sceneAlpha <= 0f) return
        drawIntoCanvas { c ->
            val layer = Paint().apply { alpha = sceneAlpha }
            c.saveLayer(Rect(Offset.Zero, size), layer)
            scene(t)
            c.restore()
        }
    }

    private fun DrawScope.scene(t: Float) {
        val u = size.height / 1080f
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // alley
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0614), Color(0xFF2A1450), Color(0xFF140B26))))
        fun half(y: Float) = mix(0.10f * w, 0.56f * w, ((y - 0.30f * h) / (0.70f * h)).coerceIn(0f, 1.2f))
        val laneTop = 0.30f * h
        val lane = Path().apply {
            moveTo(cx - half(laneTop), laneTop); lineTo(cx + half(laneTop), laneTop)
            lineTo(cx + half(h), h); lineTo(cx - half(h), h); close()
        }
        drawPath(lane, Brush.verticalGradient(listOf(Color(0xFF9A6230), Color(0xFFD99A55), Color(0xFFEDB26E)), laneTop, h))
        for (i in -5..5) {   // boards
            val f = i / 5.5f
            drawLine(Color(0x22000000), Offset(cx + f * half(laneTop), laneTop), Offset(cx + f * half(h), h), 2f * u)
        }
        drawPath(lane, Color(0x66FFFFFF), style = Stroke(3f * u))
        drawRect(Color(0x55FFFFFF), Offset(cx - half(0.88f * h), 0.88f * h), Size(half(0.88f * h) * 2, 4f * u))
        drawCircle(Brush.radialGradient(listOf(Color(0x55FFFFFF), Color.Transparent), Offset(cx, 0.42f * h), 0.34f * w), 0.34f * w, Offset(cx, 0.42f * h))

        // pins (glass tiles)
        val sec = ((t - HIT) * 2.8f)
        for (pin in pins.reversed()) {
            val size0 = 128f * u * pin.scale
            val gap = 150f * u * pin.scale
            val bx = cx + pin.xs * gap
            val by = pin.y * h
            var x = bx; var y = by; var s = 1f; var rot = 0f; var a = 1f
            if (sec > 0f) {
                val ti = (sec - pin.seed * 0.012f).coerceAtLeast(0f)
                val dirx = ((bx - cx) / (0.18f * w)) + (rand01(51, pin.seed) - 0.5f) * 1.4f
                x = bx + dirx * 620f * u * ti
                y = by - (380f + rand01(52, pin.seed) * 700f) * u * ti + 0.5f * 2500f * u * ti * ti
                s = 1f + ti * 1.7f
                rot = (rand01(53, pin.seed) - 0.5f) * 1700f * ti
                a = 1f - seg(ti, 0.55f, 1.05f)
            }
            if (a <= 0f) continue
            glassTile(Offset(x, y), size0 * s, rot, pin.color, a, u)
        }

        // ball
        val roll = seg(t, 0.05f, HIT)
        if (roll > 0f) {
            val bp = easeIn(roll)
            val pass = seg(t, HIT, HIT + 0.12f)
            val bx = cx + sin(roll * 3.1416f) * 0.025f * w
            val by = if (pass > 0f) mix(0.54f * h, 0.38f * h, pass) else mix(1.18f * h, 0.54f * h, bp)
            val r = (if (pass > 0f) mix(66f, 24f, pass) else mix(170f, 66f, bp)) * u
            val a = if (pass > 0f) 1f - seg(pass, 0.4f, 1f) else 1f
            if (a > 0f) {
                drawOval(Color(0x55000000), Offset(bx - r * 0.9f, by + r * 0.75f), Size(r * 1.8f, r * 0.45f))
                ball(Offset(bx, by), r, roll * 1100f, a)
            }
        }

        // STRIKE!
        if (t >= HIT) {
            val p = seg(t, HIT, HIT + 0.06f)
            val pop = popScale(p)
            val c = Offset(cx, 0.20f * h)
            withTransform({ scale(pop, pop, c) }) {
                starburst(c, 330f * u, 215f * u, 14, sin(t * 20f) * 4f - 8f, Color(0xFFFFD02B), Color(0xFFE53935), 10f * u)
            }
            comicText("STRIKE!", c.x, c.y, 160f * u, angle = -7f, scale = pop, fill = 0xFFFFFFFF.toInt(), stroke = 0xFFC62828.toInt())
            val flash = 0.65f * (1f - seg(t, HIT, HIT + 0.05f))
            if (flash > 0f) drawRect(Color.White.copy(alpha = flash))
        }
    }

    private fun DrawScope.glassTile(c: Offset, size: Float, rotDeg: Float, color: Color, alpha: Float, u: Float) {
        withTransform({ translate(c.x, c.y); rotate(rotDeg, Offset.Zero) }) {
            val tl = Offset(-size / 2, -size / 2)
            val sz = Size(size, size)
            val rr = CornerRadius(size * 0.24f)
            drawRoundRect(color.copy(alpha = 0.75f * alpha), tl, sz, rr)
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f * alpha), Color.Transparent), -size / 2, 0f),
                tl, sz, rr,
            )
            drawRoundRect(Color.White.copy(alpha = 0.9f * alpha), tl, sz, rr, style = Stroke(3f * u))
            drawCircle(Color.White.copy(alpha = 0.85f * alpha), size * 0.17f, Offset.Zero)
        }
    }

    private fun DrawScope.ball(c: Offset, r: Float, spinDeg: Float, alpha: Float) {
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF7C8BFF), Color(0xFF232A8F), Color(0xFF080B33)),
                Offset(c.x - r * 0.35f, c.y - r * 0.4f), r * 1.5f,
            ),
            r, c, alpha,
        )
        withTransform({ rotate(spinDeg, c) }) {
            for ((dx, dy) in listOf(-0.02f to -0.52f, 0.22f to -0.42f, -0.24f to -0.40f)) {
                drawCircle(Color(0xFF03041A).copy(alpha = alpha), r * 0.11f, Offset(c.x + dx * r, c.y + dy * r))
            }
        }
        drawOval(Color.White.copy(alpha = 0.7f * alpha), Offset(c.x - r * 0.52f, c.y - r * 0.62f), Size(r * 0.34f, r * 0.2f))
    }
}
