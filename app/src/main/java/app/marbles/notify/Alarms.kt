package app.marbles.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.marbles.MainActivity
import app.marbles.R
import app.marbles.container
import app.marbles.content.Energy
import app.marbles.content.Ideas
import app.marbles.data.Child
import app.marbles.data.Session
import app.marbles.domain.Due
import app.marbles.domain.Jar
import app.marbles.domain.Nudge
import app.marbles.domain.Nudges
import app.marbles.localized
import app.marbles.ui.T
import app.marbles.usage.Usage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Gentle reminders: an idea for the evening, Thursday's question about Saturday, the month's letter,
 * what the children are into now, and the shield in the family hours. One alarm, set for the earliest.
 */
object Alarms {
    private const val CHANNEL = "nudges_v1"
    private const val CHANNEL_TOGETHER = "together_v1"
    private const val ACTION = "app.marbles.notify.NUDGE"
    const val EXTRA_OPEN = "open"
    const val EXTRA_CHILD = "child"
    private const val ID_TOGETHER = 1

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val l = context.localized()
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, l.getString(R.string.channel_nudges), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = l.getString(R.string.channel_nudges_desc)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_TOGETHER) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_TOGETHER, l.getString(R.string.channel_together), NotificationManager.IMPORTANCE_LOW).apply {
                    description = l.getString(R.string.channel_together_desc)
                    setShowBadge(false)
                }
            )
        }
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private suspend fun plans(context: Context, now: Long): List<Due> {
        val c = context.container
        val dao = c.db.dao()
        return Nudges.plan(dao.children(), dao.together(), dao.plans(), dao.interests(), c.settings.now(), now)
    }

    suspend fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val p = pi(context)
        val now = System.currentTimeMillis()
        val next = plans(context, now).firstOrNull { it.at > now }?.at
        if (next == null) {
            am.cancel(p)
            return
        }
        // A reminder doesn't need to-the-second precision; this avoids the exact-alarm permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, p)
    }

    /** Gives every reminder whose time has come. */
    suspend fun fire(context: Context) {
        val c = context.container
        val now = System.currentTimeMillis()
        val s = c.settings.now()
        // Keep the evenings the phone still remembers while we're awake anyway.
        runCatching { Usage.record(context, c.db.dao(), s) }
        val due = plans(context, now).filter { it.at <= now + 60_000 }
        if (due.isEmpty()) return
        c.settings.update { it.copy(fired = Nudges.remember(it.fired, due.map { d -> d.key })) }
        if (!canNotify(context)) return
        val children = c.db.dao().children()
        val l = context.localized()
        val today = LocalDate.now()
        val young = children.filter { !Jar.grown(it, today) }
        for (d in due) when (d.nudge) {
            Nudge.EVENING -> {
                if (young.isEmpty()) continue
                val k = young[(today.toEpochDay() % young.size).toInt()]
                val age = Jar.age(k.birthday, today).years
                val deck = Ideas.deck(age, Energy.MID, null) + Ideas.deck(age, Energy.LOW, null)
                val idea = deck.getOrNull((today.toEpochDay() % deck.size.coerceAtLeast(1)).toInt()) ?: continue
                post(context, 10, l.getString(R.string.nudge_evening_title, k.name), idea.title.get(l) + ". " + idea.text.get(l), "idea:${idea.key}", k.id)
            }
            Nudge.WEEKEND -> post(context, 11, l.getString(R.string.nudge_weekend_title), l.getString(R.string.nudge_weekend), "plan", 0)
            Nudge.LETTER -> post(context, 12, l.getString(R.string.nudge_letter_title, T.monthName(l, YearMonth.now().minusMonths(1))), l.getString(R.string.nudge_letter), "letter", 0)
            Nudge.INTERESTS -> children.firstOrNull { it.id == d.child }?.let { k ->
                post(context, 20 + (k.id % 50).toInt(), l.getString(R.string.nudge_interests_title, k.name), l.getString(R.string.nudge_interests), "child", k.id)
            }
            Nudge.SHIELD -> shield(context, young, s)
        }
    }

    /** The phone has had a long stretch in the family hours: a word, with a child's name in it. */
    private suspend fun shield(context: Context, young: List<Child>, s: app.marbles.data.Settings) {
        if (young.isEmpty() || !Usage.granted(context)) return
        val now = System.currentTimeMillis()
        val phone = Usage.lately(context, 15, now)
        if (!Nudges.shieldSpeaks(phone, s, now)) return
        context.container.settings.update { it.copy(shieldLast = now) }
        val l = context.localized()
        val k = young[((now / 3_600_000L) % young.size).toInt()]
        val text = l.getString(
            when { k.girl -> R.string.shield_text_f; k.boy -> R.string.shield_text_m; else -> R.string.shield_text },
            k.name, l.resources.getQuantityString(R.plurals.minutes, phone, phone),
        )
        post(context, 30, l.getString(R.string.shield_title, k.name), text, "together", k.id)
    }

    private fun open(context: Context, open: String, child: Long, code: Int) = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN, open).putExtra(EXTRA_CHILD, child)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun post(context: Context, id: Int, title: String, text: String, open: String, child: Long) {
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, open, child, 100 + id))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    /** While the phone is put away: a quiet clock in the shade, and a way back to finish. */
    fun showTogether(context: Context, session: Session, names: String) {
        if (!canNotify(context)) return
        val l = context.localized()
        val n = NotificationCompat.Builder(context, CHANNEL_TOGETHER)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(l.getString(R.string.together_note_title, names))
            .setContentText(l.getString(R.string.together_note_text))
            .setWhen(session.since)
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(open(context, "together", 0, 99))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_TOGETHER, n)
        } catch (_: SecurityException) {
        }
    }

    fun hideTogether(context: Context) = NotificationManagerCompat.from(context).cancel(ID_TOGETHER)

    internal const val NUDGE = ACTION
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Alarms.NUDGE) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context)
                Alarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
