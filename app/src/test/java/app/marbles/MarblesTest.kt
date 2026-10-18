package app.marbles

import androidx.compose.ui.geometry.Rect
import app.marbles.content.Energy
import app.marbles.content.Ideas
import app.marbles.content.Notes
import app.marbles.content.Questions
import app.marbles.data.Backup
import app.marbles.data.Child
import app.marbles.data.Evening
import app.marbles.data.Everything
import app.marbles.data.Hours
import app.marbles.data.Interest
import app.marbles.data.Kind
import app.marbles.data.Plan
import app.marbles.data.Session
import app.marbles.data.Settings
import app.marbles.data.Together
import app.marbles.domain.Jar
import app.marbles.domain.Letter
import app.marbles.domain.Mirror
import app.marbles.domain.Nudge
import app.marbles.domain.Nudges
import app.marbles.domain.Use
import app.marbles.domain.Weeks
import app.marbles.ui.T
import app.marbles.ui.jar.Pack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MarblesTest {
    private val utc = ZoneOffset.UTC
    private fun ms(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).toInstant(utc).toEpochMilli()

    @Test
    fun aChildhoodHasAboutNineHundredFortySaturdays() {
        val c = Child(id = 1, name = "A", born = LocalDate.of(2020, 3, 4).toEpochDay())
        assertTrue(Jar.total(c) in 937..941)
    }

    @Test
    fun theJarEmptiesWeekByWeek() {
        val c = Child(id = 1, name = "A", born = LocalDate.of(2020, 3, 4).toEpochDay())
        val mon = LocalDate.of(2026, 10, 12)
        val left = Jar.left(c, mon)
        // Any day of the same week leaves the same jar; the next week takes one more out.
        assertEquals(left, Jar.left(c, mon.plusDays(6)))
        assertEquals(left - 1, Jar.left(c, mon.plusDays(7)))
        // In hand + left + gone = all of them.
        assertEquals(Jar.total(c), Jar.left(c, mon) + Jar.gone(c, mon) + 1)
    }

    @Test
    fun aGrownChildHasAnEmptyJar() {
        val c = Child(id = 1, name = "A", born = LocalDate.of(2000, 1, 1).toEpochDay())
        val today = LocalDate.of(2026, 10, 18)
        assertTrue(Jar.grown(c, today))
        assertEquals(0, Jar.left(c, today))
        assertEquals(Jar.total(c), Jar.gone(c, today))
    }

    @Test
    fun weeksColourByTimeAndKind() {
        val mon = LocalDate.of(2026, 10, 12)
        val e = listOf(
            Together(id = 1, kids = ",1,", start = ms(mon.plusDays(1), 18), minutes = 30, kind = Kind.BOOK.name),
            Together(id = 2, kids = ",1,2,", start = ms(mon.plusDays(5), 11), minutes = 120, kind = Kind.OUTSIDE.name),
            Together(id = 3, kids = ",2,", start = ms(mon.plusDays(5), 15), minutes = 300, kind = Kind.TRIP.name),
        )
        val w = Weeks.week(mon, Weeks.of(1, e), 180, mon.plusDays(6), utc)
        assertEquals(150, w.minutes)
        assertEquals(Kind.OUTSIDE, w.kind)
        assertEquals(150f / 180f, w.fill, 0.001f)
        assertTrue(w.current)
        val empty = Weeks.week(mon.minusWeeks(1), Weeks.of(1, e), 180, mon, utc)
        assertEquals(0f, empty.fill)
        assertEquals(null, empty.kind)
    }

    @Test
    fun theTrayStartsWhenTheJarWasOpened() {
        val c = Child(id = 1, name = "A", born = LocalDate.of(2020, 3, 4).toEpochDay())
        val today = LocalDate.of(2026, 10, 18)
        val tray = Weeks.tray(c, emptyList(), today.minusWeeks(4), 180, today, utc)
        assertEquals(5, tray.size)
        assertTrue(tray.last().current)
        // Time logged from before the jar brings its week in.
        val old = Together(kids = ",1,", start = ms(today.minusWeeks(9), 12), minutes = 60)
        assertEquals(10, Weeks.tray(c, listOf(old), today.minusWeeks(4), 180, today, utc).size)
    }

    @Test
    fun theMirrorCountsAppsInFrontWithinTheWindow() {
        val ev = listOf(
            Use(1_000, "com.insta", Mirror.RESUMED),
            Use(5_000, "com.insta", Mirror.PAUSED),
            Use(5_000, "launcher", Mirror.RESUMED),
            Use(6_000, "launcher", Mirror.PAUSED),
            Use(6_000, "com.tube", Mirror.RESUMED),
            Use(9_000, "x", Mirror.SCREEN_OFF),
            Use(20_000, "com.tube", Mirror.RESUMED),
        )
        val st = Mirror.stretches(ev, 2_000, 25_000, setOf("launcher"))
        assertEquals(listOf("com.insta", "com.tube", "com.tube"), st.map { it.pkg })
        assertEquals(3_000L, st[0].ms) // from the window's start, not the app's
        assertEquals(3_000L, st[1].ms) // ends when the screen goes off
        assertEquals(5_000L, st[2].ms) // still open when the window ends
    }

    @Test
    fun glancesAreNotPickups() {
        val ev = listOf(
            Use(0, "a", Mirror.RESUMED), Use(10_000, "a", Mirror.PAUSED), // a glance
            Use(300_000, "b", Mirror.RESUMED), Use(420_000, "b", Mirror.PAUSED), // two minutes
            Use(900_000, "c", Mirror.RESUMED), Use(960_000, "c", Mirror.PAUSED), // one minute
        )
        val st = Mirror.stretches(ev, 0, 1_000_000, emptySet())
        assertEquals(2, Mirror.pickups(st))
        assertEquals(3, Mirror.sessionPhoneMinutes(st))
    }

    @Test
    fun togetherInTheWindowCountsOverlapsOnce() {
        val d = LocalDate.of(2026, 10, 14)
        val w = Mirror.window(d, Hours(18 * 60, 21 * 60), utc)
        val e = listOf(
            Together(kids = ",1,", start = ms(d, 17, 30), minutes = 60), // 30 min inside
            Together(kids = ",2,", start = ms(d, 18, 0), minutes = 30), // overlaps the first
            Together(kids = ",1,", start = ms(d, 20, 30), minutes = 60), // 30 min inside
        )
        assertEquals(60, Mirror.togetherIn(w, e))
        assertEquals(60, Mirror.togetherIn(w, e, 1))
        assertEquals(30, Mirror.togetherIn(w, e, 2))
    }

    @Test
    fun theEveningIdeaLearnsWhenYouStart() {
        val start = LocalDate.of(2026, 10, 5) // a Monday
        val e = (0 until 5).map { Together(kids = ",1,", start = ms(start.plusDays(it.toLong()), 19, 10), minutes = 30, timed = true) }
        val now = ms(start.plusDays(7), 12)
        assertEquals(18 * 60 + 50, Nudges.eveningAt(e, 18 * 60 + 15, now, utc))
        assertEquals(18 * 60 + 15, Nudges.eveningAt(e.take(4), 18 * 60 + 15, now, utc))
    }

    @Test
    fun remindersComeOnceAndNotWhenDone() {
        val today = LocalDate.of(2026, 10, 14) // a Wednesday
        val kids = listOf(Child(id = 1, name = "A", born = LocalDate.of(2020, 1, 1).toEpochDay(), createdAt = ms(today, 0)))
        val s = Settings(onboarded = true)
        val now = ms(today, 9)
        val due = Nudges.plan(kids, emptyList(), emptyList(), emptyList(), s, now, utc)
        val eve = due.first { it.nudge == Nudge.EVENING }
        assertEquals(ms(today, 18, 15), eve.at)
        // Given once, never again.
        val after = Nudges.plan(kids, emptyList(), emptyList(), emptyList(), s.copy(fired = Nudges.remember("", listOf(eve.key))), now, utc)
        assertFalse(after.any { it.key == eve.key })
        // Time together already today: no evening reminder for today.
        val done = listOf(Together(kids = ",1,", start = ms(today, 8), minutes = 10))
        assertFalse(Nudges.plan(kids, done, emptyList(), emptyList(), s, now, utc).any { it.key == eve.key })
        // A Saturday already planned: no Thursday question.
        val sat = LocalDate.of(2026, 10, 17)
        val plan = listOf(Plan(kids = ",1,", day = sat.toEpochDay(), text = "x"))
        assertTrue(due.any { it.nudge == Nudge.WEEKEND })
        assertFalse(Nudges.plan(kids, emptyList(), plan, emptyList(), s, now, utc).any { it.nudge == Nudge.WEEKEND })
        // A month without a word about what they're into brings the question.
        assertTrue(Nudges.plan(kids, emptyList(), emptyList(), emptyList(), s, ms(today.plusDays(40), 9), utc).any { it.nudge == Nudge.INTERESTS })
        val fresh = listOf(Interest(childId = 1, text = "x", since = ms(today.plusDays(35), 9)))
        assertFalse(Nudges.plan(kids, emptyList(), emptyList(), fresh, s, ms(today.plusDays(40), 9), utc).any { it.nudge == Nudge.INTERESTS })
    }

    @Test
    fun theShieldIsPoliteAboutIt() {
        val s = Settings(onboarded = true, shield = true, shieldMinutes = 10)
        val now = 10_000_000L
        assertTrue(Nudges.shieldSpeaks(12, s, now))
        assertFalse(Nudges.shieldSpeaks(8, s, now))
        assertFalse(Nudges.shieldSpeaks(12, s.copy(shieldLast = now - 20 * 60_000L), now))
        assertFalse(Nudges.shieldSpeaks(12, s.copy(session = Session(since = now - 1000)), now))
        assertFalse(Nudges.shieldSpeaks(12, s.copy(shield = false), now))
        val d = LocalDate.of(2026, 10, 14)
        assertEquals(ms(d, 18, 15), Nudges.shieldAt(s, ms(d, 12), utc))
        assertEquals(ms(d, 19, 30), Nudges.shieldAt(s, ms(d, 19, 20), utc))
    }

    @Test
    fun theLetterCountsSharedTimeOnce() {
        val d = LocalDate.of(2026, 9, 5)
        val kids = listOf(Child(id = 1, name = "A", born = 0), Child(id = 2, name = "B", born = 0))
        val e = listOf(
            Together(id = 1, kids = ",1,2,", start = ms(d, 10), minutes = 60, quote = "hi", first = true),
            Together(id = 2, kids = ",1,", start = ms(d, 10, 30), minutes = 60),
            Together(id = 3, kids = ",2,", start = ms(d.plusMonths(1), 10), minutes = 60),
        )
        val l = Letter.of(java.time.YearMonth.of(2026, 9), java.time.YearMonth.of(2026, 9), kids, e, emptyList(), Settings(), utc)
        assertEquals(90, l.minutes)
        assertEquals(1, l.days)
        assertEquals(listOf(120, 60), l.perChild.map { it.second })
        assertEquals(1, l.quotes.size)
        assertEquals(1, l.firsts.size)
    }

    @Test
    fun genderMarksPickTheRightForm() {
        val girl = Child(name = "Маша", born = 0, gender = "GIRL")
        val boy = Child(name = "Петя", born = 0, gender = "BOY")
        val q = "Если бы ты был{|а} главн{ым|ой}, что бы ты сделал{|а}?"
        assertEquals("Если бы ты была главной, что бы ты сделала?", T.g(q, girl))
        assertEquals("Если бы ты был главным, что бы ты сделал?", T.g(q, boy))
        assertEquals("Если бы ты был главным, что бы ты сделал?", T.g(q, null))
        // Every Russian text with marks has well-formed ones.
        val all = Questions.all.map { it.text.ru } + Ideas.all.flatMap { listOf(it.title.ru, it.text.ru) } + Notes.all.flatMap { listOf(it.title.ru, it.text.ru) }
        all.forEach { assertFalse(it, T.g(it, girl).contains('{') || T.g(it, girl).contains('}')) }
    }

    @Test
    fun theDeckHasSomethingForEveryAgeAndBattery() {
        for (age in 0..17) for (e in Energy.entries) {
            assertTrue("age $age $e", Ideas.deck(age, e, null).isNotEmpty() || age < 2)
        }
        for (age in 3..17) assertTrue("rituals $age", Ideas.deck(age, Energy.LOW, null, rituals = true).isNotEmpty())
        for (age in 0..17) assertTrue("note $age", Notes.forWeek(age, 5) != null)
        assertEquals(Ideas.all.size, Ideas.byKey.size)
        assertEquals(Questions.all.size, Questions.byKey.size)
    }

    @Test
    fun allMarblesFitInTheJar() {
        val body = Rect(0f, 0f, 600f, 800f)
        for (n in listOf(1, 40, 300, 940)) assertEquals(n, Pack.pack(n, body, 0.16f, 0.62f).centers.size)
    }

    @Test
    fun backupRoundTrips() {
        val all = Everything(
            children = listOf(Child(id = 1, name = "Маша", born = 18000, gender = "GIRL", glass = 2, createdAt = 5)),
            together = listOf(Together(id = 3, kids = ",1,", start = 100, minutes = 45, timed = true, phoneMinutes = 4, kind = Kind.TALK.name, note = "n", quote = "«q»", photo = "p.jpg", idea = "fort", question = "funny", alone = true, first = true)),
            interests = listOf(Interest(id = 4, childId = 1, text = "Лошади", since = 7, until = 9)),
            plans = listOf(Plan(id = 5, kids = ",1,", day = 20000, text = "t", idea = "train", alone = true, done = 11)),
            evenings = listOf(Evening(day = 20001, from = 1080, to = 1260, phoneMinutes = 95, apps = "a=50;b=45")),
        )
        val back = Backup.decode(Backup.encode(all))
        assertEquals(all.children, back.children)
        assertEquals(all.together, back.together)
        assertEquals(all.interests, back.interests)
        assertEquals(all.plans, back.plans)
        assertEquals(all.evenings, back.evenings)
        assertEquals(listOf("a" to 50, "b" to 45), back.evenings[0].appMinutes)
    }
}
