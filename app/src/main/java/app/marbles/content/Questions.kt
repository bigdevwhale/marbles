package app.marbles.content

/** A question for the edge of the bed, with the ages it suits. */
class Question(val key: String, val ages: IntRange, val text: L)

/** Questions that open a child up better than "how was school?". */
object Questions {
    private fun q(key: String, ages: IntRange, en: String, ru: String) = Question(key, ages, L(en, ru))

    val all: List<Question> = listOf(
        q("funny", 3..17, "What was the funniest thing today?", "Что сегодня было самым смешным?"),
        q("invisible", 4..12, "If you were invisible for a day, what would you do?", "Если бы ты был{|а} невидимкой один день, что бы ты сделал{|а}?"),
        q("superpower", 4..12, "Which superpower would you choose, and why that one?", "Какую суперсилу ты бы выбрал{|а} и почему именно её?"),
        q("animal", 3..9, "If you could be any animal tomorrow, which?", "Каким животным ты хотел{|а} бы стать завтра?"),
        q("kind", 4..17, "Who was kind to you today?", "Кто сегодня был к тебе добр?"),
        q("helped", 4..17, "Did you help anyone today?", "Ты сегодня кому-нибудь помог{|ла}?"),
        q("hard", 5..17, "What was hard today?", "Что сегодня было трудным?"),
        q("proud", 4..17, "What are you proud of this week?", "Чем ты гордишься на этой неделе?"),
        q("grown", 6..17, "What will you do when you're grown up that grown-ups don't do now?", "Что ты будешь делать, когда вырастешь, чего взрослые сейчас не делают?"),
        q("rule", 5..17, "If you were in charge of the house for a day, what would the rules be?", "Если бы ты был{|а} главн{ым|ой} в доме на один день, какие были бы правила?"),
        q("dream", 4..17, "What did you dream about last?", "Что тебе снилось в последний раз?"),
        q("scared", 4..14, "Is there anything you're scared of right now?", "Чего ты сейчас боишься?"),
        q("friend", 4..17, "What do you like most about your best friend?", "Что тебе больше всего нравится в твоём лучшем друге?"),
        q("me_child", 5..17, "What do you think I liked at your age?", "Как ты думаешь, что мне нравилось в твоём возрасте?"),
        q("parent_better", 7..17, "What could I do better as a parent?", "Что мне стоит делать лучше как родителю?"),
        q("perfect_day", 4..17, "What would a perfect day look like?", "Каким был бы идеальный день?"),
        q("change_world", 7..17, "If you could change one thing in the world, what?", "Если бы ты мог{|ла} изменить в мире одну вещь, что бы это было?"),
        q("time_machine", 6..17, "A time machine: forward or back? Where to?", "Машина времени: вперёд или назад? Куда именно?"),
        q("happy_when", 4..17, "When do you feel happiest?", "Когда ты чувствуешь себя счастливее всего?"),
        q("learn", 6..17, "What would you like to learn that school doesn't teach?", "Чему бы ты хотел{|а} научиться, чему не учат в школе?"),
        q("song", 6..17, "Which song is stuck in your head lately?", "Какая песня у тебя сейчас в голове?"),
        q("island", 5..17, "Three things you'd take to a desert island?", "Какие три вещи ты взял{|а} бы на необитаемый остров?"),
        q("worry", 8..17, "Is anything on your mind that you haven't told anyone?", "Есть что-то, о чём ты думаешь, но никому не говорил{|а}?"),
        q("mistake", 7..17, "What mistake taught you something?", "Какая ошибка тебя чему-то научила?"),
        q("unfair", 6..17, "What felt unfair lately?", "Что в последнее время казалось несправедливым?"),
        q("someone_new", 6..17, "Who would you like to get to know better?", "С кем бы тебе хотелось познакомиться поближе?"),
        q("memory", 4..17, "What's your favourite memory of us together?", "Какое твоё любимое воспоминание о нас вместе?"),
        q("wish", 3..12, "If you found a magic lamp, what would your three wishes be?", "Если бы ты наш{ёл|ла} волшебную лампу, какие три желания ты бы загадал{|а}?"),
        q("colour", 3..7, "What colour is today?", "Какого цвета был сегодняшний день?"),
        q("toy", 3..7, "What would your toys say about you if they could talk?", "Что сказали бы о тебе игрушки, если бы умели говорить?"),
        q("monster", 3..8, "If a monster lived under the bed, what would its name be?", "Если бы под кроватью жил монстр, как бы его звали?"),
        q("bored", 6..17, "What do you do when you're bored?", "Что ты делаешь, когда скучно?"),
        q("weekend", 4..17, "What should we do together this weekend?", "Чем нам заняться вместе в эти выходные?"),
        q("job", 6..17, "Which job would you try for one week?", "Какую профессию ты попробовал{|а} бы на одну неделю?"),
        q("brave", 5..17, "When were you brave?", "Когда ты был{|а} смел{ым|ой}?"),
        q("online", 10..17, "What's the best thing you saw online this week?", "Что лучшего ты видел{|а} в интернете на этой неделе?"),
        q("future_self", 10..17, "What would you tell yourself at five?", "Что бы ты сказал{|а} себе пятилетне{му|й}?"),
        q("admire", 9..17, "Who do you admire, and what for?", "Кем ты восхищаешься и за что?"),
        q("thanks", 4..17, "What are you thankful for today?", "За что ты сегодня благодар{ен|на}?"),
        q("laugh_me", 6..17, "What do I do that makes you laugh?", "Что я делаю такого, от чего тебе смешно?"),
    )

    val byKey: Map<String, Question> = all.associateBy { it.key }

    fun forAge(age: Int) = all.filter { age in it.ages }
}
