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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * The screen is a dark, moonlit cover. A ninja dashes across it, a glowing slash appears,
 * and the cover splits along the cut and slides away to reveal the home screen.
 */
object NinjaSlice : Intro("ninja", "Ninja slice", 2700) {
    private const val SLASH = 0.34f

    override fun homeLayer(t: Float): HomeLayer =
        HomeLayer(scale = mix(1.08f, 1f, easeOut(seg(t, SLASH, 0.85f))))

    override fun DrawScope.drawForeground(t: Float) {
        val u = size.height / 1080f
        val w = size.width
        val h = size.height
        val a = Offset(-0.05f * w, 0.16f * h)
        val b = Offset(1.05f * w, 0.84f * h)
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = sqrt(dx * dx + dy * dy)
        val ux = dx / len
        val uy = dy / len
        val nx = -uy      // normal, pointing "down" the screen
        val ny = ux
        fun lineY(x: Float) = a.y + dy * (x - a.x) / dx

        // ---- the cover, in two halves --------------------------------------------------
        val split = easeIn(seg(t, SLASH + 0.02f, 0.80f))
        val off = split * h * 1.05f
        val upper = Path().apply { moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, lineY(w)); lineTo(0f, lineY(0f)); close() }
        val lower = Path().apply { moveTo(0f, lineY(0f)); lineTo(w, lineY(w)); lineTo(w, h); lineTo(0f, h); close() }
        if (split < 1f) {
            translate(-nx * off, -ny * off) { cover(upper, u, w, h) }
            translate(nx * off, ny * off) { cover(lower, u, w, h) }
        }

        // ---- the ninja -----------------------------------------------------------------
        val p = easeIn(seg(t, 0.10f, SLASH))
        if (p > 0f && p < 1f) {
            val s = mix(-0.18f, 1.22f, p)
            for (g in 2 downTo 0) {
                val sg = s - g * 0.055f
                val pos = Offset(a.x + dx * sg, a.y + dy * sg)
                drawNinja(pos, atan2(dy, dx) * 57.2958f, u, alpha = if (g == 0) 1f else 0.28f / g)
            }
            // light trail
            val tail = Offset(a.x + dx * (s - 0.45f).coerceAtLeast(-0.2f), a.y + dy * (s - 0.45f).coerceAtLeast(-0.2f))
            drawLine(
                Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.8f)), tail, Offset(a.x + dx * s, a.y + dy * s)),
                tail, Offset(a.x + dx * s, a.y + dy * s), 6f * u, StrokeCap.Round,
            )
        }

        // ---- the slash -----------------------------------------------------------------
        if (t >= SLASH) {
            val fade = 1f - seg(t, SLASH + 0.02f, 0.64f)
            if (fade > 0f) {
                val wd = (16f * fade + 3f) * u
                drawLine(Color(0xFF66E0FF).copy(alpha = 0.22f * fade), a, b, wd * 6f, StrokeCap.Round)
                drawLine(Color(0xFFA8F0FF).copy(alpha = 0.55f * fade), a, b, wd * 2.6f, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = fade), a, b, wd, StrokeCap.Round)
            }
            val flash = 0.55f * (1f - seg(t, SLASH, SLASH + 0.08f))
            if (flash > 0f) drawRect(Color.White.copy(alpha = flash))
            comicText(
                "SHHK!", w * 0.5f, h * 0.22f, 200f * u,
                angle = 14f, scale = popScale(seg(t, SLASH, SLASH + 0.06f)),
                alpha = 1f - seg(t, 0.56f, 0.68f), fill = 0xFFFFFFFF.toInt(), stroke = 0xFF2A3AA8.toInt(),
            )
        }
    }

    private fun DrawScope.cover(shape: Path, u: Float, w: Float, h: Float) {
        clipPath(shape) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF34457A), Color(0xFF151B33), Color(0xFF0B0E1C)), 0f, h))
            drawCircle(Color(0x22FFFFFF), 220f * u, Offset(w * 0.84f, h * 0.18f))
            drawCircle(Color(0xFFF2F4FF), 92f * u, Offset(w * 0.84f, h * 0.18f))
            for (i in 0 until 46) {
                val r = (1.5f + rand01(41, i) * 2.6f) * u
                drawCircle(Color.White.copy(alpha = 0.35f + rand01(43, i) * 0.5f), r, Offset(rand01(42, i) * w, rand01(44, i) * h * 0.8f))
            }
        }
    }

    /** Drawn facing +x, origin at the torso. */
    private fun DrawScope.drawNinja(pos: Offset, angleDeg: Float, u: Float, alpha: Float) {
        val ink = Color(0xFF05060C).copy(alpha = alpha)
        val red = Color(0xFFE23B3B).copy(alpha = alpha)
        withTransform({ translate(pos.x, pos.y); rotate(angleDeg, Offset.Zero); scale(u, u, Offset.Zero) }) {
            // trailing legs + back arm
            drawLine(ink, Offset(-50f, 18f), Offset(-150f, 52f), 30f, StrokeCap.Round)
            drawLine(ink, Offset(-40f, 24f), Offset(-128f, 100f), 30f, StrokeCap.Round)
            drawLine(ink, Offset(-30f, -8f), Offset(-104f, 22f), 24f, StrokeCap.Round)
            // torso + head
            drawRoundRect(ink, Offset(-70f, -36f), Size(124f, 74f), CornerRadius(37f))
            drawCircle(ink, 36f, Offset(70f, -12f))
            // eye slit
            if (alpha > 0.9f) {
                drawRoundRect(Color(0xFFF1C9A2), Offset(56f, -26f), Size(34f, 18f), CornerRadius(8f))
                drawCircle(Color.White, 5.5f, Offset(66f, -17f)); drawCircle(Color.White, 5.5f, Offset(82f, -17f))
                drawCircle(Color.Black, 2.6f, Offset(68f, -17f)); drawCircle(Color.Black, 2.6f, Offset(84f, -17f))
                drawLine(Color.Black, Offset(60f, -27f), Offset(72f, -22f), 3.2f, StrokeCap.Round)
                drawLine(Color.Black, Offset(88f, -27f), Offset(78f, -22f), 3.2f, StrokeCap.Round)
            }
            // headband + flowing tails
            drawLine(red, Offset(38f, -34f), Offset(100f, -30f), 10f, StrokeCap.Round)
            val tail1 = Path().apply { moveTo(40f, -34f); cubicTo(-10f, -64f, -60f, -26f, -120f, -60f) }
            val tail2 = Path().apply { moveTo(40f, -30f); cubicTo(-6f, -46f, -50f, -8f, -104f, -30f) }
            drawPath(tail1, red, style = Stroke(9f, cap = StrokeCap.Round))
            drawPath(tail2, red, style = Stroke(9f, cap = StrokeCap.Round))
            // sword arm + katana
            drawLine(ink, Offset(26f, -4f), Offset(112f, 14f), 24f, StrokeCap.Round)
            drawLine(Color(0xFFDCE6F5).copy(alpha = alpha), Offset(112f, 14f), Offset(290f, -26f), 7f, StrokeCap.Round)
            drawLine(Color(0xFF7A4A2A).copy(alpha = alpha), Offset(96f, 18f), Offset(126f, 11f), 12f, StrokeCap.Round)
        }
    }
}
