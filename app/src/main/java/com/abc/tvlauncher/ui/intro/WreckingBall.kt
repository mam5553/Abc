package com.abc.tvlauncher.ui.intro

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A brick wall covers the screen. A wrecking ball winds back, swings into the camera and
 * blows the wall to pieces, revealing the home screen.
 */
object WreckingBall : Intro("wrecking_ball", "Wrecking ball", 2800) {
    private const val HIT = 0.46f
    private const val COLS = 10
    private const val ROWS = 7
    private val brickColors = listOf(0xFFB4533A, 0xFFC4623F, 0xFFA84A33, 0xFFCF7048, 0xFF9C4130).map { Color(it) }

    override fun homeLayer(t: Float): HomeLayer {
        val sh = seg(t, HIT, HIT + 0.14f)
        return HomeLayer(
            scale = mix(1.09f, 1f, easeOut(seg(t, HIT, 0.9f))),
            dx = if (t > HIT) sin(sh * 50f) * (1f - sh) * 0.008f else 0f,
            dy = if (t > HIT) cos(sh * 44f) * (1f - sh) * 0.008f else 0f,
        )
    }

    override fun DrawScope.drawForeground(t: Float) {
        val u = size.height / 1080f
        val w = size.width
        val h = size.height
        val bw = w / COLS
        val bh = h / ROWS
        val impact = Offset(w * 0.5f, h * 0.5f)
        val sec = ((t - HIT) * 2.8f).coerceAtLeast(0f)
        val maxDist = sqrt(w * w + h * h) / 2f

        // mortar (vanishes at impact so the home screen shows through the gaps)
        val mortar = 1f - seg(t, HIT, HIT + 0.02f)
        if (mortar > 0f) drawRect(Color(0xFF6E5D52).copy(alpha = mortar))

        // bricks
        var idx = 0
        for (r in 0 until ROWS) {
            val offset = if (r % 2 == 1) -bw / 2f else 0f
            for (c in 0..COLS) {
                val i = idx++
                val cx = offset + c * bw + bw / 2f
                val cy = r * bh + bh / 2f
                if (cx + bw / 2f < 0f || cx - bw / 2f > w) continue
                var x = cx; var y = cy; var s = 1f; var rot = 0f
                if (sec > 0f) {
                    val dx = cx - impact.x
                    val dy = cy - impact.y
                    val dist = sqrt(dx * dx + dy * dy)
                    val ti = (sec - dist / (1.8f * w) * 0.16f).coerceAtLeast(0f)
                    val ang = atan2(dy, dx) + (rand01(61, i) - 0.5f) * 0.7f
                    val speed = (1000f + rand01(62, i) * 900f) * u * (1f - 0.45f * dist / maxDist)
                    x = cx + cos(ang) * speed * ti
                    y = cy + sin(ang) * speed * ti - 380f * u * ti + 0.5f * 3300f * u * ti * ti
                    s = 1f + ti * 1.0f
                    rot = (rand01(63, i) - 0.5f) * 900f * ti
                    if (ti > 1.0f) continue   // long gone: keep the end frame clean
                    if (x < -bw * 3 || x > w + bw * 3 || y > h + bh * 3) continue
                }
                withTransform({ translate(x, y); rotate(rot, Offset.Zero); scale(s, s, Offset.Zero) }) {
                    val inset = 3f * u
                    val tl = Offset(-bw / 2 + inset, -bh / 2 + inset)
                    val sz = Size(bw - inset * 2, bh - inset * 2)
                    drawRoundRect(brickColors[(r * 3 + c * 7 + (rand01(64, i) * 5).toInt()) % brickColors.size], tl, sz, CornerRadius(5f * u))
                    drawRect(Color(0x30FFFFFF), tl, Size(sz.width, 6f * u))
                    drawRect(Color(0x28000000), Offset(tl.x, tl.y + sz.height - 6f * u), Size(sz.width, 6f * u))
                }
            }
        }

        // the ball and its chain
        val pivot = Offset(w * 0.90f, -0.08f * h)
        val windUp = easeInOut(seg(t, 0f, 0.20f))
        val swing = easeIn(seg(t, 0.20f, HIT))
        val ball: Offset
        val radius: Float
        if (t < HIT) {
            val rest = Offset(w * 0.86f, h * 0.55f)
            val back = Offset(w * 1.04f, h * 0.24f)
            val ctrl = Offset(w * 0.92f, h * 0.90f)
            val p0 = Offset(mix(rest.x, back.x, windUp), mix(rest.y, back.y, windUp))
            val q = 1f - swing
            ball = Offset(
                q * q * p0.x + 2 * q * swing * ctrl.x + swing * swing * impact.x,
                q * q * p0.y + 2 * q * swing * ctrl.y + swing * swing * impact.y,
            )
            radius = (mix(mix(100f, 64f, windUp), 340f, swing * swing)) * u
        } else {
            ball = Offset(impact.x + w * 0.02f * sec, impact.y + 0.5f * 3000f * u * sec * sec)
            radius = 340f * u * (1f + sec * 0.15f)
        }
        if (ball.y - radius < h * 1.2f) {
            chain(pivot, ball, u)
            drawCircle(Color(0x44000000), radius * 1.05f, Offset(ball.x + radius * 0.08f, ball.y + radius * 0.1f))
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFA3A9B5), Color(0xFF3A3E47), Color(0xFF0B0C10)),
                    Offset(ball.x - radius * 0.35f, ball.y - radius * 0.4f), radius * 1.45f,
                ),
                radius, ball,
            )
            drawOval(Color(0x88FFFFFF), Offset(ball.x - radius * 0.55f, ball.y - radius * 0.65f), Size(radius * 0.4f, radius * 0.22f))
        }

        // dust + flash + lettering
        if (t >= HIT) {
            val p = easeOut(seg(t, HIT, HIT + 0.22f))
            val a = 0.5f * (1f - seg(t, HIT + 0.05f, HIT + 0.30f))
            if (a > 0f) for (i in 0 until 14) {
                val ang = rand01(71, i) * 6.2832f
                val d = (120f + rand01(72, i) * 520f) * u * p
                drawCircle(Color(0xFFCDBFAE).copy(alpha = a), (70f + rand01(73, i) * 110f) * u * p, Offset(impact.x + cos(ang) * d, impact.y + sin(ang) * d * 0.7f))
            }
            val f = 0.8f * (1f - seg(t, HIT, HIT + 0.07f))
            if (f > 0f) drawRect(Color.White.copy(alpha = f))
            comicText(
                "BOOM!", w * 0.5f, h * 0.24f, 260f * u,
                angle = -6f, scale = popScale(seg(t, HIT, HIT + 0.06f)), alpha = 1f - seg(t, 0.68f, 0.78f),
                fill = 0xFFFFA726.toInt(), stroke = 0xFF7F1D1D.toInt(),
            )
        }
    }

    private fun DrawScope.chain(from: Offset, to: Offset, u: Float) {
        drawLine(Color(0xFF22252B), from, to, 16f * u, StrokeCap.Round)
        drawLine(
            Color(0xFF9AA1AD), from, to, 9f * u,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f * u, 12f * u)),
        )
        drawCircle(Color(0xFFFFC107), 26f * u, from.copy(y = 0f))
    }
}
