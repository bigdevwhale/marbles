package app.marbles.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore("settings")

/** The phone is put away for time together: since when, with whom, and what it was meant to be. */
data class Session(
    val since: Long = 0,
    val kids: String = "",
    val idea: String = "",
    val alone: Boolean = false,
) {
    val active: Boolean get() = since > 0
}

/** The hours of a day that belong to the family, minutes after midnight. */
data class Hours(val from: Int, val to: Int) {
    val minutes: Int get() = (to - from).coerceAtLeast(0)
}

data class Settings(
    val onboarded: Boolean = false,
    /** When the jar's tray starts: weeks before it are "before the jar", not lost. */
    val startedAt: Long = 0,
    /** Minutes together in a week that make its marble full colour. */
    val weekGoal: Int = 180,
    val weekdayHours: Hours = Hours(18 * 60, 21 * 60),
    val weekendHours: Hours = Hours(10 * 60, 20 * 60),
    /** A word in the family hours when the phone has had too long. */
    val shield: Boolean = false,
    /** How many minutes of phone in a quarter of an hour bring the word. */
    val shieldMinutes: Int = 10,
    val shieldLast: Long = 0,
    /** One idea for the evening, around the time the parent gets home. */
    val evening: Boolean = true,
    val eveningAt: Int = 18 * 60 + 15,
    /** On Thursday: what are you doing on Saturday? */
    val weekend: Boolean = true,
    /** On the 1st: the month's letter. Once a month: what are they into now? */
    val letter: Boolean = true,
    val session: Session = Session(),
    /** Reminders already given ("letter:2026-09", "shield:20380"), so none comes twice. */
    val fired: String = "",
) {
    fun hours(d: LocalDate): Hours = if (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) weekendHours else weekdayHours
}

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        startedAt = this[STARTED_AT] ?: 0L,
        weekGoal = this[WEEK_GOAL] ?: 180,
        weekdayHours = Hours(this[WD_FROM] ?: (18 * 60), this[WD_TO] ?: (21 * 60)),
        weekendHours = Hours(this[WE_FROM] ?: (10 * 60), this[WE_TO] ?: (20 * 60)),
        shield = this[SHIELD] ?: false,
        shieldMinutes = this[SHIELD_MINUTES] ?: 10,
        shieldLast = this[SHIELD_LAST] ?: 0L,
        evening = this[EVENING] ?: true,
        eveningAt = this[EVENING_AT] ?: (18 * 60 + 15),
        weekend = this[WEEKEND] ?: true,
        letter = this[LETTER] ?: true,
        session = Session(
            since = this[S_SINCE] ?: 0L,
            kids = this[S_KIDS] ?: "",
            idea = this[S_IDEA] ?: "",
            alone = this[S_ALONE] ?: false,
        ),
        fired = this[FIRED] ?: "",
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[STARTED_AT] = s.startedAt
            it[WEEK_GOAL] = s.weekGoal
            it[WD_FROM] = s.weekdayHours.from
            it[WD_TO] = s.weekdayHours.to
            it[WE_FROM] = s.weekendHours.from
            it[WE_TO] = s.weekendHours.to
            it[SHIELD] = s.shield
            it[SHIELD_MINUTES] = s.shieldMinutes
            it[SHIELD_LAST] = s.shieldLast
            it[EVENING] = s.evening
            it[EVENING_AT] = s.eveningAt
            it[WEEKEND] = s.weekend
            it[LETTER] = s.letter
            it[S_SINCE] = s.session.since
            it[S_KIDS] = s.session.kids
            it[S_IDEA] = s.session.idea
            it[S_ALONE] = s.session.alone
            it[FIRED] = s.fired
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val STARTED_AT = longPreferencesKey("started_at")
        val WEEK_GOAL = intPreferencesKey("week_goal")
        val WD_FROM = intPreferencesKey("weekday_from")
        val WD_TO = intPreferencesKey("weekday_to")
        val WE_FROM = intPreferencesKey("weekend_from")
        val WE_TO = intPreferencesKey("weekend_to")
        val SHIELD = booleanPreferencesKey("shield")
        val SHIELD_MINUTES = intPreferencesKey("shield_minutes")
        val SHIELD_LAST = longPreferencesKey("shield_last")
        val EVENING = booleanPreferencesKey("evening")
        val EVENING_AT = intPreferencesKey("evening_at")
        val WEEKEND = booleanPreferencesKey("weekend")
        val LETTER = booleanPreferencesKey("letter")
        val S_SINCE = longPreferencesKey("session_since")
        val S_KIDS = stringPreferencesKey("session_kids")
        val S_IDEA = stringPreferencesKey("session_idea")
        val S_ALONE = booleanPreferencesKey("session_alone")
        val FIRED = stringPreferencesKey("fired")
    }
}
