package app.marbles.ui.jar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import app.marbles.data.Kind
import app.marbles.ui.components.Look
import app.marbles.ui.components.jarPath
import app.marbles.ui.components.marble
import app.marbles.ui.theme.U
import app.marbles.ui.theme.color
import kotlin.math.sqrt
import kotlin.random.Random

/** Where each marble lies in the jar, from the bottom up. */
class Packing(val r: Float, val centers: List<Offset>)

object Pack {
    /**
     * Lays [count] marbles in rows from the bottom of [body], each row shifted half a marble, as wide
     * as the glass is at that height. The marbles shrink until all of them fit.
     */
    fun pack(count: Int, body: Rect, shoulder: Float, neck: Float, minR: Float = 1.5f, maxR: Float = 40f): Packing {
        if (count <= 0) return Packing(maxR, emptyList())
        val area = body.width * body.height * 0.9f
        var r = sqrt(area * 0.86f / (count * 2f * sqrt(3f))).coerceIn(minR, maxR)
        repeat(40) {
            val got = layout(count, body, shoulder, neck, r)
            if (got.size >= count || r <= minR) return Packing(r, got.take(count))
            r *= 0.96f
        }
        return Packing(r, layout(count, body, shoulder, neck, r).take(count))
    }

    /** The glass's half-width at height [y], from the centre line. */
    private fun halfWidth(y: Float, body: Rect, shoulder: Float, neck: Float): Float {
        val w = body.width / 2
        val corner = body.width * 0.16f
        val sh = body.top + body.height * (shoulder + 0.06f)
        return when {
            y > body.bottom - corner -> {
                val dy = y - (body.bottom - corner)
                w - corner + sqrt((corner * corner - dy * dy).coerceAtLeast(0f))
            }
            y >= sh -> w
            else -> {
                val t = ((y - body.top) / (sh - body.top)).coerceIn(0f, 1f)
                w * neck + (w - w * neck) * sqrt(t)
            }
        }
    }

    private fun layout(count: Int, body: Rect, shoulder: Float, neck: Float, r: Float): List<Offset> {
        val out = ArrayList<Offset>(count)
        val cx = body.center.x
        var y = body.bottom - r
        var row = 0
        val rnd = Random(7)
        while (y - r >= body.top + body.height * 0.04f && out.size < count) {
            val half = minOf(halfWidth(y - r * 0.7f, body, shoulder, neck), halfWidth(y + r * 0.7f, body, shoulder, neck)) - r
            if (half >= 0) {
                val shift = if (row % 2 == 1) r else 0f
                val fit = ((half * 2 - shift) / (r * 2)).toInt() + 1
                val n = minOf(fit, count - out.size)
                // The last, partial row lies in the middle, the way marbles settle.
                var x = if (n < fit) cx - (n - 1) * r else cx - half + shift
                repeat(n) {
                    // A little jostle, so it looks poured rather than stacked.
                    out += Offset(x + (rnd.nextFloat() - 0.5f) * r * 0.12f, y + (rnd.nextFloat() - 0.5f) * r * 0.1f)
                    x += r * 2
                }
            }
            y -= r * sqrt(3f)
            row++
        }
        return out
    }
}

/**
 * The jar of Saturdays still to come: [left] marbles in clear glass, a lid in the child's colour, and
 * the lamp's light falling on it.
 */
@Composable
fun JarView(left: Int, lid: Color, modifier: Modifier = Modifier, seed: Long = 1) {
    val p = U.c
    Spacer(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val jarW = minOf(w * 0.86f, h * 0.7f)
            val lidH = h * 0.065f
            val neck = 0.62f
            val shoulder = 0.16f
            val outer = Rect(Offset((w - jarW) / 2, lidH * 1.3f), Size(jarW, h - lidH * 1.3f - h * 0.035f))
            val glass = jarPath(outer, neck, shoulder)
            val t = jarW * 0.035f
            val inner = Rect(outer.left + t, outer.top + t * 1.4f, outer.right - t, outer.bottom - t)
            val pack = Pack.pack(left, inner, shoulder, neck)
            val kinds = Kind.entries
            val rnd = Random(seed)
            val looks = List(pack.centers.size) { Look(kinds[rnd.nextInt(kinds.size)].color(), 0.62f + rnd.nextFloat() * 0.3f, rnd.nextFloat()) }
            val neckW = jarW * neck
            val lidRect = Rect(Offset((w - neckW * 1.08f) / 2, lidH * 0.35f), Size(neckW * 1.08f, lidH))
            onDrawBehind {
                // The lamp's circle of light on the table.
                drawOval(
                    Brush.radialGradient(listOf(p.lamp.copy(alpha = 0.16f), Color.Transparent), center = Offset(w / 2, h * 0.55f), radius = w * 0.62f),
                    topLeft = Offset(0f, h * 0.05f), size = Size(w, h),
                )
                drawOval(Color.Black.copy(alpha = 0.35f), topLeft = Offset(outer.left - jarW * 0.02f, outer.bottom - h * 0.018f), size = Size(jarW * 1.04f, h * 0.05f))
                clipPath(glass) {
                    drawRect(p.glass.copy(alpha = 0.05f))
                    // Back to front: the bottom rows are nearer.
                    for (i in pack.centers.indices.reversed()) marble(pack.centers[i], pack.r, looks[i], shadow = false, detail = pack.r > 6f)
                    // The glass catches the lamp: a long highlight on the left, a faint one on the right.
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0f)), startX = outer.left + jarW * 0.07f, endX = outer.left + jarW * 0.2f),
                        topLeft = Offset(outer.left + jarW * 0.07f, outer.top + outer.height * 0.2f), size = Size(jarW * 0.13f, outer.height * 0.68f),
                    )
                    drawRect(
                        Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0f)), startX = outer.right - jarW * 0.14f, endX = outer.right - jarW * 0.06f),
                        topLeft = Offset(outer.right - jarW * 0.14f, outer.top + outer.height * 0.25f), size = Size(jarW * 0.08f, outer.height * 0.6f),
                    )
                }
                drawPath(glass, p.glass.copy(alpha = 0.55f), style = Stroke(width = t * 0.55f))
                drawPath(glass, Color.White.copy(alpha = 0.12f), style = Stroke(width = t * 0.18f))
                // The lid: a band of the child's colour with a rim.
                drawRoundRect(
                    Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(lid, Color.White, 0.25f), lid, androidx.compose.ui.graphics.lerp(lid, Color.Black, 0.35f)), startY = lidRect.top, endY = lidRect.bottom),
                    topLeft = lidRect.topLeft, size = lidRect.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(lidH * 0.3f),
                )
                for (k in 1..3) {
                    val y = lidRect.top + lidRect.height * k / 4f
                    drawLine(Color.Black.copy(alpha = 0.18f), Offset(lidRect.left + lidH * 0.2f, y), Offset(lidRect.right - lidH * 0.2f, y), strokeWidth = lidH * 0.05f)
                }
            }
        }
    )
}
