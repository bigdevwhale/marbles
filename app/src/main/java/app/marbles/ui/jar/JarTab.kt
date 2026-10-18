package app.marbles.ui.jar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marbles.R
import app.marbles.data.Child
import app.marbles.domain.Jar
import app.marbles.domain.Week
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.Bar
import app.marbles.ui.components.ChildBar
import app.marbles.ui.components.Hollow
import app.marbles.ui.components.Look
import app.marbles.ui.components.MarbleDot
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.UCard
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.color
import app.marbles.ui.theme.glass

/** The look of a week's marble. */
fun Week.look(): Look = Look(kind?.color() ?: androidx.compose.ui.graphics.Color.Gray, if (kind == null) 0f else 0.25f + 0.75f * fill, (monday.toEpochDay() % 13) / 13f)

@Composable
fun JarTab(house: House, child: Child, go: Go, onPick: (Long) -> Unit) {
    val c = ctx()
    val today = house.today
    val left = Jar.left(child, today)
    val grown = Jar.grown(child, today)
    val week = house.thisWeek(child)
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        ChildBar(house, child, go, onPick)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(6.dp))
            Text(
                T.g(stringResource(R.string.jar_eyebrow, child.name, T.ageOf(c, child, today)), child).uppercase(),
                style = UType.Eyebrow, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            if (grown) {
                Text(stringResource(R.string.jar_grown_title), style = UType.Display, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(T.g(stringResource(R.string.jar_grown_text, child.name), child), style = UType.Sub, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            } else {
                Text("$left", style = UType.Hero, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(
                    T.g(c.resources.getQuantityString(R.plurals.saturdays_left, left, child.name), child),
                    style = UType.Italic, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
            }
            JarView(left, glass(child.glass), Modifier.fillMaxWidth().height(330.dp).padding(top = 6.dp), seed = child.id)

            // This week's marble, in your hand.
            UCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarbleDot(week.look(), size = 52.dp, shadow = true)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.jar_this_week), style = UType.Eyebrow)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (week.minutes == 0) stringResource(R.string.jar_week_empty)
                            else stringResource(R.string.jar_week_of, T.span(c, week.minutes), T.span(c, house.settings.weekGoal)),
                            style = UType.Strong,
                        )
                        Spacer(Modifier.height(8.dp))
                        Bar(week.fill, color = week.kind?.color() ?: U.c.accent, track = U.c.surface2)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(if (week.fill >= 1f) R.string.jar_week_full else R.string.jar_week_hint),
                    style = UType.Muted,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.together_start), go.start, Modifier.fillMaxWidth(), icon = Icons.Rounded.Timer)
            }

            Tray(house, child, go)

            SectionTitle(stringResource(R.string.jar_why))
            UCard {
                val share = (Jar.goneShare(child, today) * 100).toInt()
                if (!grown) Text(T.g(stringResource(R.string.jar_share, share, child.name), child), style = UType.Body)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.jar_tail_end), style = UType.Muted)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** The weeks taken out of the jar since it was started: coloured where there was time together, grey where not. */
@Composable
private fun Tray(house: House, child: Child, go: Go) {
    val c = ctx()
    val tray = house.tray(child)
    val before = (Jar.gone(child, house.today) - (tray.size - 1)).coerceAtLeast(0)
    SectionTitle(stringResource(R.string.jar_tray))
    UCard {
        val shown = tray.takeLast(52)
        val perRow = 9
        // Newest first, so this week is top left; empty places at the end of the last row.
        val rows = shown.reversed().chunked(perRow)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 0 until perRow) {
                    val w = row.getOrNull(i)
                    Box(Modifier.size(34.dp).clip(CircleShape).then(if (w != null) Modifier.clickable { go.week(child.id, w.monday) } else Modifier), contentAlignment = Alignment.Center) {
                        when {
                            w == null -> Spacer(Modifier.size(28.dp))
                            w.current && w.minutes == 0 -> Hollow(size = 28.dp)
                            else -> MarbleDot(w.look(), size = 28.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.height(6.dp))
        val lived = tray.count { it.minutes > 0 }
        Text(
            c.resources.getQuantityString(R.plurals.tray_count, tray.size, tray.size, lived),
            style = UType.Muted,
        )
        if (before > 0) Text(c.resources.getQuantityString(R.plurals.tray_before, before, before), style = UType.Small, modifier = Modifier.padding(top = 4.dp))
    }
}
