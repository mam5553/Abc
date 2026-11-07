package com.abc.tvlauncher.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object Pal {
    val Ink = Color(0xFF05060B)
    val Text = Color.White
    val TextDim = Color(0xB3FFFFFF)
    val TextFaint = Color(0x80FFFFFF)
    val Glow = Color(0xFFA8C8FF)
    val Live = Color(0xFFFF4D5A)
    val Good = Color(0xFF4ADE80)
    val Warn = Color(0xFFFBBF24)
    val Bad = Color(0xFFFF5C6C)
    val Xbox = Color(0xFF3DDC5A)
}

/** Dark "blurred wallpaper": a few big soft colour blobs on near-black. Drawn once, cached. */
fun Modifier.launcherBackground(): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val blobs = listOf(
        Brush.radialGradient(listOf(Color(0x663A5BFF), Color.Transparent), Offset(w * 0.12f, h * 0.05f), w * 0.65f),
        Brush.radialGradient(listOf(Color(0x55B03CFF), Color.Transparent), Offset(w * 0.92f, h * 0.30f), w * 0.55f),
        Brush.radialGradient(listOf(Color(0x4400C2C7), Color.Transparent), Offset(w * 0.45f, h * 1.05f), w * 0.70f),
        Brush.radialGradient(listOf(Color(0x33FF4D8D), Color.Transparent), Offset(w * 0.02f, h * 0.85f), w * 0.40f),
    )
    val vignette = Brush.verticalGradient(listOf(Color(0x00000000), Color(0x66000000)))
    onDrawBehind {
        drawRect(Pal.Ink)
        blobs.forEach { drawRect(it) }
        drawRect(vignette)
    }
}

@Composable
fun GText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Medium,
    color: Color = Pal.Text,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = size, fontWeight = weight, textAlign = align),
    )
}
