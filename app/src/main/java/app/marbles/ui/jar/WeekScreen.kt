package app.marbles.ui.jar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marbles.R
import app.marbles.domain.Jar
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.EntryRow
import app.marbles.ui.components.MarbleDot
import app.marbles.ui.components.SoftButton
import app.marbles.ui.components.TopBar
import app.marbles.ui.components.UCard
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import java.time.LocalDate

/** One marble from the tray, opened: the week, and the time together that coloured it. */
@Composable
fun WeekScreen(childId: Long, monday: LocalDate, house: House, go: Go) {
    val c = ctx()
    val child = house.child(childId) ?: return
    val week = house.tray(child).firstOrNull { it.monday == monday } ?: app.marbles.domain.Weeks.week(monday, house.of(childId), house.settings.weekGoal, house.today)
    val entries = house.of(childId).filter { Jar.monday(Jar.date(it.start, house.zone)) == monday }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(T.day(c, monday) + " – " + T.day(c, monday.plusDays(6)), go::back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            UCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarbleDot(week.look(), size = 64.dp, shadow = true)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(T.g(stringResource(R.string.week_of_age, child.name, T.ageOf(c, child, monday.plusDays(5))), child), style = UType.Muted)
                        Text(
                            if (week.minutes == 0) stringResource(R.string.week_none) else T.span(c, week.minutes),
                            style = UType.Title,
                        )
                        week.kind?.let { Text(stringResource(R.string.week_mostly, T.kind(c, it)), style = UType.Muted) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            entries.forEach { t -> EntryRow(t, house, { go.log(t.id) }) }
            if (entries.isEmpty()) Text(
                stringResource(if (week.current) R.string.week_empty_now else R.string.week_empty_past),
                style = UType.Sub, modifier = Modifier.padding(vertical = 12.dp),
            )
            Spacer(Modifier.height(12.dp))
            SoftButton(stringResource(R.string.week_add), { go.log(0, kid = childId) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            Spacer(Modifier.height(30.dp))
        }
    }
}
