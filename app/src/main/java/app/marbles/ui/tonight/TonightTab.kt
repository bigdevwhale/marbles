package app.marbles.ui.tonight

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marbles.R
import app.marbles.content.Energy
import app.marbles.content.Idea
import app.marbles.content.Ideas
import app.marbles.content.Notes
import app.marbles.content.Place
import app.marbles.content.Questions
import app.marbles.content.Ritual
import app.marbles.data.Child
import app.marbles.domain.Jar
import app.marbles.domain.Weeks
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.ChildBar
import app.marbles.ui.components.IconDisc
import app.marbles.ui.components.Look
import app.marbles.ui.components.MarbleDot
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.Segmented
import app.marbles.ui.components.SoftButton
import app.marbles.ui.components.UCard
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.color
import app.marbles.ui.together.StartSheet
import java.time.LocalTime

/** The stretch of the day the clock is in, for a ritual that fits it. */
fun ritualNow(t: LocalTime = LocalTime.now()): Ritual = when (t.hour) {
    in 5..10 -> Ritual.ROAD
    in 11..18 -> Ritual.MEAL
    in 19..19 -> Ritual.BATH
    else -> Ritual.BED
}

/**
 * Tonight: what to do with the battery you have left, a five-minute ritual, a question for the edge
 * of the bed, what they're into, and who hasn't had you to themselves for a while.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TonightTab(house: House, child: Child, go: Go, onPick: (Long) -> Unit) {
    val c = ctx()
    val age = house.age(child)
    val day = house.today.toEpochDay()
    var energy by rememberSaveable { mutableStateOf(Energy.MID) }
    var outside by rememberSaveable { mutableStateOf(false) }
    var shuffle by rememberSaveable { mutableIntStateOf(0) }
    var qShuffle by rememberSaveable { mutableIntStateOf(0) }
    var starting by remember { mutableStateOf<Idea?>(null) }
    val deck = Ideas.deck(age, energy, if (outside) Place.OUT else Place.HOME).ifEmpty { Ideas.deck(age, energy, null) }
    val idea = deck.getOrNull(((day * 7 + child.id * 3 + shuffle) % deck.size.coerceAtLeast(1)).toInt())
    val ritual = Ideas.deck(age, Energy.LOW, null, rituals = true).let { l -> l.filter { it.ritual == ritualNow() }.ifEmpty { l } }
        .let { l -> l.getOrNull(((day + child.id) % l.size.coerceAtLeast(1)).toInt()) }
    val questions = Questions.forAge(age)
    val question = questions.getOrNull(((day * 3 + child.id + qShuffle) % questions.size.coerceAtLeast(1)).toInt())
    val note = Notes.forWeek(age, Jar.monday(house.today).toEpochDay() / 7)
    val interests = house.interestsOf(child.id).filter { it.current }

    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        ChildBar(house, child, go, onPick)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(4.dp))
            Text(T.weekday(c, house.today).uppercase(), style = UType.Eyebrow)
            Text(T.g(stringResource(R.string.tonight_title, child.name), child), style = UType.Display)

            SectionTitle(stringResource(R.string.tonight_battery))
            Segmented(listOf(Energy.LOW, Energy.MID, Energy.HIGH), energy, {
                stringResource(when (it) { Energy.LOW -> R.string.energy_low; Energy.MID -> R.string.energy_mid; Energy.HIGH -> R.string.energy_high })
            }, { energy = it; shuffle = 0 })
            Spacer(Modifier.height(8.dp))
            Segmented(listOf(false, true), outside, { stringResource(if (it) R.string.place_out else R.string.place_home) }, { outside = it; shuffle = 0 })
            Spacer(Modifier.height(12.dp))
            if (idea != null) UCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarbleDot(Look(idea.kind.color(), 1f, idea.key.hashCode() % 100 / 100f), size = 40.dp, shadow = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(idea.title.get(c), style = UType.Name)
                        Text(T.kind(c, idea.kind) + " · " + stringResource(R.string.about_minutes, T.span(c, idea.minutes)), style = UType.Small)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(idea.text.get(c), style = UType.Body)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(stringResource(R.string.tonight_go), { starting = idea }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
                    SoftButton(stringResource(R.string.tonight_another), { shuffle++ }, Modifier.weight(1f), icon = Icons.Rounded.Shuffle)
                }
                Spacer(Modifier.height(4.dp))
                LinkRow(stringResource(R.string.tonight_deck, deck.size)) { go.deck(child.id) }
            }

            if (ritual != null) {
                SectionTitle(stringResource(R.string.tonight_ritual))
                UCard(onClick = { go.deck(child.id, rituals = true) }) {
                    Text(stringResource(ritualLabel(ritual.ritual!!)).uppercase(), style = UType.Eyebrow.copy(color = U.c.lamp))
                    Spacer(Modifier.height(4.dp))
                    Text(ritual.title.get(c), style = UType.Name)
                    Text(ritual.text.get(c), style = UType.Muted)
                }
            }

            if (question != null) {
                SectionTitle(stringResource(R.string.tonight_question))
                UCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Bedtime, null, Modifier.size(22.dp).padding(top = 4.dp), tint = U.c.lamp)
                        Spacer(Modifier.width(10.dp))
                        Text(T.g(question.text.get(c), child), style = UType.Title.copy(fontSize = UType.Title.fontSize * 0.9f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SoftButton(stringResource(R.string.tonight_answer), { go.log(0, kid = child.id, question = question.key) }, Modifier.weight(1f), icon = Icons.Rounded.EditNote)
                        SoftButton(stringResource(R.string.tonight_other_q), { qShuffle++ }, Modifier.weight(1f), color = U.c.muted, soft = U.c.surface2, icon = Icons.Rounded.Shuffle)
                    }
                }
            }

            SectionTitle(T.g(stringResource(R.string.tonight_into, child.name), child))
            UCard(onClick = { go.child(child.id) }) {
                if (interests.isEmpty()) {
                    Text(T.g(stringResource(R.string.tonight_into_empty, child.name), child), style = UType.Muted)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        interests.forEach { Text(it.text, style = UType.Body, modifier = Modifier.clip(U.RSm).background(U.c.surface2).padding(horizontal = 12.dp, vertical = 6.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    val ask = interests[((day + qShuffle) % interests.size).toInt()]
                    Text(T.g(stringResource(R.string.tonight_ask, ask.text), child), style = UType.Italic.copy(color = U.c.lamp))
                }
            }

            OneOnOne(house, go)
            Plans(house, go)

            if (note != null) {
                SectionTitle(stringResource(R.string.tonight_note, T.ageOf(c, child, house.today)))
                UCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Lightbulb, null, Modifier.size(20.dp).padding(top = 2.dp), tint = U.c.lamp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(note.title.get(c), style = UType.Strong)
                            Spacer(Modifier.height(4.dp))
                            Text(note.text.get(c), style = UType.Muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
    starting?.let { i -> StartSheet(house, child, go, onDismiss = { starting = null }, idea = i.key) }
}

fun ritualLabel(r: Ritual): Int = when (r) {
    Ritual.ROAD -> R.string.ritual_road
    Ritual.MEAL -> R.string.ritual_meal
    Ritual.BATH -> R.string.ritual_bath
    Ritual.BED -> R.string.ritual_bed
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(U.RSm).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = UType.Button.copy(color = U.c.accent), modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.accent)
    }
}

/** With brothers and sisters about: whose turn it is to have you to themselves. */
@Composable
private fun OneOnOne(house: House, go: Go) {
    val c = ctx()
    val young = house.young
    if (young.size < 2) return
    SectionTitle(stringResource(R.string.alone_title))
    UCard {
        Text(stringResource(R.string.alone_sub), style = UType.Muted)
        Spacer(Modifier.height(6.dp))
        young.forEach { k ->
            val last = Weeks.lastAlone(k.id, house.together)
            val days = last?.let { ((house.now - it.start) / 86_400_000L).toInt() }
            val due = days == null || days > 30
            Row(Modifier.fillMaxWidth().clip(U.RSm).clickable { go.plan(k.id, alone = true) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                MarbleDot(Look(app.marbles.ui.theme.glass(k.glass), if (due) 0.3f else 1f, k.id / 7f), size = 26.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(k.name, style = UType.Strong)
                    Text(
                        when {
                            days == null -> stringResource(R.string.alone_never)
                            days == 0 -> stringResource(R.string.alone_today)
                            else -> c.resources.getQuantityString(R.plurals.alone_days, days, days)
                        },
                        style = UType.Muted.copy(color = if (due) U.c.accent else U.c.muted),
                    )
                }
                Icon(Icons.Rounded.Event, stringResource(R.string.plan_title), tint = U.c.faint)
            }
        }
    }
}

/** What's planned from today on. */
@Composable
private fun Plans(house: House, go: Go) {
    val c = ctx()
    val ahead = house.plans.filter { it.done == 0L && !it.date.isBefore(house.today.minusDays(1)) }
    SectionTitle(stringResource(R.string.plans_title))
    UCard {
        if (ahead.isEmpty()) Text(stringResource(R.string.plans_empty), style = UType.Muted)
        ahead.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconDisc(Icons.Rounded.Event, 38.dp, U.c.accent, U.c.accentSoft)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.text.ifBlank { Ideas.byKey[p.idea]?.title?.get(c).orEmpty() }, style = UType.Strong)
                    Text(T.weekday(c, p.date) + " · " + T.names(c, house.kidsOf(p)) + if (p.alone) " · " + stringResource(R.string.plan_alone_short) else "", style = UType.Muted)
                }
                if (!p.date.isAfter(house.today)) Icon(
                    Icons.Rounded.CheckCircle, stringResource(R.string.plan_done),
                    Modifier.size(34.dp).clip(U.RSm).clickable { app.marbles.ui.plan.markDone(c, p); go.log(0, kid = p.kidIds.firstOrNull() ?: 0, idea = p.idea) }.padding(4.dp),
                    tint = U.c.accent,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        SoftButton(stringResource(R.string.plans_add), { go.plan() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
    }
}
