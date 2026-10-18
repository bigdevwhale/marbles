package app.marbles.domain

import app.marbles.data.Child
import app.marbles.data.Kind
import app.marbles.data.Together
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Years, months and days of age. */
data class Age(val years: Int, val months: Int, val days: Int)

/**
 * The jar of Saturdays: one marble for every Saturday from birth to the eighteenth birthday. Each week
 * one marble leaves the jar; the weeks spent together come out coloured, the others grey.
 */
object Jar {
    const val YEARS = 18

    fun date(ms: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun age(born: LocalDate, on: LocalDate): Age {
        if (on.isBefore(born)) return Age(0, 0, 0)
        val p = Period.between(born, on)
        return Age(p.years, p.months, p.days)
    }

    /** The Monday a week starts on. */
    fun monday(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun saturday(d: LocalDate): LocalDate = monday(d).plusDays(5)

    fun eighteen(c: Child): LocalDate = c.birthday.plusYears(YEARS.toLong())

    /** Saturdays from [from] up to (not including) [until]. */
    fun saturdays(from: LocalDate, until: LocalDate): Int {
        if (!until.isAfter(from)) return 0
        val first = from.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        if (!first.isBefore(until)) return 0
        return (ChronoUnit.DAYS.between(first, until.minusDays(1)) / 7 + 1).toInt()
    }

    /** Every Saturday of a childhood. */
    fun total(c: Child): Int = saturdays(c.birthday, eighteen(c))

    /** The marbles still in the jar: Saturdays after this week's, before they turn eighteen. */
    fun left(c: Child, today: LocalDate): Int = saturdays(saturday(today).plusDays(1), eighteen(c))

    /** Marbles already gone, this week's in your hand not counted. */
    fun gone(c: Child, today: LocalDate): Int {
        val total = total(c)
        val inHand = if (saturday(today).isBefore(eighteen(c)) && !saturday(today).isBefore(c.birthday)) 1 else 0
        return (total - left(c, today) - inHand).coerceAtLeast(0)
    }

    /**
     * A rough share of the childhood Saturdays already behind: by the time a child is twelve it is
     * two thirds of the jar.
     */
    fun goneShare(c: Child, today: LocalDate): Float = total(c).let { if (it == 0) 1f else gone(c, today).toFloat() / it }

    fun grown(c: Child, today: LocalDate) = !today.isBefore(eighteen(c))
}

/** One week of a child's life as a marble: how much time together it had, and what kind mostly. */
data class Week(
    val monday: LocalDate,
    val minutes: Int,
    /** The kind with the most minutes, or null for an empty week. */
    val kind: Kind?,
    val entries: Int,
    /** 0 grey .. 1 full colour. */
    val fill: Float,
    val current: Boolean,
) {
    val saturday: LocalDate get() = monday.plusDays(5)
}

object Weeks {
    /** Minutes that count toward a child, of each entry. */
    fun of(child: Long, entries: List<Together>) = entries.filter { it.of(child) }

    fun week(monday: LocalDate, entries: List<Together>, goal: Int, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Week {
        val inWeek = entries.filter { Jar.monday(Jar.date(it.start, zone)) == monday }
        val minutes = inWeek.sumOf { it.minutes }
        val kind = inWeek.groupBy { it.kindOf }.maxByOrNull { (_, l) -> l.sumOf { it.minutes } }?.key
        return Week(
            monday = monday,
            minutes = minutes,
            kind = kind,
            entries = inWeek.size,
            fill = if (goal <= 0) 1f else (minutes.toFloat() / goal).coerceIn(0f, 1f),
            current = monday == Jar.monday(today),
        )
    }

    /**
     * The tray: every week since the jar was started (or since birth, if later), oldest first, this
     * week last. Weeks before that are not in the tray: nobody was counting them.
     */
    fun tray(c: Child, entries: List<Together>, startedAt: LocalDate, goal: Int, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Week> {
        val mine = of(c.id, entries)
        // Time logged before the jar was started still brings its weeks in.
        val earliest = mine.minOfOrNull { Jar.date(it.start, zone) }
        var from = Jar.monday(listOfNotNull(startedAt, earliest).min())
        if (from.isBefore(Jar.monday(c.birthday))) from = Jar.monday(c.birthday)
        val to = Jar.monday(today)
        val out = ArrayList<Week>()
        var m = from
        while (!m.isAfter(to)) {
            out += week(m, mine, goal, today, zone)
            m = m.plusWeeks(1)
        }
        return out
    }

    /** Minutes with a child in the last [days] days. */
    fun recent(child: Long, entries: List<Together>, now: Long, days: Int): Int =
        entries.filter { it.of(child) && it.start > now - days * 86_400_000L }.sumOf { it.minutes }

    /** The last time with just this child, no brothers or sisters, or null. */
    fun lastAlone(child: Long, entries: List<Together>): Together? = entries.filter { it.alone && it.of(child) }.maxByOrNull { it.start }
}
