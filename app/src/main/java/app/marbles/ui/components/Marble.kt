package app.marbles.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marbles.ui.theme.U
import kotlin.math.cos
import kotlin.math.sin

/** How a marble looks: its colour, how much of it is lit (0 grey .. 1 full), and the swirl inside. */
data class Look(val color: Color, val fill: Float = 1f, val swirl: Float = 0f)

private val Grey = Color(0xFF5E5550)

/** The marble's own colour after its fill: grey glass slowly tinted toward the full colour. */
fun tint(l: Look): Color = if (l.fill <= 0f) Grey else lerp(lerp(Grey, l.color, 0.35f), l.color, l.fill)

/**
 * A glass marble: a body darker at the rim, a twist of lighter colour inside (a cat's eye), the lamp
 * reflected as a soft highlight up and to the left, and a small shadow on the table.
 */
fun DrawScope.marble(center: Offset, r: Float, look: Look, shadow: Boolean = true, detail: Boolean = r > 7f) {
    val c = tint(look)
    if (shadow) drawOval(Color.Black.copy(alpha = 0.28f), topLeft = Offset(center.x - r * 0.9f, center.y + r * 0.62f), size = androidx.compose.ui.geometry.Size(r * 1.9f, r * 0.55f))
    drawCircle(
        Brush.radialGradient(
            0f to lerp(c, Color.White, 0.28f),
            0.55f to c,
            1f to lerp(c, Color.Black, 0.5f),
            center = Offset(center.x - r * 0.25f, center.y - r * 0.3f),
            radius = r * 1.25f,
        ),
        radius = r, center = center,
    )
    if (detail) {
        // The twist: two thin crescents of lighter glass.
        val a = look.swirl * 6.283f
        val p = Path()
        for (k in 0..1) {
            val base = a + k * 3.1416f
            p.reset()
            p.moveTo(center.x + cos(base) * r * 0.15f, center.y + sin(base) * r * 0.15f)
            p.quadraticTo(
                center.x + cos(base + 0.9f) * r * 0.95f, center.y + sin(base + 0.9f) * r * 0.95f,
                center.x + cos(base + 1.7f) * r * 0.62f, center.y + sin(base + 1.7f) * r * 0.62f,
            )
            drawPath(p, lerp(c, Color.White, 0.45f).copy(alpha = 0.32f), style = Stroke(width = r * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
    drawCircle(Color.White.copy(alpha = 0.10f), radius = r, center = center, style = Stroke(width = r * 0.07f))
    drawOval(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0f)), center = Offset(center.x - r * 0.38f, center.y - r * 0.42f), radius = r * 0.42f),
        topLeft = Offset(center.x - r * 0.72f, center.y - r * 0.72f), size = androidx.compose.ui.geometry.Size(r * 0.7f, r * 0.58f),
    )
}

/** A marble on its own, for lists and buttons. */
@Composable
fun MarbleDot(look: Look, modifier: Modifier = Modifier, size: Dp = 22.dp, shadow: Boolean = false) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2 * (if (shadow) 0.82f else 0.96f)
        marble(Offset(this.size.width / 2, if (shadow) r * 1.02f else this.size.height / 2), r, look, shadow = shadow, detail = r > 9f)
    }
}

/** An empty marble-shaped hollow: a week still to come. */
@Composable
fun Hollow(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val line = U.c.line
    Canvas(modifier.size(size)) {
        drawCircle(line, radius = this.size.minDimension / 2 * 0.86f, style = Stroke(width = 1.4f * density, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f * density, 3f * density))))
    }
}

/** The outline of the jar's glass body, inside [r]: a wide shoulder, a neck, a flat bottom. */
fun jarPath(r: Rect, neck: Float = 0.62f, shoulder: Float = 0.16f): Path {
    val w = r.width
    val h = r.height
    val nx = (w - w * neck) / 2
    val top = r.top
    val sh = top + h * shoulder
    val corner = w * 0.16f
    return Path().apply {
        moveTo(r.left + nx, top)
        lineTo(r.right - nx, top)
        cubicTo(r.right - nx, sh * 0.6f + top * 0.4f, r.right, sh - h * 0.04f, r.right, sh + h * 0.06f)
        lineTo(r.right, r.bottom - corner)
        quadraticTo(r.right, r.bottom, r.right - corner, r.bottom)
        lineTo(r.left + corner, r.bottom)
        quadraticTo(r.left, r.bottom, r.left, r.bottom - corner)
        lineTo(r.left, sh + h * 0.06f)
        cubicTo(r.left, sh - h * 0.04f, r.left + nx, sh * 0.6f + top * 0.4f, r.left + nx, top)
        close()
    }
}
