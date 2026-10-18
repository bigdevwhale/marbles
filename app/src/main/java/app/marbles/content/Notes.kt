package app.marbles.content

/** A short note about what matters at an age, one a week. */
class Note(val ages: IntRange, val title: L, val text: L)

/**
 * What this age asks of a parent, in a sentence or two. Not lectures and not science: the things
 * parents tend to notice too late.
 */
object Notes {
    private fun n(ages: IntRange, en: String, ru: String, enText: String, ruText: String) = Note(ages, L(en, ru), L(enText, ruText))

    val all: List<Note> = listOf(
        n(0..0, "Your face is the toy", "Ваше лицо — главная игрушка",
            "Nothing interests a baby more than a face close by that answers. Talking back to their sounds is a whole conversation.",
            "Ничто не интересует малыша сильнее, чем близкое лицо, которое отвечает. Ответить на его звуки — это уже разговор."),
        n(0..0, "Say what you do", "Проговаривайте",
            "\"Now we put on the sock, the blue one.\" The words go in long before they come out.",
            "«Сейчас наденем носок, синий». Слова заходят задолго до того, как начнут выходить."),
        n(0..1, "Floor time", "Время на полу",
            "Get down to their level. The world looks different from the carpet, and they notice you came.",
            "Опуститесь на их уровень. С ковра мир выглядит иначе, и малыш замечает, что вы пришли."),
        n(1..2, "Again!", "Ещё!",
            "The tenth time of the same book is not boredom for them; it's how they learn. Read it again.",
            "Десятый раз одной и той же книжки для них не скука, а способ учиться. Прочитайте ещё раз."),
        n(1..2, "Let them help", "Пусть помогает",
            "Carrying one spoon to the table is a big job at two. It takes longer and it's worth it.",
            "Донести ложку до стола в два года — серьёзная работа. Это дольше, и это того стоит."),
        n(2..3, "Big feelings, small body", "Большие чувства в маленьком теле",
            "Tantrums are a storm they can't steer yet. Staying near and calm is the help.",
            "Истерика — шторм, которым ребёнок пока не умеет управлять. Помощь — быть рядом и спокойным."),
        n(3..5, "The age of why", "Возраст «почему»",
            "Hundreds of questions a day. \"Let's find out together\" is a better answer than the right one.",
            "Сотни вопросов в день. «Давай выясним вместе» лучше, чем правильный ответ."),
        n(3..5, "Pretend play is serious", "Игра «понарошку» — это серьёзно",
            "Shops, doctors and dragons are how they practise the world. Take the role they give you.",
            "Магазин, больница и драконы — так ребёнок репетирует мир. Берите роль, которую дают."),
        n(3..6, "They'll remember the feeling", "Запомнится чувство",
            "Most memories from before six fade. What stays is how it felt to be with you.",
            "Большинство воспоминаний до шести лет потом стираются. Остаётся ощущение, каково было рядом с вами."),
        n(4..6, "Rituals are anchors", "Ритуалы — это якоря",
            "The same song at bedtime, pancakes on Sunday. Small repeats make a childhood feel safe.",
            "Одна и та же песня перед сном, блины по воскресеньям. Маленькие повторы делают детство надёжным."),
        n(6..8, "School takes the day", "Школа забирает день",
            "Weekdays are short now. The walk home and dinner are where the talking happens.",
            "Будни стали короткими. Разговоры теперь случаются по дороге домой и за ужином."),
        n(6..8, "They want to be good at things", "Им важно что-то уметь",
            "Learning to ride, to swim, to tie laces. Be there for the practice, not only the result.",
            "Кататься, плавать, завязывать шнурки. Будьте рядом на тренировках, а не только при результате."),
        n(7..9, "Ask about details", "Спрашивайте о деталях",
            "\"How was school?\" gets \"fine\". \"Who sat next to you at lunch?\" gets a story.",
            "На «как в школе?» ответят «нормально». На «кто сегодня сидел с тобой за обедом?» — расскажут историю."),
        n(8..10, "Their world, their experts", "Их мир, их эксперты",
            "Football cards, a game, a series. Ask them to explain it; being the one who knows feels good.",
            "Футбольные карточки, игра, сериал. Попросите объяснить: быть тем, кто знает, приятно."),
        n(9..11, "The last easy years", "Последние лёгкие годы",
            "Many children still want their parents' company at this age, without asking. It changes soon; spend it.",
            "В этом возрасте многим детям всё ещё хочется быть с родителями, и они не стесняются. Скоро это изменится — тратьте это время."),
        n(9..12, "One on one", "Один на один",
            "Brothers and sisters change how a child talks. Time with just you is when they say more.",
            "При братьях и сёстрах ребёнок говорит иначе. Наедине с вами он рассказывает больше."),
        n(10..12, "Friends matter more", "Друзья становятся важнее",
            "It's not a rejection of you. Know their friends' names and ask about them.",
            "Это не отказ от вас. Знайте имена друзей и спрашивайте о них."),
        n(11..13, "Be the safe place", "Будьте безопасным местом",
            "When they tell you something hard, the first reaction decides whether they tell you the next thing.",
            "Когда ребёнок рассказывает что-то трудное, ваша первая реакция решает, расскажет ли он следующее."),
        n(12..14, "Side by side", "Плечом к плечу",
            "Teenagers talk more in the car, on a walk, washing up: when nobody has to look at anyone.",
            "Подростки говорят охотнее в машине, на прогулке, за мытьём посуды — когда не надо смотреть в глаза."),
        n(12..15, "Be around", "Будьте поблизости",
            "They won't ask for time. They'll talk late at night, in passing, when you happen to be there.",
            "Подростки не просят времени. Они заговорят поздно вечером, мимоходом, когда вы просто оказались рядом."),
        n(13..15, "Their music, their screens", "Их музыка, их экраны",
            "Asking what they're watching and actually watching one episode with them says more than rules.",
            "Спросить, что они смотрят, и правда посмотреть одну серию вместе — это говорит больше, чем правила."),
        n(14..16, "Invite, don't insist", "Приглашайте, не заставляйте",
            "Offer the trip, the film, the breakfast out. A no today is not a no forever.",
            "Предлагайте поездку, кино, завтрак в кафе. Сегодняшнее «нет» не значит «никогда»."),
        n(15..17, "The last Saturdays", "Последние субботы",
            "Soon there will be work, friends, another city. The Saturdays left are fewer than you think.",
            "Скоро будут работа, друзья, другой город. Суббот осталось меньше, чем кажется."),
        n(15..17, "Treat them as the adult they're becoming", "Говорите со взрослым, которым они становятся",
            "Ask their opinion on real things: a purchase, a trip, your work. They notice.",
            "Спрашивайте мнения о настоящих вещах: о покупке, поездке, вашей работе. Подростки это замечают."),
        n(16..17, "Teach the practical things", "Научите практичному",
            "Cooking a real meal, paying a bill, changing a tyre. It's time together that also sends them off ready.",
            "Приготовить настоящий обед, оплатить счёт, поменять колесо. Это и время вместе, и подготовка к самостоятельной жизни."),
    )

    /** This week's note for a child of [age]; changes every week, the same all week. */
    fun forWeek(age: Int, weekIndex: Long): Note? {
        val l = all.filter { age in it.ages }
        if (l.isEmpty()) return null
        return l[(weekIndex % l.size).toInt()]
    }
}
