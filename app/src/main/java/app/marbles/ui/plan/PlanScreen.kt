package app.marbles.ui.plan

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marbles.R
import app.marbles.container
import app.marbles.content.Energy
import app.marbles.content.Ideas
import app.marbles.data.Plan
import app.marbles.data.Together
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.Chip
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.SwitchRow
import app.marbles.ui.components.TopBar
import app.marbles.ui.components.UCard
import app.marbles.ui.components.UTextField
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.glass
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A plan was kept: it leaves the list. */
fun markDone(c: Context, p: Plan) {
    c.container.write { db.dao().update(p.copy(done = System.currentTimeMillis())) }
}

/** Planning one thing: a Saturday out, or an evening with just one of them. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(kid: Long, alone0: Boolean, ideaKey: String, house: House, go: Go) {
    val c = ctx()
    val today = house.today
    val first = house.child(kid) ?: house.young.firstOrNull() ?: house.children.firstOrNull()
    var kids by rememberSaveable { mutableStateOf(listOfNotNull(first?.id)) }
    var alone by rememberSaveable { mutableStateOf(alone0) }
    val sat = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
    var day by rememberSaveable { mutableLongStateOf(if (alone0) today.plusDays(1).toEpochDay() else sat.toEpochDay()) }
    var idea by rememberSaveable { mutableStateOf(ideaKey) }
    var text by rememberSaveable { mutableStateOf(Ideas.byKey[ideaKey]?.title?.get(c).orEmpty()) }
    var pickDay by remember { mutableStateOf(false) }
    val one = house.children.size > 1 && kids.size == 1
    val age = house.children.filter { it.id in kids }.minOfOrNull { house.age(it) } ?: 6
    val suggestions = (Ideas.deck(age, Energy.HIGH, null) + Ideas.deck(age, Energy.MID, null)).let { l ->
        val d = today.toEpochDay().toInt()
        List(minOf(4, l.size)) { l[(d * 5 + it * 7) % l.size] }.distinctBy { it.key }
    }
    val days = listOf(today, today.plusDays(1), sat, sat.plusDays(1)).distinct()

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.plan_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (alone0) Text(stringResource(R.string.plan_alone_why), style = UType.Muted)
            SectionTitle(stringResource(R.string.log_with))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                house.children.forEach { k ->
                    Chip(k.name, k.id in kids, { kids = if (k.id in kids && kids.size > 1) kids - k.id else (kids + k.id).distinct() }, glass(k.glass))
                }
            }
            if (one) SwitchRow(stringResource(R.string.log_alone), stringResource(R.string.log_alone_sub), alone, { alone = it })

            SectionTitle(stringResource(R.string.plan_when))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { d ->
                    val label = when (d) {
                        today -> stringResource(R.string.plan_today)
                        today.plusDays(1) -> stringResource(R.string.plan_tomorrow)
                        else -> T.dow(c, d) + " " + T.day(c, d)
                    }
                    Chip(label, day == d.toEpochDay(), { day = d.toEpochDay() })
                }
                val other = LocalDate.ofEpochDay(day).takeIf { it !in days }
                Chip(other?.let { T.day(c, it) } ?: stringResource(R.string.plan_other), other != null, { pickDay = true })
            }

            SectionTitle(stringResource(R.string.plan_what))
            UTextField(text, { text = it; if (Ideas.byKey[idea]?.title?.get(c) != it) idea = "" }, placeholder = stringResource(R.string.plan_what_hint))
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { i -> Chip(i.title.get(c), idea == i.key, { idea = i.key; text = i.title.get(c) }) }
            }

            val mine = house.plans.filter { it.done == 0L && !it.date.isBefore(today) }
            if (mine.isNotEmpty()) {
                SectionTitle(stringResource(R.string.plans_title))
                UCard {
                    mine.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.text, style = UType.Strong)
                                Text(T.weekday(c, p.date) + " · " + T.names(c, house.kidsOf(p)), style = UType.Muted)
                            }
                            IconButton(onClick = { c.container.write { db.dao().delete(p) } }) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = U.c.faint)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            stringResource(R.string.plan_save), {
                val p = Plan(kids = Together.kidsOf(kids), day = day, text = text.trim(), idea = idea, alone = alone && one)
                c.container.write { db.dao().insert(p) }
                go.back()
            },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            enabled = kids.isNotEmpty() && text.isNotBlank(),
        )
    }
    if (pickDay) {
        val zone = house.zone
        app.marbles.ui.components.DateTimeDialog(
            LocalDate.ofEpochDay(day).atTime(10, 0).atZone(zone).toInstant().toEpochMilli(), { pickDay = false }, future = true,
        ) { ms -> day = T.local(ms, zone).toLocalDate().toEpochDay() }
    }
}
