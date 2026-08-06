package com.notesnync.app.branding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notesnync.app.R
import com.notesnync.app.domain.ThemeProfile

/**
 * The Notes'nync mark, faithful to notes_nync_v4_adaptive.html: a neumorphic rounded box
 * (adaptive light/dark) holding a gradient "N" that draws itself, a page-fold, and note lines.
 * Colours come from the active [palette].
 */
@Composable
fun AnimatedNotesNyncLogo(
    size: Dp = 54.dp,
    palette: BrandPalette = ThemeProfile.Neon.palette(),
    dark: Boolean = isSystemInDarkTheme(),
    animate: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // Draw-on plays exactly once (1.5s) then holds on the finished mark — no restart loop,
    // so the settled icon is visible for the rest of the splash.
    val draw = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (animate) {
            draw.snapTo(0f)
            draw.animateTo(1f, tween(1400, delayMillis = 100, easing = LinearEasing))
        } else {
            draw.snapTo(1f)
        }
    }
    // A gentle, continuous breathe for the ambient glow only (not the stroke reveal).
    val transition = rememberInfiniteTransition(label = "logo")
    val breathe by transition.animateFloat(
        0.985f, 1.03f,
        infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val t = if (animate) draw.value else 1f
    val corner = size * 0.26f
    val shape = RoundedCornerShape(corner)

    Box(
        modifier
            .size(size)
            .shadow(elevation = size * 0.11f, shape = shape, clip = false)
            .clip(shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val r = w * 0.26f

            // Neumorphic box background (145deg gradient)
            val boxColors = if (dark) listOf(Color(0xFF1E1440), Color(0xFF0E0A24))
            else listOf(Color(0xFFFFFFFF), Color(0xFFF3EFFB))
            drawRoundRect(
                brush = Brush.linearGradient(boxColors, Offset(0f, 0f), Offset(w, h)),
                cornerRadius = CornerRadius(r, r),
            )
            // Inner top highlight (the inset 3D lift)
            drawRoundRect(
                color = Color.White.copy(alpha = if (dark) 0.05f else 0.6f),
                size = Size(w, h * 0.46f),
                cornerRadius = CornerRadius(r, r),
            )
            // Ambient palette glow
            drawCircle(palette.c1.copy(alpha = if (dark) 0.20f else 0.14f), w * 0.30f * breathe, Offset(w * 0.36f, h * 0.30f))
            drawCircle(palette.c4.copy(alpha = if (dark) 0.16f else 0.10f), w * 0.24f * breathe, Offset(w * 0.70f, h * 0.34f))

            // "N": one connected stroke p1->p2->p3->p4
            val p1 = Offset(w * 0.26f, h * 0.76f)
            val p2 = Offset(w * 0.26f, h * 0.24f)
            val p3 = Offset(w * 0.60f, h * 0.76f)
            val p4 = Offset(w * 0.60f, h * 0.24f)
            val sw = w * 0.155f
            val nBrush = Brush.linearGradient(
                palette.gradient,
                Offset(w * 0.22f, h * 0.20f),
                Offset(w * 0.64f, h * 0.80f),
            )
            seg(p1, p2, span(t, 0.00f, 0.22f), sw, nBrush)
            seg(p2, p3, span(t, 0.18f, 0.46f), sw, nBrush)
            seg(p3, p4, span(t, 0.44f, 0.64f), sw, nBrush)

            // Three note lines
            listOf(0.35f to listOf(palette.c1, palette.c2), 0.47f to listOf(palette.c2, palette.c3), 0.59f to listOf(palette.c3, palette.c4))
                .forEachIndexed { i, (yf, cs) ->
                    val reveal = span(t, 0.66f + i * 0.07f, 0.86f + i * 0.07f)
                    if (reveal > 0f) {
                        val y = h * yf
                        val x0 = w * 0.66f + (1f - reveal) * w * 0.16f
                        drawLine(
                            brush = Brush.linearGradient(cs, Offset(x0, y), Offset(w * 0.82f, y)),
                            start = Offset(x0, y),
                            end = Offset(w * 0.82f, y),
                            strokeWidth = w * 0.052f,
                            cap = StrokeCap.Round,
                            alpha = reveal,
                        )
                    }
                }
        }
    }
}

@Composable
fun NotesNyncCutoutLogo(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.notesnync_cutout),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.seg(
    start: Offset,
    end: Offset,
    progress: Float,
    stroke: Float,
    brush: Brush,
) {
    if (progress <= 0f) return
    val cur = Offset(start.x + (end.x - start.x) * progress, start.y + (end.y - start.y) * progress)
    drawLine(brush = brush, start = start, end = cur, strokeWidth = stroke, cap = StrokeCap.Round)
}

private fun span(value: Float, start: Float, end: Float): Float =
    ((value - start) / (end - start)).coerceIn(0f, 1f)
