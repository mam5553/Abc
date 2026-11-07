package com.abc.tvlauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * The launcher's one interactive building block: a frosted-glass tile that zooms and glows when
 * the remote focuses it.
 *
 *  - OK / Enter            -> [onClick] (fires on key-up)
 *  - hold OK (or Menu key) -> [onLongClick], if given
 *  - [onKey] gets first look at every key (used for "move app" mode)
 *
 * [glass] sizes the glass part; [label] (optional) sits underneath and zooms with it.
 */
@Composable
fun GlassTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glass: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onKey: ((KeyEvent) -> Boolean)? = null,
    focusRequester: FocusRequester? = null,
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = Color.White,
    glow: Color = Pal.Glow,
    zoom: Float = 0.12f,
    label: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val f = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "focus",
    )
    val press = remember { object { var armed = false; var longFired = false } }
    val currentClick by rememberUpdatedLambda(onClick)
    val currentLong by rememberUpdatedLambda(onLongClick)

    Column(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                val s = 1f + zoom * f.value
                scaleX = s; scaleY = s
            }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { ev ->
                if (onKey?.invoke(ev) == true) return@onPreviewKeyEvent true
                val k = ev.key
                val isOk = k == Key.DirectionCenter || k == Key.Enter || k == Key.NumPadEnter
                val longCb = currentLong
                when {
                    isOk && ev.type == KeyEventType.KeyDown -> {
                        val n = ev.nativeKeyEvent
                        if (n.repeatCount == 0) {
                            press.armed = true; press.longFired = false
                        } else if (press.armed && !press.longFired && longCb != null) {
                            press.longFired = true; longCb()
                        }
                        true
                    }
                    isOk && ev.type == KeyEventType.KeyUp -> {
                        val fire = press.armed && !press.longFired
                        press.armed = false; press.longFired = false
                        if (fire) currentClick()
                        true
                    }
                    k == Key.Menu && ev.type == KeyEventType.KeyDown && longCb != null -> { longCb(); true }
                    else -> false
                }
            }
            .focusable()
            .pointerInput(Unit) {   // touch / mouse (e.g. testing on a phone)
                detectTapGestures(onTap = { currentClick() }, onLongPress = { currentLong?.invoke() })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = glass
                .glassGlow(shape, glow, f)
                .clip(shape)
                .glassBody(shape, tint, f),
            content = content,
        )
        if (label != null) {
            Spacer(Modifier.height(10.dp))
            GText(
                label,
                size = 14.sp,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer { alpha = 0.62f + 0.38f * f.value },
                align = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun <T> rememberUpdatedLambda(v: T): State<T> {
    val s = remember { mutableStateOf(v) }
    s.value = v
    return s
}

/** Soft coloured halo that fades in with focus. */
private fun Modifier.glassGlow(shape: Shape, color: Color, f: State<Float>): Modifier = drawBehind {
    val a = f.value
    if (a <= 0.01f) return@drawBehind
    val pad = 36.dp.toPx()
    val br = Brush.radialGradient(
        colors = listOf(color.copy(alpha = 0.42f * a), color.copy(alpha = 0.14f * a), Color.Transparent),
        center = center,
        radius = maxOf(size.width, size.height) * 0.78f,
    )
    drawRoundRect(
        brush = br,
        topLeft = Offset(-pad, -pad),
        size = Size(size.width + pad * 2, size.height + pad * 2),
        cornerRadius = CornerRadius(pad + 24.dp.toPx()),
    )
}

/** Frosted fill + top highlight + hairline rim, all drawn in one pass. */
private fun Modifier.glassBody(shape: Shape, tint: Color, f: State<Float>): Modifier = drawBehind {
    val a = f.value
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(
        outline,
        Brush.verticalGradient(listOf(tint.copy(alpha = 0.20f + 0.12f * a), tint.copy(alpha = 0.07f + 0.08f * a))),
    )
    // specular band across the top
    drawOutline(
        outline,
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.16f + 0.10f * a),
            0.45f to Color.Transparent,
            endY = size.height,
        ),
    )
    drawOutline(
        outline,
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.55f + 0.3f * a), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.22f + 0.2f * a)),
            start = Offset.Zero, end = Offset(size.width, size.height),
        ),
        style = Stroke(width = 1.2.dp.toPx() + 0.8.dp.toPx() * a),
    )
}
