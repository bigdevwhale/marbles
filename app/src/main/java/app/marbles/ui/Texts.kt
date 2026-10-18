package app.marbles.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.marbles.R
import app.marbles.data.Child
import app.marbles.data.Kind
import app.marbles.domain.Age
import app.marbles.domain.Jar
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Words and dates, in the app's language. Plain functions over a [Context] so receivers can use them too. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "7 years", "2 years 3 months", "5 months". */
    fun age(c: Context, a: Age): String = when {
        a.years == 0 && a.months == 0 -> plural(c, R.plurals.days, a.days)
        a.years == 0 -> plural(c, R.plurals.months, a.months)
        a.years >= 3 || a.months == 0 -> plural(c, R.plurals.years, a.years)
        else -> plural(c, R.plurals.years, a.years) + " " + plural(c, R.plurals.months, a.months)
    }

    fun ageOf(c: Context, k: Child, today: LocalDate = LocalDate.now()): String = age(c, Jar.age(k.birthday, today))

    /**
     * Russian sentences about a child change with them: "{м|ж}" marks pick the boy's or the girl's
     * form, the first when unknown. English texts simply have none.
     */
    fun g(text: String, k: Child?): String = Regex("\\{([^|{}]*)\\|([^|{}]*)\\}").replace(text) { m -> if (k?.girl == true) m.groupValues[2] else m.groupValues[1] }

    fun local(ms: Long, zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(ms).atZone(zone)

    /** "17 Oct 2026". */
    fun date(c: Context, d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale(c)))
    fun date(c: Context, ms: Long): String = date(c, local(ms).toLocalDate())

    /** "17 Oct". */
    fun day(c: Context, d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM", locale(c)))
    fun day(c: Context, ms: Long): String = day(c, local(ms).toLocalDate())

    /** "Saturday, 17 Oct". */
    fun weekday(c: Context, d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEEE, d MMM", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    /** "Sat". */
    fun dow(c: Context, d: LocalDate): String =
        d.format(DateTimeFormatter.ofPattern("EEE", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }.trimEnd('.')

    /** "September 2026". */
    fun month(c: Context, ym: YearMonth): String =
        ym.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    /** "September", as a word on its own. */
    fun monthName(c: Context, ym: YearMonth): String =
        ym.format(DateTimeFormatter.ofPattern("LLLL", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    fun time(c: Context, ms: Long): String = local(ms).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale(c)))

    fun clock(c: Context, m: Int): String =
        java.time.LocalTime.of((m / 60).coerceIn(0, 23), m % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale(c)))

    /** "18:00–21:00". */
    fun hours(c: Context, from: Int, to: Int): String = clock(c, from) + "–" + if (to >= 24 * 60) "24:00" else clock(c, to)

    /** "1 h 40 min", "15 min". */
    fun span(c: Context, minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> c.getString(R.string.span_m, m)
            m == 0 -> c.getString(R.string.span_h, h)
            else -> c.getString(R.string.span_hm, h, m)
        }
    }

    /** "0:42", "1:05:09": a running clock. */
    fun stopwatch(ms: Long): String {
        val s = ms / 1000
        return if (s < 3600) "%d:%02d".format(s / 60, s % 60) else "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
    }

    fun kind(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.PLAY -> R.string.kind_play
            Kind.OUTSIDE -> R.string.kind_outside
            Kind.TALK -> R.string.kind_talk
            Kind.BOOK -> R.string.kind_book
            Kind.KITCHEN -> R.string.kind_kitchen
            Kind.MAKE -> R.string.kind_make
            Kind.SPORT -> R.string.kind_sport
            Kind.ROUTINE -> R.string.kind_routine
            Kind.TRIP -> R.string.kind_trip
        }
    )

    /** "Masha", "Masha and Petya", "Masha, Petya and Olya". */
    fun names(c: Context, kids: List<Child>): String = when (kids.size) {
        0 -> ""
        1 -> kids[0].name
        else -> kids.dropLast(1).joinToString(", ") { it.name } + c.getString(R.string.and_sep) + kids.last().name
    }
}

@Composable
fun ctx(): Context = LocalContext.current
