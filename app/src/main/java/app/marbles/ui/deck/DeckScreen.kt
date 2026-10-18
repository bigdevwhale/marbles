package app.marbles.ui.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marbles.R
import app.marbles.content.Energy
import app.marbles.content.Idea
import app.marbles.content.Ideas
import app.marbles.content.Ritual
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.Look
import app.marbles.ui.components.MarbleDot
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.Segmented
import app.marbles.ui.components.SoftButton
import app.marbles.ui.components.TopBar
import app.marbles.ui.components.UCard
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.color
import app.marbles.ui.together.StartSheet
import app.marbles.ui.tonight.ritualLabel

/** The whole deck for a child's age: by how much battery is left, or the five-minute rituals. */
@Composable
fun DeckScreen(childId: Long, rituals: Boolean, house: House, go: Go) {
    val c = ctx()
    val child = house.child(childId) ?: return
    val age = house.age(child)
    var mode by rememberSaveable { mutableStateOf(if (rituals) 3 else 1) }
    var starting by remember { mutableStateOf<Idea?>(null) }
    val cards = when (mode) {
        0 -> Ideas.deck(age, Energy.LOW, null)
        1 -> Ideas.deck(age, Energy.MID, null)
        2 -> Ideas.deck(age, Energy.HIGH, null)
        else -> Ideas.deck(age, Energy.LOW, null, rituals = true)
    }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(T.g(stringResource(R.string.deck_title, child.name), child), go::back)
        Segmented(listOf(0, 1, 2, 3), mode, {
            stringResource(when (it) { 0 -> R.string.energy_low; 1 -> R.string.energy_mid; 2 -> R.string.energy_high; else -> R.string.deck_rituals })
        }, { mode = it }, Modifier.padding(horizontal = 20.dp))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            if (mode == 3) {
                Ritual.entries.forEach { r ->
                    val l = cards.filter { it.ritual == r }
                    if (l.isEmpty()) return@forEach
                    item(key = r.name) { SectionTitle(stringResource(ritualLabel(r))) }
                    items(l, key = { it.key }) { i -> Card(i, { starting = i }, { go.plan(childId, idea = i.key) }) }
                }
            } else {
                item { Spacer(Modifier.height(12.dp)) }
                if (cards.isEmpty()) item { Text(stringResource(R.string.deck_empty), style = UType.Sub) }
                items(cards, key = { it.key }) { i -> Card(i, { starting = i }, { go.plan(childId, idea = i.key) }) }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
    starting?.let { i -> StartSheet(house, child, go, onDismiss = { starting = null }, idea = i.key) }
}

@Composable
private fun Card(i: Idea, onStart: () -> Unit, onPlan: () -> Unit) {
    val c = ctx()
    UCard(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MarbleDot(Look(i.kind.color(), 1f, (i.key.hashCode() and 0xff) / 255f), size = 30.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(i.title.get(c), style = UType.Name)
                Text(T.kind(c, i.kind) + " · " + stringResource(R.string.about_minutes, T.span(c, i.minutes)), style = UType.Small)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(i.text.get(c), style = UType.Body)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton(stringResource(R.string.tonight_go), onStart, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow)
            SoftButton(stringResource(R.string.deck_plan), onPlan, Modifier.weight(1f), color = U.c.muted, soft = U.c.surface2, icon = Icons.Rounded.Event)
        }
    }
}
