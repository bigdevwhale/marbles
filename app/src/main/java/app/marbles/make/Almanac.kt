package app.marbles.make

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import app.marbles.R
import app.marbles.data.Child
import app.marbles.data.Together
import app.marbles.data.Vault
import app.marbles.domain.Jar
import app.marbles.domain.Letter
import app.marbles.ui.T
import app.marbles.ui.components.titleText
import app.marbles.ui.theme.color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.time.YearMonth

/**
 * The almanac: a month or a year of time together as a small printable book (A5 PDF). A cover, the
 * numbers, then every month with its pictures and what the children said.
 */
class Almanac(private val context: Context, private val vault: Vault) {
    private val w = 420
    private val h = 595
    private val margin = 36f
    private val cream = Color.rgb(0xFB, 0xF5, 0xEA)
    private val ink = Color.rgb(0x2A, 0x22, 0x1C)
    private val muted = Color.rgb(0x7A, 0x6B, 0x5E)
    private val amber = Color.rgb(0xC9, 0x84, 0x22)
    private val serif: Typeface = ResourcesCompat.getFont(context, R.font.cormorant_semibold) ?: Typeface.SERIF
    private val italic: Typeface = ResourcesCompat.getFont(context, R.font.cormorant_italic) ?: Typeface.SERIF
    private val sans: Typeface = ResourcesCompat.getFont(context, R.font.manrope_regular) ?: Typeface.SANS_SERIF
    private val sansBold: Typeface = ResourcesCompat.getFont(context, R.font.manrope_bold) ?: Typeface.DEFAULT_BOLD
    private val hand: Typeface = ResourcesCompat.getFont(context, R.font.caveat) ?: Typeface.SERIF

    private fun paint(tf: Typeface, size: Float, color: Int = ink) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = size; this.color = color }

    private fun layout(text: String, p: TextPaint, width: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, width).setAlignment(align).setLineSpacing(0f, 1.08f).build()

    private fun Canvas.text(l: StaticLayout, x: Float, y: Float) { save(); translate(x, y); l.draw(this); restore() }

    private fun Canvas.marble(cx: Float, cy: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(cx - r * 0.3f, cy - r * 0.35f, r * 1.3f, intArrayOf(blend(color, Color.WHITE, 0.35f), color, blend(color, Color.BLACK, 0.45f)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        drawCircle(cx, cy, r, p)
        val hl = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(200, 255, 255, 255) }
        drawOval(RectF(cx - r * 0.62f, cy - r * 0.66f, cx - r * 0.1f, cy - r * 0.22f), hl)
    }

    private fun blend(a: Int, b: Int, t: Float) = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt(),
    )

    /** Writes the book for [letter] to a file in the share folder and returns it. */
    fun make(letter: Letter, children: List<Child>, title: String): File {
        val doc = PdfDocument()
        var n = 0
        fun page(): PdfDocument.Page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, ++n).create()).also { it.canvas.drawColor(cream) }

        // The cover.
        page().let { pg ->
            val cv = pg.canvas
            val tw = (w - margin * 2).toInt()
            cv.text(layout(context.getString(R.string.almanac_title).uppercase(), paint(sansBold, 9f, muted).apply { letterSpacing = 0.2f }, tw, Layout.Alignment.ALIGN_CENTER), margin, 120f)
            cv.text(layout(title, paint(serif, 40f), tw, Layout.Alignment.ALIGN_CENTER), margin, 140f)
            cv.text(layout(T.names(context, children), paint(italic, 20f, muted), tw, Layout.Alignment.ALIGN_CENTER), margin, 200f)
            // A small heap of the period's marbles, one per stretch of time together, up to forty.
            val heap = letter.entries.take(40)
            var i = 0
            var row = 0
            val r = 11f
            while (i < heap.size) {
                val inRow = (9 - row).coerceAtLeast(1)
                val y = 470f - row * r * 1.75f
                val x0 = w / 2f - (inRow - 1) * r
                for (k in 0 until inRow) {
                    if (i >= heap.size) break
                    cv.marble(x0 + k * r * 2, y, r, heap[i].kindOf.color().toArgb())
                    i++
                }
                row++
            }
            doc.finishPage(pg)
        }

        // The numbers.
        page().let { pg ->
            val cv = pg.canvas
            val tw = (w - margin * 2).toInt()
            var y = margin
            cv.text(layout(context.getString(R.string.almanac_numbers), paint(serif, 26f), tw), margin, y); y += 46f
            val big = paint(serif, 34f, amber)
            val small = paint(sans, 10f, muted)
            fun stat(v: String, label: String, x: Float) {
                cv.text(layout(v, big, 170), x, y)
                cv.text(layout(label, small, 170), x, y + 42f)
            }
            stat(T.span(context, letter.minutes), context.getString(R.string.letter_together), margin)
            stat(letter.days.toString(), context.getString(R.string.letter_days), margin + 180f)
            y += 80f
            letter.perChild.filter { it.second > 0 }.forEach { (k, m) ->
                cv.text(layout(k.name + " — " + T.span(context, m), paint(sans, 12f), tw), margin, y); y += 20f
            }
            y += 14f
            letter.kinds.forEach { (k, m) ->
                cv.marble(margin + 8f, y + 8f, 7f, k.color().toArgb())
                cv.text(layout(T.kind(context, k) + " · " + T.span(context, m), paint(sans, 11f), tw - 24), margin + 24f, y); y += 22f
            }
            if (letter.evenings > 0) {
                y += 16f
                cv.text(layout(context.getString(R.string.almanac_mirror, letter.evenings, T.span(context, letter.phoneMinutes), T.span(context, letter.familyTogether)), paint(italic, 13f, muted), tw), margin, y)
            }
            doc.finishPage(pg)
        }

        // Month by month.
        var pg = page()
        var y = margin
        val tw = (w - margin * 2).toInt()
        fun room(need: Float) {
            if (y + need > h - margin) {
                doc.finishPage(pg)
                pg = page()
                y = margin
            }
        }
        letter.entries.groupBy { YearMonth.from(Jar.date(it.start)) }.forEach { (ym, list) ->
            room(60f)
            pg.canvas.text(layout(T.month(context, ym), paint(serif, 24f), tw), margin, y); y += 38f
            list.forEach { t -> y = entry(t, children, tw) { need -> room(need); pg.canvas to y } }
            y += 10f
        }
        doc.finishPage(pg)

        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "almanac-" + title.replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-') + ".pdf")
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        return f
    }

    /** One stretch of time: a picture on the left if there is one, what and when, and the words in handwriting. */
    private fun entry(t: Together, children: List<Child>, tw: Int, place: (Float) -> Pair<Canvas, Float>): Float {
        val kids = children.filter { t.of(it.id) }
        val photo = if (t.photo.isNotEmpty()) vault.full(t.photo, 600) else null
        val textW = if (photo != null) tw - 114 else tw - 20
        val head = layout(titleText(context, t), paint(sansBold, 11f), textW)
        val sub = layout(T.day(context, t.start) + " · " + T.names(context, kids) + " · " + T.span(context, t.minutes), paint(sans, 9f, muted), textW)
        val quote = t.quote.takeIf { it.isNotBlank() }?.let { layout("«$it»", paint(hand, 15f, amber), textW) }
        val note = t.note.takeIf { it.isNotBlank() }?.let { layout(it, paint(sans, 9.5f), textW) }
        val textH = head.height + sub.height + (quote?.height ?: 0) + (note?.height ?: 0) + 14f
        val need = maxOf(textH, if (photo != null) 104f else 0f) + 14f
        val (cv, y) = place(need)
        val x: Float
        if (photo != null) {
            val side = 100
            val src = run {
                val s = minOf(photo.width, photo.height)
                Rect((photo.width - s) / 2, (photo.height - s) / 2, (photo.width + s) / 2, (photo.height + s) / 2)
            }
            cv.drawBitmap(photo, src, RectF(margin, y, margin + side, y + side), Paint(Paint.FILTER_BITMAP_FLAG))
            x = margin + side + 14f
        } else {
            cv.marble(margin + 6f, y + 8f, 6f, t.kindOf.color().toArgb())
            x = margin + 20f
        }
        var yy = y
        cv.text(head, x, yy); yy += head.height + 2f
        cv.text(sub, x, yy); yy += sub.height + 4f
        quote?.let { cv.text(it, x, yy); yy += it.height + 2f }
        note?.let { cv.text(it, x, yy) }
        return y + need
    }
}
