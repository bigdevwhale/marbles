package app.marbles.data

import android.content.Context
import app.marbles.AppContainer
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/**
 * A made-up family for the demo build (screenshots): Masha, six, and her brother Petya, three. Ten
 * weeks of time together (some full, some thin, one missed), what they said, a few pictures, two
 * weeks of evenings in the mirror, and a plan for Saturday.
 */
object DemoSeed {
    /** [list] names the files of a demo folder; [open] reads one (the screen renders read them from disk). */
    suspend fun seedIfEmpty(
        context: Context,
        c: AppContainer,
        list: (String) -> List<String> = { context.assets.list(it)?.toList().orEmpty() },
        open: (String) -> java.io.InputStream = { context.assets.open(it) },
    ) {
        val dao = c.db.dao()
        if (dao.children().isNotEmpty()) return
        val tags = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val lang = if (!tags.isEmpty) tags[0]!!.language else context.resources.configuration.locales[0].language
        val ru = lang == "ru"
        fun t(en: String, r: String) = if (ru) r else en
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun at(d: LocalDate, h: Int, m: Int) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

        val files = list("demo").toSet()
        val cache = HashMap<String, String>()
        fun photo(name: String): String = cache.getOrPut(name) {
            if (name !in files) return@getOrPut ""
            val tmp = File(context.cacheDir, "demo-$name")
            val ok = runCatching { open("demo/$name").use { a -> tmp.outputStream().use { a.copyTo(it) } } }.isSuccess
            if (!ok) "" else tmp.inputStream().use { c.vault.take(it, "jpg") }.also { tmp.delete() }
        }

        val started = today.minusWeeks(10)
        val masha = Child(name = t("Masha", "Маша"), born = today.minusYears(6).minusMonths(4).minusDays(9).toEpochDay(), gender = "GIRL", glass = 0, createdAt = at(started, 20, 0))
        val petya = Child(name = t("Petya", "Петя"), born = today.minusYears(3).minusMonths(2).minusDays(20).toEpochDay(), gender = "BOY", glass = 1, createdAt = at(started, 20, 0))
        val m = dao.insert(masha)
        val p = dao.insert(petya)
        val both = Together.kidsOf(listOf(m, p))
        val ms = Together.kidsOf(listOf(m))
        val ps = Together.kidsOf(listOf(p))

        // Days back from today, hour, minutes, kind, with whom, and the rest.
        class E(
            val back: Int, val h: Int, val min: Int, val kind: Kind, val kids: String,
            val quote: String = "", val note: String = "", val pic: String = "", val idea: String = "", val question: String = "",
            val timed: Boolean = true, val phone: Int = 0, val alone: Boolean = false, val first: Boolean = false,
        )
        val sat = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        val satBack = (today.toEpochDay() - sat.toEpochDay()).toInt()
        val list = listOf(
            E(0, 18, 25, Kind.TALK, ms, quote = t("If I were invisible I'd sit in your office and watch what you actually do all day.", "Если бы я была невидимкой, я бы сидела у тебя на работе и смотрела, что ты там правда делаешь."), question = "invisible", alone = true),
            E(1, 19, 40, Kind.KITCHEN, both, quote = t("The first one is lumpy because it's shy.", "Первый блин комом, потому что он стесняется."), pic = "d-04.jpg", idea = "pancakes", phone = 4),
            E(2, 18, 20, Kind.BOOK, ps, idea = "read_roles", alone = true),
            E(3, 8, 10, Kind.ROUTINE, ms, idea = "road_story", timed = false),
            E(satBack, 11, 150, Kind.OUTSIDE, both, quote = t("Dad, the sea is breathing.", "Папа, море дышит."), pic = "d-09.jpg", idea = "walk_new", first = true, note = t("Petya walked the whole way back on his own.", "Петя сам прошёл всю дорогу обратно.")),
            E(satBack + 2, 19, 30, Kind.PLAY, both, idea = "fort", phone = 6),
            E(satBack + 4, 18, 45, Kind.TALK, ms, quote = t("When I'm big I'll be a vet and a pilot. A flying vet.", "Когда вырасту, буду ветеринаром и лётчиком. Летающим ветеринаром."), idea = "meal_rose", timed = false),
            E(satBack + 7, 12, 120, Kind.TRIP, both, pic = "d-07.jpg", idea = "museum", note = t("Masha chose the dinosaur skull. Petya chose the fire extinguisher.", "Маша выбрала череп динозавра. Петя — огнетушитель.")),
            E(satBack + 8, 17, 60, Kind.SPORT, ms, pic = "d-02.jpg", idea = "bike", first = true, quote = t("Don't hold me! Okay hold me a bit.", "Не держи! Ладно, держи чуть-чуть."), alone = true),
            E(satBack + 11, 19, 25, Kind.MAKE, both, idea = "cardboard", phone = 12),
            E(satBack + 14, 10, 90, Kind.OUTSIDE, both, pic = "d-01.jpg", idea = "puddles", quote = t("This puddle has no bottom.", "У этой лужи нет дна.")),
            E(satBack + 17, 20, 15, Kind.ROUTINE, ps, idea = "bed_three", timed = false, quote = t("Good thing one: the cat. Two: the cat. Three: the cat.", "Хорошее первое: кот. Второе: кот. Третье: кот.")),
            E(satBack + 21, 13, 45, Kind.PLAY, ps, pic = "d-05.jpg", idea = "hide_seek", alone = true),
            E(satBack + 22, 18, 30, Kind.TALK, ms, question = "proud", quote = t("I'm proud I didn't cry at the dentist. Almost.", "Горжусь, что не плакала у зубного. Почти."), timed = false),
            E(satBack + 28, 11, 180, Kind.TRIP, both, pic = "d-03.jpg", idea = "train", phone = 15),
            E(satBack + 36, 19, 40, Kind.BOOK, both, idea = "audiobook"),
            E(satBack + 42, 15, 60, Kind.OUTSIDE, ms, pic = "d-08.jpg", note = t("Ice cream on the bench by the flower shop.", "Мороженое на лавке у цветочного.")),
            E(satBack + 44, 18, 20, Kind.KITCHEN, ps, pic = "d-06.jpg", idea = "meal_waiter", timed = false),
            E(satBack + 56, 12, 90, Kind.SPORT, both, idea = "football"),
            E(satBack + 63, 18, 30, Kind.PLAY, both, idea = "board", phone = 8),
        )
        for (e in list) {
            val d = today.minusDays(e.back.toLong())
            if (d.isBefore(started)) continue
            dao.insert(
                Together(
                    kids = e.kids, start = at(d, e.h, if (e.back == 0) 5 else 30), minutes = e.min, timed = e.timed, phoneMinutes = if (e.timed) e.phone else 0,
                    kind = e.kind.name, note = e.note, quote = e.quote, photo = if (e.pic.isEmpty()) "" else photo(e.pic), idea = e.idea, question = e.question,
                    alone = e.alone, first = e.first,
                )
            )
        }

        listOf(
            Interest(childId = m, text = t("Horses", "Лошади"), since = at(today.minusDays(40), 12, 0)),
            Interest(childId = m, text = t("Her friend Sonya", "Подружка Соня"), since = at(today.minusDays(25), 12, 0)),
            Interest(childId = m, text = t("Drawing comics", "Рисует комиксы"), since = at(today.minusDays(12), 12, 0)),
            Interest(childId = m, text = t("Frozen", "«Холодное сердце»"), since = at(today.minusDays(200), 12, 0), until = at(today.minusDays(45), 12, 0)),
            Interest(childId = p, text = t("Diggers", "Экскаваторы"), since = at(today.minusDays(60), 12, 0)),
            Interest(childId = p, text = t("The cat", "Кот"), since = at(today.minusDays(30), 12, 0)),
            Interest(childId = p, text = t("Afraid of the hoover", "Боится пылесоса"), since = at(today.minusDays(20), 12, 0)),
        ).forEach { dao.insert(it) }

        val nextSat = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
        dao.insert(Plan(kids = both, day = nextSat.toEpochDay(), text = t("A train to anywhere", "Электричка куда-нибудь"), idea = "train"))
        dao.insert(Plan(kids = ps, day = today.plusDays(2).toEpochDay(), text = t("Diggers at the building site", "Смотреть экскаваторы на стройке"), alone = true))

        // Two weeks of evenings: a lot of phone at first, a little less lately.
        val s = c.settings.now()
        val r = Random(11)
        val apps = listOf("com.instagram.android", "com.google.android.youtube", "org.telegram.messenger", "com.android.chrome", "com.whatsapp")
        for (back in 14 downTo 1) {
            val d = today.minusDays(back.toLong())
            val h = s.hours(d)
            val weekend = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
            val base = if (weekend) 150 else 95
            val phone = (base * (0.55f + back / 14f * 0.6f) + r.nextInt(-15, 16)).toInt().coerceAtLeast(10)
            var left = phone
            val split = apps.mapIndexed { i, a ->
                val share = if (i == apps.lastIndex) left else (left * listOf(0.42f, 0.3f, 0.45f, 0.5f, 1f)[i]).toInt()
                left -= share
                a to share
            }.filter { it.second > 0 }.sortedByDescending { it.second }
            dao.put(Evening(d.toEpochDay(), h.from, h.to, phone, split.joinToString(";") { "${it.first}=${it.second}" }))
        }

        c.settings.update { it.copy(onboarded = true, startedAt = at(started, 20, 0), shield = true) }
    }
}
