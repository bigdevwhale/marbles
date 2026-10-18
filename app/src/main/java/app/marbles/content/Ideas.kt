package app.marbles.content

import android.content.Context
import app.marbles.data.Kind

/** A text in both languages; the app's language picks one. */
class L(val en: String, val ru: String) {
    fun get(c: Context): String = if (c.resources.configuration.locales[0].language == "ru") ru else en
}

/** How much the parent has left in them tonight. */
enum class Energy { LOW, MID, HIGH }

enum class Place { HOME, OUT, ANY }

/** The everyday stretches that can become time together. */
enum class Ritual { ROAD, MEAL, BATH, BED }

/** Something to do together, from the deck. */
class Idea(
    val key: String,
    val kind: Kind,
    val energy: Energy,
    val place: Place,
    val minutes: Int,
    /** Ages it suits, in years, inclusive. */
    val ages: IntRange,
    val title: L,
    val text: L,
    val ritual: Ritual? = null,
) {
    fun suits(age: Int) = age in ages
}

/**
 * The deck. Short on purpose: every card is something a tired parent can start in a minute, with
 * what's already in the house.
 */
object Ideas {
    private fun i(key: String, kind: Kind, energy: Energy, place: Place, minutes: Int, ages: IntRange, en: String, ru: String, enText: String, ruText: String, ritual: Ritual? = null) =
        Idea(key, kind, energy, place, minutes, ages, L(en, ru), L(enText, ruText), ritual)

    val all: List<Idea> = listOf(
        // Lying down: for an empty battery.
        i("doctor", Kind.PLAY, Energy.LOW, Place.HOME, 15, 2..7, "Doctor and patient", "Доктор и пациент",
            "You lie on the sofa and are very ill. They treat you with everything they can find. Recover slowly.",
            "Вы лежите на диване и очень больны. Ребёнок лечит вас всем, что найдёт. Выздоравливайте медленно."),
        i("back_draw", Kind.PLAY, Energy.LOW, Place.HOME, 10, 3..12, "Drawing on a back", "Рисунок на спине",
            "Take turns drawing a letter, a house or a cat on each other's back with a finger. Guess what it was.",
            "По очереди рисуйте пальцем на спине букву, домик или кота. Угадайте, что нарисовано."),
        i("tell_me", Kind.TALK, Energy.LOW, Place.HOME, 15, 3..10, "Tell me a story", "Расскажи мне сказку",
            "Swap roles: you lie down, they tell the bedtime story. Ask for one about yourself.",
            "Поменяйтесь местами: вы лежите, ребёнок рассказывает сказку. Попросите сказку про вас."),
        i("draw_me", Kind.MAKE, Energy.LOW, Place.HOME, 20, 3..12, "Draw me", "Нарисуй меня",
            "You pose, they draw your portrait. Then swap. Sign and date both; keep them.",
            "Вы позируете, ребёнок рисует ваш портрет. Потом наоборот. Подпишите и поставьте дату."),
        i("massage", Kind.PLAY, Energy.LOW, Place.HOME, 10, 4..14, "Back rub for back rub", "Массаж за массаж",
            "Five minutes each. They'll be strict about it being exactly fair.",
            "По пять минут каждому. Ребёнок будет строго следить, чтобы было честно."),
        i("read_roles", Kind.BOOK, Energy.LOW, Place.HOME, 20, 6..13, "Read in roles", "Читаем по ролям",
            "One book, every character gets a voice. They pick who they are.",
            "Одна книга, у каждого героя свой голос. Ребёнок выбирает, кем будет."),
        i("old_photos", Kind.TALK, Energy.LOW, Place.HOME, 20, 4..17, "Old photos", "Старые фотографии",
            "Scroll back to the year they were born. Tell them what they were like. They will ask for more.",
            "Отмотайте галерею к году, когда ребёнок родился. Расскажите, каким был малыш. Попросят ещё."),
        i("audiobook", Kind.BOOK, Energy.LOW, Place.HOME, 30, 5..14, "An audiobook in the dark", "Аудиокнига в темноте",
            "Lights off, a blanket, one chapter. Talk about it after.",
            "Свет выключен, плед, одна глава. Потом обсудите."),
        i("cloud", Kind.TALK, Energy.LOW, Place.ANY, 10, 3..12, "What does it look like", "На что похоже",
            "Clouds, stains on the ceiling, shadows. Each says what they see.",
            "Облака, пятна на потолке, тени. Каждый говорит, что видит."),
        i("their_game", Kind.PLAY, Energy.LOW, Place.HOME, 20, 6..17, "Their game, their rules", "Их игра, их правила",
            "Ask them to teach you the game they play on their own. Be a slow learner.",
            "Попросите научить вас игре, в которую ребёнок играет сам. Учитесь медленно."),
        i("baby_face", Kind.PLAY, Energy.LOW, Place.HOME, 10, 0..1, "Faces", "Рожицы",
            "Face to face, a hand's length away. Copy every sound and face they make.",
            "Лицом к лицу, на расстоянии ладони. Повторяйте каждый звук и каждую рожицу."),
        i("baby_talk", Kind.TALK, Energy.LOW, Place.ANY, 10, 0..2, "Narrate", "Комментатор",
            "Say out loud everything you're both doing and seeing. Pause for their answer.",
            "Проговаривайте вслух всё, что вы делаете и видите. Делайте паузы для ответа."),
        i("music_talk", Kind.TALK, Energy.LOW, Place.HOME, 20, 10..17, "Their music", "Их музыка",
            "Ask them to play you three songs they love and say why. No comments on taste.",
            "Попросите поставить три любимые песни и объяснить, почему. Без комментариев о вкусе."),

        // Some battery left.
        i("fort", Kind.PLAY, Energy.MID, Place.HOME, 30, 3..10, "A blanket fort", "Шалаш из пледов",
            "Chairs, blankets, a torch. Eat dinner inside if you dare.",
            "Стулья, пледы, фонарик. Поужинайте внутри, если решитесь."),
        i("pancakes", Kind.KITCHEN, Energy.MID, Place.HOME, 40, 3..17, "Pancakes", "Блинчики",
            "They crack the eggs and pour. You hold the pan. Ugly first pancake is theirs to eat.",
            "Ребёнок разбивает яйца и наливает тесто. Вы держите сковородку. Первый комом — ему."),
        i("cook_dinner", Kind.KITCHEN, Energy.MID, Place.HOME, 45, 8..17, "They cook tonight", "Сегодня готовит ребёнок",
            "They choose the dish and run the kitchen. You are the assistant.",
            "Ребёнок выбирает блюдо и командует на кухне. Вы — помощник."),
        i("lego", Kind.MAKE, Energy.MID, Place.HOME, 30, 3..12, "Build the same thing", "Построить одно и то же",
            "Each builds a house (or a rocket) from the same pile in ten minutes. Then a tour.",
            "Каждый за десять минут строит дом или ракету из одной кучи. Потом экскурсия."),
        i("cardboard", Kind.MAKE, Energy.MID, Place.HOME, 40, 4..10, "A box becomes", "Коробка превращается",
            "Any cardboard box: a car, a robot, a shop. Tape and markers only.",
            "Любая коробка: машина, робот, магазин. Только скотч и фломастеры."),
        i("board", Kind.PLAY, Energy.MID, Place.HOME, 30, 4..17, "A board game", "Настольная игра",
            "One game, start to finish, phones in another room.",
            "Одна игра от начала до конца, телефоны в другой комнате."),
        i("cards", Kind.PLAY, Energy.MID, Place.ANY, 20, 6..17, "A card game", "Игра в карты",
            "Teach them one you played as a child. Tell them who taught you.",
            "Научите игре, в которую играли сами в детстве. Расскажите, кто научил вас."),
        i("experiment", Kind.MAKE, Energy.MID, Place.HOME, 30, 5..12, "A kitchen experiment", "Опыт на кухне",
            "Baking soda and vinegar, an egg in salt water, a rainbow in a glass. Let them guess first.",
            "Сода с уксусом, яйцо в солёной воде, радуга в стакане. Пусть сначала угадает, что будет."),
        i("dance", Kind.SPORT, Energy.MID, Place.HOME, 15, 2..12, "Dance party", "Дискотека",
            "Three songs, lights low, everyone dances. The parent too.",
            "Три песни, приглушённый свет, танцуют все. Родитель тоже."),
        i("puppets", Kind.PLAY, Energy.MID, Place.HOME, 20, 3..8, "A sock theatre", "Театр носков",
            "Two socks, two voices, one argument to settle.",
            "Два носка, два голоса, один спор, который надо решить."),
        i("letter_gran", Kind.MAKE, Energy.MID, Place.HOME, 20, 4..12, "A letter by post", "Письмо по почте",
            "Write and draw a real letter to a grandparent. Post it together tomorrow.",
            "Напишите и нарисуйте настоящее письмо бабушке или дедушке. Отправьте завтра вместе."),
        i("plan_trip", Kind.TALK, Energy.MID, Place.HOME, 30, 7..17, "Plan a trip", "Спланировать поездку",
            "A map, a budget, a day. They plan a real Saturday out; you follow the plan.",
            "Карта, бюджет, один день. Ребёнок планирует настоящую субботу, вы следуете плану."),
        i("fix_thing", Kind.MAKE, Energy.MID, Place.HOME, 30, 6..17, "Fix something", "Починить что-то",
            "A wobbly chair, a bike chain, a loose door. They hold the tools and do the turning.",
            "Шатающийся стул, цепь велосипеда, дверца. Ребёнок держит инструмент и крутит сам."),
        i("teach_me", Kind.TALK, Energy.MID, Place.ANY, 20, 9..17, "Teach me", "Научи меня",
            "Ask them to teach you something they're good at: a trick, a level, a move.",
            "Попросите научить вас тому, что ребёнок умеет лучше: трюку, уровню, движению."),
        i("drive_talk", Kind.TALK, Energy.MID, Place.OUT, 30, 12..17, "A drive with nowhere to go", "Поездка в никуда",
            "Side by side is easier than face to face. Their music, no questions until they start.",
            "Рядом говорить легче, чем лицом к лицу. Их музыка и никаких вопросов, пока не заговорят сами."),

        // Full battery.
        i("walk_new", Kind.OUTSIDE, Energy.HIGH, Place.OUT, 60, 2..17, "A street you've never walked", "Улица, где вы не были",
            "Pick a street nearby you have never walked down. They choose every turn.",
            "Выберите улицу рядом, по которой вы никогда не ходили. Каждый поворот выбирает ребёнок."),
        i("bike", Kind.SPORT, Energy.HIGH, Place.OUT, 60, 4..17, "A bike ride", "На велосипедах",
            "Somewhere with an ice cream at the end.",
            "Туда, где в конце будет мороженое."),
        i("puddles", Kind.OUTSIDE, Energy.HIGH, Place.OUT, 30, 1..8, "Puddles", "Лужи",
            "Wellies on, the biggest puddle wins. Do not stay dry.",
            "Резиновые сапоги, побеждает самая большая лужа. Сухим не остаться."),
        i("treasure", Kind.PLAY, Energy.HIGH, Place.HOME, 40, 3..10, "A treasure hunt", "Поиск клада",
            "Five notes with clues around the house, a small treasure at the end.",
            "Пять записок с подсказками по дому, в конце маленький клад."),
        i("picnic", Kind.OUTSIDE, Energy.HIGH, Place.OUT, 90, 1..17, "A picnic", "Пикник",
            "Even in the courtyard, even in October. They pack the bag.",
            "Хоть во дворе, хоть в октябре. Сумку собирает ребёнок."),
        i("stars", Kind.OUTSIDE, Energy.HIGH, Place.OUT, 40, 5..17, "Stars", "Звёзды",
            "Go out after dark, find the Plough and the Pole Star. Hot tea in a flask.",
            "Выйдите после темноты, найдите Большую Медведицу и Полярную звезду. Горячий чай в термосе."),
        i("hide_seek", Kind.PLAY, Energy.HIGH, Place.HOME, 20, 2..9, "Hide and seek", "Прятки",
            "Lights off for the brave version. Hide badly on purpose.",
            "Для смелых — с выключенным светом. Прячьтесь плохо нарочно."),
        i("football", Kind.SPORT, Energy.HIGH, Place.OUT, 45, 4..17, "A ball in the yard", "Мяч во дворе",
            "Penalties, keepy-uppy, whatever they want. Lose some.",
            "Пенальти, чеканка, что захочет. Иногда проигрывайте."),
        i("museum", Kind.TRIP, Energy.HIGH, Place.OUT, 120, 5..17, "One room of a museum", "Один зал музея",
            "Not the whole museum: one room, and each picks a favourite thing in it.",
            "Не весь музей, а один зал. Каждый выбирает в нём любимую вещь."),
        i("market", Kind.TRIP, Energy.HIGH, Place.OUT, 60, 4..17, "The market", "На рынок",
            "They carry the money and choose one fruit neither of you has tried.",
            "Деньги несёт ребёнок и выбирает фрукт, который вы оба не пробовали."),
        i("forest", Kind.OUTSIDE, Energy.HIGH, Place.OUT, 120, 3..17, "The woods", "В лес",
            "Bring back three things: something smooth, something rough, something that smells.",
            "Принести три вещи: гладкую, шершавую и ту, что пахнет."),
        i("climb", Kind.SPORT, Energy.HIGH, Place.OUT, 90, 7..17, "Climbing or the pool", "Скалодром или бассейн",
            "Something where they can see you try hard and get it wrong.",
            "Что-то, где ребёнок увидит, как вы стараетесь и ошибаетесь."),
        i("volunteer", Kind.TRIP, Energy.HIGH, Place.OUT, 120, 9..17, "Help someone", "Помочь кому-то",
            "An animal shelter, a neighbour's shopping, a clean-up in the park.",
            "Приют для животных, покупки для соседки, уборка в парке."),
        i("train", Kind.TRIP, Energy.HIGH, Place.OUT, 180, 3..17, "A train to anywhere", "Электричка куда-нибудь",
            "Two stops out of town, a walk, back by dinner.",
            "Две остановки за город, прогулка, к ужину домой."),

        // Five minutes inside the everyday.
        i("road_spy", Kind.ROUTINE, Energy.LOW, Place.OUT, 5, 2..8, "I spy on the way", "Шпион по дороге",
            "On the way to nursery: I spy something red, something round, something that moves.",
            "По дороге в сад: найди красное, круглое, то, что движется.", Ritual.ROAD),
        i("road_story", Kind.ROUTINE, Energy.LOW, Place.OUT, 5, 4..10, "A story in turns", "Сказка по очереди",
            "One sentence each, all the way to school.",
            "По одному предложению, до самой школы.", Ritual.ROAD),
        i("road_count", Kind.ROUTINE, Energy.LOW, Place.OUT, 5, 3..7, "Count the dogs", "Считаем собак",
            "Dogs, red cars, yellow coats: pick one and count them on the way.",
            "Собаки, красные машины, жёлтые куртки: выберите одно и считайте по дороге.", Ritual.ROAD),
        i("meal_rose", Kind.ROUTINE, Energy.LOW, Place.HOME, 5, 4..17, "Rose and thorn", "Роза и шип",
            "At dinner, everyone says the best and the worst bit of their day. The parent too.",
            "За ужином каждый называет лучшее и худшее за день. Родитель тоже.", Ritual.MEAL),
        i("meal_would", Kind.ROUTINE, Energy.LOW, Place.HOME, 5, 5..17, "Would you rather", "Что бы ты выбрал",
            "Fly or be invisible? Never homework or never sweets? Defend your answer.",
            "Летать или быть невидимкой? Никогда уроков или никогда сладкого? Защитите свой ответ.", Ritual.MEAL),
        i("meal_waiter", Kind.ROUTINE, Energy.LOW, Place.HOME, 5, 3..8, "The restaurant", "Ресторан",
            "They are the waiter tonight: take the orders, bring the bread, a towel over the arm.",
            "Сегодня ребёнок — официант: принимает заказ, приносит хлеб, полотенце на руке.", Ritual.MEAL),
        i("bath_boats", Kind.ROUTINE, Energy.LOW, Place.HOME, 5, 1..6, "Boats", "Кораблики",
            "Anything that floats is a ship. Sink it with a storm.",
            "Всё, что плавает, — корабль. Утопите его штормом.", Ritual.BATH),
        i("bath_letters", Kind.ROUTINE, Energy.LOW, Place.HOME, 5, 3..6, "Foam letters", "Буквы из пены",
            "Write a letter in foam on the tiles; they wipe it and say a word that starts with it.",
            "Напишите пеной букву на кафеле; ребёнок стирает её и называет слово на эту букву.", Ritual.BATH),
        i("bed_three", Kind.ROUTINE, Energy.LOW, Place.HOME, 5, 3..12, "Three good things", "Три хорошие вещи",
            "Before lights out, each says three good things from today. Small ones count.",
            "Перед сном каждый называет три хорошие вещи за день. Мелочи считаются.", Ritual.BED),
        i("bed_parent", Kind.ROUTINE, Energy.LOW, Place.HOME, 5, 3..12, "When I was little", "В моём детстве",
            "Tell them one true story from when you were their age.",
            "Расскажите одну правдивую историю из времени, когда вам было столько же лет.", Ritual.BED),
        i("bed_dark", Kind.ROUTINE, Energy.LOW, Place.HOME, 5, 8..17, "Talk in the dark", "Разговор в темноте",
            "Sit on the edge of the bed with the light off. Older children tell more in the dark.",
            "Посидите на краю кровати с выключенным светом. В темноте старшие рассказывают больше.", Ritual.BED),
    )

    val byKey: Map<String, Idea> = all.associateBy { it.key }

    fun deck(age: Int, energy: Energy, place: Place?, rituals: Boolean = false): List<Idea> =
        all.filter { it.suits(age) && (it.ritual != null) == rituals && (rituals || it.energy == energy) && (place == null || it.place == place || it.place == Place.ANY) }
}
