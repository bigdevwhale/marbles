package app.marbles.domain

import app.marbles.data.Child
import app.marbles.data.Interest
import app.marbles.data.Plan
import app.marbles.data.Settings
import app.marbles.data.Together
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class Nudge {
    /** One idea for the evening, around when the parent gets home. */
    EVENING,
    /** Thursday: what are you doing on Saturday? */
    WEEKEND,
    /** The 1st: last month's letter. */
    LETTER,
    /** What is a child into now? Asked when nobody has said for a month. */
    INTERESTS,
    /** A look at the last quarter of an hour, in the family hours. */
    SHIELD,
}

/** A reminder due at [at]; [key] is remembered once given, so it comes only once. */
data class Due(val at: Long, val nudge: Nudge, val key: String, val child: Long = 0)

object Nudges {
    /** Keys older than this many entries are forgotten. */
    private const val KEEP = 80

    /**
     * The hour the parent usually starts time together on a weekday, a little before it, learned from
     * the timed sessions; [fallback] until there are enough of them.
     */
    fun eveningAt(entries: List<Together>, fallback: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val starts = entries.asSequence()
            .filter { it.timed && it.start > now - 60 * 86_400_000L }
            .map { Instant.ofEpochMilli(it.start).atZone(zone) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            .map { it.hour * 60 + it.minute }
            .filter { it in 15 * 60..21 * 60 }
            .sorted().toList()
        if (starts.size < 5) return fallback
        val median = starts[starts.size / 2] - 20
        return (median / 5 * 5).coerceIn(15 * 60, 21 * 60)
    }

    private fun at(d: LocalDate, minutes: Int, zone: ZoneId) = d.atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(zone).toInstant().toEpochMilli()

    fun plan(
        children: List<Child>,
        entries: List<Together>,
        plans: List<Plan>,
        interests: List<Interest>,
        s: Settings,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Due> {
        if (!s.onboarded || children.isEmpty()) return emptyList()
        val fired = s.fired.split('|').toSet()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val out = ArrayList<Due>()

        if (s.evening) {
            val t = eveningAt(entries, s.eveningAt, now, zone)
            for (d in listOf(today, today.plusDays(1))) {
                val time = at(d, t, zone)
                val dayStart = at(d, 0, zone)
                // Already together today before the reminder: nothing to remind.
                val done = entries.any { it.start in dayStart until time } || (s.session.active && d == today)
                if (!done) out += Due(time, Nudge.EVENING, "eve:${d.toEpochDay()}")
            }
        }
        if (s.weekend) {
            var thu = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
            if (at(thu, 19 * 60 + 30, zone) < now - 60_000) thu = thu.plusWeeks(1)
            val sat = thu.plusDays(2)
            val planned = plans.any { it.day == sat.toEpochDay() || it.day == sat.plusDays(1).toEpochDay() }
            if (!planned) out += Due(at(thu, 19 * 60 + 30, zone), Nudge.WEEKEND, "wkd:${sat.toEpochDay()}")
        }
        if (s.letter) {
            var first = today.withDayOfMonth(1)
            if (at(first, 10 * 60, zone) < now - 60_000) first = first.plusMonths(1)
            val month = YearMonth.from(first.minusMonths(1))
            val any = entries.any { YearMonth.from(Jar.date(it.start, zone)) == month }
            if (any) out += Due(at(first, 10 * 60, zone), Nudge.LETTER, "letter:$month")

            var sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            if (at(sunday, 11 * 60, zone) < now - 60_000) sunday = sunday.plusWeeks(1)
            for (c in children) {
                if (Jar.grown(c, today)) continue
                val last = interests.filter { it.childId == c.id }.maxOfOrNull { maxOf(it.since, it.until) } ?: c.createdAt
                if (now - last > 30 * 86_400_000L) out += Due(at(sunday, 11 * 60, zone), Nudge.INTERESTS, "int:${c.id}:${sunday.toEpochDay()}", c.id)
            }
        }
        if (s.shield) shieldAt(s, now, zone)?.let { out += Due(it, Nudge.SHIELD, "") }
        return out.filter { it.key.isEmpty() || it.key !in fired }.sortedBy { it.at }
    }

    /** The next quarter-hour inside the family hours, today or tomorrow. */
    fun shieldAt(s: Settings, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        for (d in listOf(today, today.plusDays(1))) {
            val h = s.hours(d)
            // The first look is a quarter of an hour in: there has to be something to look back on.
            var m = h.from + 15
            while (m <= h.to) {
                val t = at(d, m, zone)
                if (t > now + 30_000) return t
                m += 15
            }
        }
        return null
    }

    /** Whether the shield should speak: enough phone lately, no time together under way, not too often. */
    fun shieldSpeaks(phoneMinutes: Int, s: Settings, now: Long): Boolean =
        s.shield && !s.session.active && phoneMinutes >= s.shieldMinutes && now - s.shieldLast > 45 * 60_000L

    fun remember(fired: String, keys: List<String>): String =
        (fired.split('|').filter { it.isNotEmpty() } + keys.filter { it.isNotEmpty() }).distinct().takeLast(KEEP).joinToString("|")
}
