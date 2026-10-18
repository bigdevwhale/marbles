package app.marbles.domain

import app.marbles.data.Child
import app.marbles.data.Evening
import app.marbles.data.Kind
import app.marbles.data.Together
import java.time.YearMonth
import java.time.ZoneId

/** A month (or a year) looked back on: what went into the jar, and what the children said. */
class Letter(
    val from: YearMonth,
    val to: YearMonth,
    val entries: List<Together>,
    /** Minutes with each child. */
    val perChild: List<Pair<Child, Int>>,
    val kinds: List<Pair<Kind, Int>>,
    /** Distinct days with time together. */
    val days: Int,
    val minutes: Int,
    val quotes: List<Together>,
    val firsts: List<Together>,
    val photos: List<Together>,
    /** The evenings measured in the mirror, if any: phone minutes and together minutes in the family hours. */
    val phoneMinutes: Int,
    val familyTogether: Int,
    val evenings: Int,
    /** The longest single stretch of time together. */
    val longest: Together?,
) {
    val empty: Boolean get() = entries.isEmpty()

    companion object {
        fun of(
            from: YearMonth,
            to: YearMonth,
            children: List<Child>,
            all: List<Together>,
            evenings: List<Evening>,
            settings: app.marbles.data.Settings,
            zone: ZoneId = ZoneId.systemDefault(),
        ): Letter {
            val inside = { ym: YearMonth -> !ym.isBefore(from) && !ym.isAfter(to) }
            val entries = all.filter { inside(YearMonth.from(Jar.date(it.start, zone))) }.sortedBy { it.start }
            val ev = evenings.filter { inside(YearMonth.from(it.date)) }
            val familyTogether = ev.sumOf { e -> Mirror.togetherIn(Mirror.window(e.date, app.marbles.data.Hours(e.from, e.to), zone), all) }
            return Letter(
                from = from,
                to = to,
                entries = entries,
                perChild = children.map { c -> c to entries.filter { it.of(c.id) }.sumOf { it.minutes } },
                kinds = entries.groupBy { it.kindOf }.map { (k, l) -> k to l.sumOf { it.minutes } }.sortedByDescending { it.second },
                days = entries.map { Jar.date(it.start, zone) }.distinct().size,
                minutes = mergedMinutes(entries),
                quotes = entries.filter { it.quote.isNotBlank() },
                firsts = entries.filter { it.first },
                photos = entries.filter { it.photo.isNotEmpty() },
                phoneMinutes = ev.sumOf { it.phoneMinutes },
                familyTogether = familyTogether,
                evenings = ev.size,
                longest = entries.maxByOrNull { it.minutes },
            )
        }

        /** Minutes of time together, with two children at once counted once. */
        fun mergedMinutes(entries: List<Together>): Int {
            var ms = 0L
            var a = Long.MIN_VALUE
            var b = Long.MIN_VALUE
            for (e in entries.sortedBy { it.start }) {
                val s = e.start
                val t = e.start + e.minutes * 60_000L
                if (s > b) { if (b > a) ms += b - a; a = s; b = t } else b = maxOf(b, t)
            }
            if (b > a) ms += b - a
            return (ms / 60_000).toInt()
        }
    }
}
