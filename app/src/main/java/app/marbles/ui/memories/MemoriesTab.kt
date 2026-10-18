package app.marbles.ui.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marbles.R
import app.marbles.data.Child
import app.marbles.domain.Jar
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.ChildBar
import app.marbles.ui.components.EntryRow
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.Segmented
import app.marbles.ui.components.UCard
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import java.time.YearMonth

private enum class Show { ALL, QUOTES, FIRSTS }

/** Everything kept: the month's letter, the year's almanac, and every stretch of time together. */
@Composable
fun MemoriesTab(house: House, child: Child, go: Go, onPick: (Long) -> Unit) {
    val c = ctx()
    var show by rememberSaveable { mutableStateOf(Show.ALL) }
    val mine = house.of(child.id).filter {
        when (show) {
            Show.ALL -> true
            Show.QUOTES -> it.quote.isNotBlank()
            Show.FIRSTS -> it.first
        }
    }
    val byMonth = mine.groupBy { YearMonth.from(Jar.date(it.start, house.zone)) }
    val lastMonth = YearMonth.from(house.today).minusMonths(1)
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        ChildBar(house, child, go, onPick)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.memories_title), style = UType.Display)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UCard(Modifier.weight(1f), onClick = { go.letter(lastMonth) }) {
                        Icon(Icons.Rounded.MailOutline, null, tint = U.c.lamp)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.memories_letter), style = UType.Muted)
                        Text(T.monthName(c, lastMonth), style = UType.Name)
                    }
                    UCard(Modifier.weight(1f), onClick = { go.letter(YearMonth.from(house.today), year = true) }) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = U.c.lamp)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.memories_almanac), style = UType.Muted)
                        Text("${house.today.year}", style = UType.Name)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Segmented(listOf(Show.ALL, Show.QUOTES, Show.FIRSTS), show, {
                    stringResource(when (it) { Show.ALL -> R.string.memories_all; Show.QUOTES -> R.string.memories_quotes; Show.FIRSTS -> R.string.memories_firsts })
                }, { show = it })
            }
            if (mine.isEmpty()) item {
                Row(Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = U.c.faint)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        T.g(stringResource(when (show) { Show.ALL -> R.string.memories_empty; Show.QUOTES -> R.string.memories_empty_quotes; Show.FIRSTS -> R.string.memories_empty_firsts }, child.name), child),
                        style = UType.Sub,
                    )
                }
            }
            byMonth.forEach { (ym, list) ->
                item(key = "m$ym") {
                    SectionTitle(T.month(c, ym) + " · " + T.span(c, list.sumOf { it.minutes }))
                }
                items(list, key = { it.id }) { t -> EntryRow(t, house, { go.log(t.id) }) }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
