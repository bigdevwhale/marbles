package app.marbles.data

import android.content.Context
import app.marbles.BuildConfig
import app.marbles.container
import app.marbles.domain.Mirror
import app.marbles.notify.Alarms
import app.marbles.usage.Usage

/** What a finished session came to: the minutes that count, and the ones the phone took. */
data class Finished(val id: Long, val minutes: Int, val phoneMinutes: Int, val pickups: Int, val measured: Boolean)

/**
 * Time together with the phone put away. The clock runs from [Session.since]; at the end the phone's
 * own usage history says how long it was picked up meanwhile, and that time doesn't count.
 */
object Sessions {
    suspend fun start(context: Context, kids: List<Child>, idea: String = "", alone: Boolean = false) {
        val c = context.container
        val s = Session(since = System.currentTimeMillis(), kids = Together.kidsOf(kids.map { it.id }), idea = idea, alone = alone || kids.size == 1 && c.db.dao().children().size > 1)
        c.settings.update { it.copy(session = s) }
        Alarms.showTogether(context, s, kids.joinToString(", ") { it.name })
        c.changed()
    }

    /** Phone minutes and pickups between [from] and [to], or null when the phone can't say. */
    fun phone(context: Context, from: Long, to: Long): Pair<Int, Int>? {
        if (BuildConfig.DEMO || !Usage.granted(context)) return null
        val st = Usage.stretches(context, from, to)
        return Mirror.sessionPhoneMinutes(st) to Mirror.pickups(st)
    }

    /** Ends the session and keeps it; null when it was too short to count. */
    suspend fun finish(context: Context, kind: Kind = Kind.PLAY): Finished? {
        val c = context.container
        val s = c.settings.now().session
        c.settings.update { it.copy(session = Session()) }
        Alarms.hideTogether(context)
        if (!s.active) return null
        val now = System.currentTimeMillis()
        val elapsed = ((now - s.since) / 60_000).toInt()
        val measured = phone(context, s.since, now)
        val phone = (measured?.first ?: 0).coerceAtMost(elapsed)
        val minutes = elapsed - phone
        c.changed()
        if (elapsed < 1) return null
        val idea = app.marbles.content.Ideas.byKey[s.idea]
        val id = c.db.dao().insert(
            Together(
                kids = s.kids, start = s.since, minutes = minutes, timed = true, phoneMinutes = phone,
                kind = (idea?.kind ?: kind).name, idea = s.idea, alone = s.alone,
            )
        )
        c.changed()
        return Finished(id, minutes, phone, measured?.second ?: 0, measured != null)
    }

    suspend fun cancel(context: Context) {
        val c = context.container
        c.settings.update { it.copy(session = Session()) }
        Alarms.hideTogether(context)
        c.changed()
    }
}
