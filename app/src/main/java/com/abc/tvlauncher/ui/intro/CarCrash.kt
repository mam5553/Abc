package com.abc.tvlauncher.ui.intro

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * A cartoon car screams across a dark road, smashes through the "screen" in a flash of glass,
 * and the home screen drops in from above and bounces into place.
 */
object CarCrash : Intro("car_crash", "Car crash", 2800) {
    private const val IMPACT = 0.36f
    private const val FALL_START = 0.40f
    private const val FALL_END = 0.80f
    private const val LAND = FALL_START + (FALL_END - FALL_START) * (1f / 2.75f)

    override fun homeLayer(t: Float): HomeLayer {
        if (t < FALL_START) return HomeLayer(alpha = 0f)
        val p = seg(t, FALL_START, FALL_END)
        val shake = seg(t, LAND, LAND + 0.16f)
        return HomeLayer(
            dy = -1.2f * (1f - bounceOut(p)),
            rotation = (1f - p) * -6f,
            dx = if (t > LAND) sin(shake * 40f) * (1f - shake) * 0.006f else 0f,
        )
    }

    private fun carX(t: Float, w: Float): Float = mix(-0.3f * w, 1.45f * w, (t / 0.6f).coerceIn(0f, 1f).pow(1.5f))

    override fun DrawScope.drawBackground(t: Float) {
        val u = size.height / 1080f
        val w = size.width
        val h = size.height
        // impact shake
        val sh = seg(t, IMPACT, IMPACT + 0.12f)
        val sx = if (t > IMPACT) sin(sh * 60f) * (1f - sh) * 14f * u else 0f
        withTransform({ translate(sx, 0f) }) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF232A5C), Color(0xFF0B0C1C))))
            drawCircle(Color(0x33FFFFFF), 200f * u, Offset(w * 0.8f, h * 0.2f))
            drawCircle(Color(0xFFF5F1D6), 84f * u, Offset(w * 0.8f, h * 0.2f))
            val roadY = h * 0.72f
            drawRect(Color(0xFF14151F), Offset(-40f, roadY), Size(w + 80f, h - roadY))
            drawRect(Color(0xFF2A2C3C), Offset(-40f, roadY), Size(w + 80f, 8f * u))
            val dash = 130f * u
            val gap = 110f * u
            val off = -((t * 7000f * u) % (dash + gap))
            var x = off
            while (x < w) {
                drawRect(Color(0xFFE9E3A5), Offset(x, roadY + 150f * u), Size(dash, 14f * u))
                x += dash + gap
            }
        }
    }

    override fun DrawScope.drawForeground(t: Float) {
        val u = size.height / 1080f
        val w = size.width
        val h = size.height
        val roadY = h * 0.72f
        val groundY = roadY + 118f * u
        val carX = carX(t, w)
        val impact = Offset(w * 0.5f + 215f * u, groundY - 100f * u)
        val after = t >= IMPACT

        // TV-glass frame, before the crash
        if (t < IMPACT + 0.05f) {
            val a = 1f - seg(t, IMPACT, IMPACT + 0.05f)
            drawRoundRect(
                Color.White.copy(alpha = 0.55f * a), Offset(16f * u, 16f * u), Size(w - 32f * u, h - 32f * u),
                CornerRadius(30f * u), style = Stroke(6f * u),
            )
            val sheen = Path().apply {
                moveTo(w * 0.22f, 0f); lineTo(w * 0.30f, 0f); lineTo(w * 0.10f, h); lineTo(w * 0.02f, h); close()
            }
            drawPath(sheen, Color.White.copy(alpha = 0.07f * a))
        }

        // speed lines
        if (t < 0.62f) {
            for (i in 0 until 8) {
                val y = h * (0.22f + 0.07f * i)
                val len = (220f + rand01(3, i) * 380f) * u
                val x0 = carX - 230f * u - rand01(5, i) * 260f * u
                drawLine(Color.White.copy(alpha = 0.28f), Offset(x0, y), Offset(x0 - len, y), 5f * u, StrokeCap.Round)
            }
        }

        // cracks
        if (after) {
            val g = easeOut(seg(t, IMPACT, IMPACT + 0.06f))
            val a = 1f - seg(t, IMPACT + 0.12f, IMPACT + 0.30f)
            if (a > 0f) for (k in 0 until 11) {
                val ang = (k / 11f) * 2f * PI.toFloat() + rand01(11, k) * 0.4f
                val len = (0.30f + rand01(12, k) * 0.55f) * w * g
                val path = Path().apply {
                    moveTo(impact.x, impact.y)
                    for (s in 1..6) {
                        val f = s / 6f
                        val jitter = (rand01(13 + k, s) - 0.5f) * 60f * u
                        val px = impact.x + cos(ang) * len * f - sin(ang) * jitter
                        val py = impact.y + sin(ang) * len * f + cos(ang) * jitter
                        lineTo(px, py)
                    }
                }
                drawPath(path, Color.Black.copy(alpha = 0.5f * a), style = Stroke(8f * u, cap = StrokeCap.Round))
                drawPath(path, Color.White.copy(alpha = 0.9f * a), style = Stroke(3.5f * u, cap = StrokeCap.Round))
            }
        }

        // car
        if (carX < w + 300f * u) {
            val tilt = if (after) -9f * seg(t, IMPACT, IMPACT + 0.05f) * (1f - seg(t, IMPACT + 0.05f, 0.58f)) else 0f
            drawCar(carX, groundY, u, tilt, wheelDeg = carX / (46f * u) * 57.3f)
        }

        // glass shards
        if (after) {
            val sec = (t - IMPACT) * 2.8f
            for (i in 0 until 30) {
                val ang = rand01(21, i) * 2f * PI.toFloat()
                val sp = (500f + rand01(22, i) * 1500f) * u
                val x = impact.x + cos(ang) * sp * sec
                val y = impact.y + sin(ang) * sp * sec + 0.5f * 2600f * u * sec * sec
                val a = 1f - seg(sec, 0.45f, 1.1f)
                if (a <= 0f) continue
                val s = (26f + rand01(23, i) * 60f) * u * (1f + sec * 0.6f)
                val rot = (rand01(24, i) - 0.5f) * 1600f * sec
                withTransform({ translate(x, y); rotate(rot, Offset.Zero) }) {
                    val p = Path().apply { moveTo(0f, -s); lineTo(s * 0.8f, s * 0.6f); lineTo(-s * 0.7f, s * 0.5f); close() }
                    drawPath(p, Color(0xFFBFE9FF).copy(alpha = 0.55f * a))
                    drawPath(p, Color.White.copy(alpha = 0.9f * a), style = Stroke(2.5f * u))
                }
            }
        }

        // dust where the screen lands
        if (t > LAND) {
            val p = easeOut(seg(t, LAND, LAND + 0.14f))
            val a = 0.5f * (1f - seg(t, LAND + 0.03f, LAND + 0.20f))
            if (a > 0f) for (i in 0 until 12) {
                val x = w * (0.02f + 0.087f * i)
                val r = (50f + rand01(31, i) * 90f) * u * p
                drawCircle(Color(0xFFD8D0C2).copy(alpha = a), r, Offset(x, h - 6f * u - rand01(32, i) * 30f * u))
            }
        }

        // flash + lettering
        if (after) {
            val f = 0.85f * (1f - seg(t, IMPACT, IMPACT + 0.07f))
            if (f > 0f) drawRect(Color.White.copy(alpha = f))
            val p = seg(t, IMPACT, IMPACT + 0.06f)
            comicText(
                "CRASH!", w * 0.5f, h * 0.30f, 250f * u,
                angle = -9f, scale = popScale(p), alpha = 1f - seg(t, 0.58f, 0.68f),
            )
        }
    }

    /** Origin = ground contact under the middle of the car. +x is forward. */
    private fun DrawScope.drawCar(x: Float, groundY: Float, u: Float, tiltDeg: Float, wheelDeg: Float) {
        withTransform({
            translate(x, groundY)
            rotate(tiltDeg, Offset(-140f * u, 0f))
            scale(u, u, Offset.Zero)
        }) {
            // shadow
            drawOval(Color(0x55000000), Offset(-230f, -14f), Size(460f, 32f))
            // body
            val red = Color(0xFFE53935)
            drawRoundRect(red, Offset(-215f, -140f), Size(430f, 92f), CornerRadius(34f))
            drawRoundRect(Color(0x33FFFFFF), Offset(-205f, -136f), Size(410f, 22f), CornerRadius(12f))
            // cabin
            val cabin = Path().apply {
                moveTo(-115f, -138f); lineTo(-72f, -210f); lineTo(66f, -210f); lineTo(126f, -138f); close()
            }
            drawPath(cabin, Color(0xFFC62828))
            val win = Path().apply {
                moveTo(-92f, -146f); lineTo(-60f, -198f); lineTo(-10f, -198f); lineTo(-10f, -146f); close()
            }
            val win2 = Path().apply {
                moveTo(2f, -146f); lineTo(2f, -198f); lineTo(58f, -198f); lineTo(102f, -146f); close()
            }
            drawPath(win, Color(0xFFBFE6FF)); drawPath(win2, Color(0xFFBFE6FF))
            drawLine(Color(0x99FFFFFF), Offset(-48f, -192f), Offset(-72f, -152f), 8f, StrokeCap.Round)
            // googly eyes in the windshield
            for (ex in listOf(30f, 74f)) {
                drawCircle(Color.White, 19f, Offset(ex, -172f))
                drawCircle(Color(0xFF1A1A1A), 19f, Offset(ex, -172f), style = Stroke(3f))
                drawCircle(Color(0xFF111111), 9f, Offset(ex + 6f, -170f))
            }
            // door line, bumpers, lights
            drawLine(Color(0x55000000), Offset(-20f, -136f), Offset(-20f, -52f), 4f)
            drawRoundRect(Color(0xFFD7DCE3), Offset(196f, -96f), Size(34f, 36f), CornerRadius(12f))
            drawRoundRect(Color(0xFFD7DCE3), Offset(-232f, -96f), Size(30f, 36f), CornerRadius(12f))
            drawCircle(Color(0x66FFF59D), 34f, Offset(188f, -108f))
            drawCircle(Color(0xFFFFF176), 16f, Offset(188f, -108f))
            drawRoundRect(Color(0xFFFF8A65), Offset(-214f, -118f), Size(14f, 26f), CornerRadius(5f))
            // wheels
            for (wx in listOf(-142f, 142f)) {
                withTransform({ translate(wx, -46f); rotate(wheelDeg, Offset.Zero) }) {
                    drawCircle(Color(0xFF15151A), 48f)
                    drawCircle(Color(0xFFB8BEC8), 26f)
                    for (s in 0 until 5) {
                        val a = s * 2f * PI.toFloat() / 5f
                        drawLine(Color(0xFF6B7280), Offset.Zero, Offset(cos(a) * 24f, sin(a) * 24f), 5f, StrokeCap.Round)
                    }
                    drawCircle(Color(0xFF3A3F4A), 7f)
                }
            }
        }
    }
}
