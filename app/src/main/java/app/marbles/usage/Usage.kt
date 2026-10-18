package app.marbles.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import app.marbles.BuildConfig
import app.marbles.data.Evening
import app.marbles.data.MarblesDao
import app.marbles.data.Settings as AppSettings
import app.marbles.domain.Mirror
import app.marbles.domain.Stretch
import app.marbles.domain.Use
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads the phone's own usage history (with the parent's "usage access"). Nothing is sent anywhere;
 * only the minutes of each evening are kept, in the app's database.
 */
object Usage {
    /** The phone keeps its event history for about a week; evenings older than this can't be read. */
    const val HISTORY_DAYS = 6

    fun granted(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** The system page where usage access is given; straight to this app where the phone allows it. */
    fun askIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun askFallback(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun events(context: Context, from: Long, to: Long): List<Use> {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val out = ArrayList<Use>()
        val q = runCatching { usm.queryEvents(from, to) }.getOrNull() ?: return emptyList()
        val e = UsageEvents.Event()
        while (q.hasNextEvent()) {
            q.getNextEvent(e)
            when (e.eventType) {
                Mirror.RESUMED, Mirror.PAUSED, Mirror.STOPPED, Mirror.SCREEN_ON, Mirror.SCREEN_OFF, Mirror.SHUTDOWN ->
                    out += Use(e.timeStamp, e.packageName.orEmpty(), e.eventType)
            }
        }
        return out
    }

    /** This app, the home screen and the system's own surfaces: being there isn't being on the phone. */
    fun skip(context: Context): Set<String> {
        val pm = context.packageManager
        val homes = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo.packageName }
            // Settings carries a fallback home screen of its own; time in Settings is still time on the phone.
            .filter { it != "com.android.settings" }
        return (homes + listOf(context.packageName, "android", "com.android.systemui", "com.android.permissioncontroller")).toSet()
    }

    fun stretches(context: Context, from: Long, to: Long): List<Stretch> =
        if (!granted(context)) emptyList() else Mirror.stretches(events(context, from - 3 * 3_600_000L, to), from, to, skip(context))

    /** An evening, measured now; the one still going is measured up to this minute. */
    fun evening(context: Context, day: LocalDate, s: AppSettings, now: Long = System.currentTimeMillis()): Evening {
        val h = s.hours(day)
        val w = Mirror.window(day, h)
        val st = stretches(context, w.first, minOf(w.last, now))
        return Evening(day.toEpochDay(), h.from, h.to, Mirror.minutes(st), Mirror.perApp(st).take(6).joinToString(";") { "${it.first}=${it.second}" })
    }

    /** Keeps every finished evening the phone still remembers and that isn't kept yet. */
    suspend fun record(context: Context, dao: MarblesDao, s: AppSettings) {
        if (BuildConfig.DEMO || !granted(context)) return
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val have = dao.eveningDays().toSet()
        val now = System.currentTimeMillis()
        for (back in HISTORY_DAYS downTo 0) {
            val d = today.minusDays(back.toLong())
            if (d.toEpochDay() in have) continue
            // Only evenings that are over, and only from the day the jar was started.
            if (Mirror.window(d, s.hours(d)).last > now) continue
            if (s.startedAt > 0 && d.isBefore(app.marbles.domain.Jar.date(s.startedAt, zone))) continue
            dao.put(evening(context, d, s, now))
        }
    }

    /** Minutes the phone was in use over the last [minutes] minutes. */
    fun lately(context: Context, minutes: Int, now: Long = System.currentTimeMillis()): Int =
        Mirror.minutes(stretches(context, now - minutes * 60_000L, now))

    /** Names for the usual suspects, when the app itself is gone (or in the demo, where it never was). */
    private val known = mapOf(
        "com.instagram.android" to "Instagram", "com.google.android.youtube" to "YouTube", "org.telegram.messenger" to "Telegram",
        "com.android.chrome" to "Chrome", "com.whatsapp" to "WhatsApp", "com.zhiliaoapp.musically" to "TikTok", "com.vkontakte.android" to "VK",
    )

    fun label(context: Context, pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(known[pkg] ?: pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() })

    fun icon(context: Context, pkg: String, px: Int): Bitmap? = runCatching {
        context.packageManager.getApplicationIcon(pkg).toBitmap(px, px)
    }.getOrNull()
}
