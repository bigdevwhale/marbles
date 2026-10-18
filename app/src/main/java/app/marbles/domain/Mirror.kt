package app.marbles.domain

import app.marbles.data.Hours
import app.marbles.data.Together
import java.time.LocalDate
import java.time.ZoneId

/** One usage event, the way the phone's usage history reports it. */
data class Use(val at: Long, val pkg: String, val type: Int)

/** A stretch of time one app was in front. */
data class Stretch(val pkg: String, val from: Long, val to: Long) {
    val ms: Long get() = to - from
}

/**
 * The honest mirror: how long the phone was in use within the family hours, next to how long was
 * spent with the children. Plain arithmetic over usage events, so it can be tested without a phone.
 */
object Mirror {
    // android.app.usage.UsageEvents.Event types.
    const val RESUMED = 1
    const val PAUSED = 2
    const val SCREEN_ON = 15
    const val SCREEN_OFF = 16
    const val STOPPED = 23
    const val SHUTDOWN = 26

    /** A glance at a message isn't "being on the phone". */
    const val GLANCE_MS = 30_000L

    /**
     * When each app was in front between [from] and [to]. Events should start a little before [from],
     * so the app already open at [from] is known. Apps in [skip] (this one, the home screen) don't count.
     */
    fun stretches(events: List<Use>, from: Long, to: Long, skip: Set<String>): List<Stretch> {
        val out = ArrayList<Stretch>()
        var front: String? = null
        var since = 0L
        fun close(at: Long) {
            val p = front ?: return
            val a = maxOf(since, from)
            val b = minOf(at, to)
            if (b > a && p !in skip) out += Stretch(p, a, b)
            front = null
        }
        for (e in events.sortedBy { it.at }) {
            if (e.at > to && front == null) break
            when (e.type) {
                RESUMED -> {
                    if (front != e.pkg) { close(e.at); front = e.pkg; since = e.at }
                }
                PAUSED, STOPPED -> if (front == e.pkg) close(e.at)
                SCREEN_OFF, SHUTDOWN -> close(e.at)
            }
        }
        close(to)
        // Back-to-back stretches of one app (a rotation, a dialog) are one.
        val merged = ArrayList<Stretch>()
        for (s in out) {
            val last = merged.lastOrNull()
            if (last != null && last.pkg == s.pkg && s.from - last.to < 2_000) merged[merged.size - 1] = last.copy(to = s.to) else merged += s
        }
        return merged
    }

    /** Minutes per app, biggest first. */
    fun perApp(stretches: List<Stretch>): List<Pair<String, Int>> =
        stretches.groupBy { it.pkg }.map { (p, l) -> p to ((l.sumOf { it.ms } + 30_000) / 60_000).toInt() }.filter { it.second > 0 }.sortedByDescending { it.second }

    fun minutes(stretches: List<Stretch>): Int = ((stretches.sumOf { it.ms } + 30_000) / 60_000).toInt()

    /** Times the phone was really picked up: stretches of use longer than a glance, joined when close together. */
    fun pickups(stretches: List<Stretch>): Int {
        var n = 0
        var lastEnd = Long.MIN_VALUE
        var run = 0L
        for (s in stretches.sortedBy { it.from }) {
            if (s.from - lastEnd > 60_000) {
                if (run >= GLANCE_MS) n++
                run = 0
            }
            run += s.ms
            lastEnd = s.to
        }
        if (run >= GLANCE_MS) n++
        return n
    }

    /** Phone minutes in a session, glances left out. */
    fun sessionPhoneMinutes(stretches: List<Stretch>): Int {
        val groups = ArrayList<Long>()
        var lastEnd = Long.MIN_VALUE
        for (s in stretches.sortedBy { it.from }) {
            if (s.from - lastEnd > 60_000 || groups.isEmpty()) groups += 0L
            groups[groups.size - 1] = groups.last() + s.ms
            lastEnd = s.to
        }
        return ((groups.filter { it >= GLANCE_MS }.sum() + 30_000) / 60_000).toInt()
    }

    /** The family hours of a day, as millis. */
    fun window(day: LocalDate, h: Hours, zone: ZoneId = ZoneId.systemDefault()): LongRange {
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        return (start + h.from * 60_000L)..(start + h.to * 60_000L)
    }

    /** Minutes spent together within a window, with a child or with any of them. */
    fun togetherIn(window: LongRange, entries: List<Together>, child: Long? = null): Int {
        var ms = 0L
        // Two children at once is one stretch of time, not two.
        // Only the minutes that count: what the phone took of a session isn't time together.
        val spans = entries.filter { child == null || it.of(child) }.map { it.start to it.start + it.minutes * 60_000L }.sortedBy { it.first }
        var curA = Long.MIN_VALUE
        var curB = Long.MIN_VALUE
        fun flush() {
            val a = maxOf(curA, window.first)
            val b = minOf(curB, window.last)
            if (b > a) ms += b - a
        }
        for ((a, b) in spans) {
            if (a > curB) { flush(); curA = a; curB = b } else curB = maxOf(curB, b)
        }
        flush()
        return (ms / 60_000).toInt()
    }
}
