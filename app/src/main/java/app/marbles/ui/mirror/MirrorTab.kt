package app.marbles.ui.mirror

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.marbles.BuildConfig
import app.marbles.R
import app.marbles.container
import app.marbles.data.Evening
import app.marbles.data.Hours
import app.marbles.domain.Jar
import app.marbles.domain.Mirror
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.InfoRow
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.SwitchRow
import app.marbles.ui.components.UCard
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.glass
import app.marbles.usage.Usage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An evening in the mirror: the phone's minutes and the children's, within the family hours. */
class Mirrored(val e: Evening, val together: Int, val perChild: List<Pair<app.marbles.data.Child, Int>>) {
    val window: Int get() = (e.to - e.from).coerceAtLeast(1)
}

fun mirrored(e: Evening, house: House): Mirrored {
    val w = Mirror.window(e.date, Hours(e.from, e.to), house.zone)
    return Mirrored(e, Mirror.togetherIn(w, house.together), house.children.map { it to Mirror.togetherIn(w, house.together, it.id) })
}

/**
 * The honest mirror. Two bars side by side for every evening: the phone, and the children. No
 * advice, no scores: the numbers say it.
 */
@Composable
fun MirrorTab(house: House, go: Go) {
    val c = ctx()
    var granted by remember { mutableStateOf(Usage.granted(c) || BuildConfig.DEMO) }
    var live by remember { mutableStateOf<Evening?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = Usage.granted(c) || BuildConfig.DEMO }
    LaunchedEffect(granted, house.now) {
        if (granted && !BuildConfig.DEMO) {
            live = withContext(Dispatchers.IO) {
                runCatching { Usage.record(c, c.container.db.dao(), house.settings) }
                val h = house.settings.hours(house.today)
                // Today counts once the family hours have begun.
                if (Mirror.window(house.today, h, house.zone).first < System.currentTimeMillis()) Usage.evening(c, house.today, house.settings) else null
            }
        }
    }
    val evenings = (listOfNotNull(live) + house.evenings.filter { it.day != live?.day }).sortedByDescending { it.day }.map { mirrored(it, house) }
    val s = house.settings

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.mirror_title), style = UType.Display)
            Text(
                stringResource(R.string.mirror_hours, T.hours(c, s.weekdayHours.from, s.weekdayHours.to), T.hours(c, s.weekendHours.from, s.weekendHours.to)),
                style = UType.Muted, modifier = Modifier.clip(U.RSm).clickable { go.settings() }.padding(vertical = 4.dp),
            )

            if (!granted) {
                Spacer(Modifier.height(16.dp))
                UCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Visibility, null, tint = U.c.phone)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.mirror_ask_title), style = UType.Strong)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.mirror_ask_text), style = UType.Body)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp).padding(top = 2.dp), tint = U.c.faint)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.mirror_private), style = UType.Muted)
                    }
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(stringResource(R.string.mirror_ask_go), {
                        runCatching { c.startActivity(Usage.askIntent(c)) }.onFailure { runCatching { c.startActivity(Usage.askFallback()) } }
                    }, Modifier.fillMaxWidth(), color = U.c.phone)
                }
            }

            val last = evenings.firstOrNull()
            if (last != null) {
                SectionTitle(
                    when {
                        last.e.date == house.today -> stringResource(R.string.mirror_today, T.hours(c, last.e.from, last.e.to))
                        last.e.date == house.today.minusDays(1) -> stringResource(R.string.mirror_yesterday, T.hours(c, last.e.from, last.e.to))
                        else -> T.weekday(c, last.e.date) + ", " + T.hours(c, last.e.from, last.e.to)
                    }
                )
                UCard { Big(last, house) }
            } else if (granted) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.mirror_none_yet), style = UType.Sub)
            }

            if (evenings.size > 1) {
                SectionTitle(stringResource(R.string.mirror_week))
                UCard {
                    Legend()
                    Spacer(Modifier.height(8.dp))
                    evenings.take(7).forEach { DayRow(it, house) }
                }
                Totals(evenings, house)
            }

            val week = evenings.filter { !it.e.date.isBefore(house.today.minusDays(6)) }
            val apps = week.flatMap { it.e.appMinutes }.groupBy { it.first }.map { (p, l) -> p to l.sumOf { it.second } }.sortedByDescending { it.second }.take(5)
            if (apps.isNotEmpty()) {
                SectionTitle(stringResource(R.string.mirror_apps))
                UCard {
                    apps.forEach { (pkg, m) -> AppRow(pkg, m, apps.first().second) }
                }
            }

            SectionTitle(stringResource(R.string.mirror_shield))
            UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                SwitchRow(
                    stringResource(R.string.shield_switch), stringResource(R.string.shield_switch_sub, s.shieldMinutes), s.shield,
                    { on -> c.container.write { settings.update { it.copy(shield = on) } } }, color = U.c.phone,
                )
                InfoRow(Icons.Rounded.Schedule, stringResource(R.string.mirror_hours_edit), T.hours(c, s.weekdayHours.from, s.weekdayHours.to) + " · " + T.hours(c, s.weekendHours.from, s.weekendHours.to), tint = U.c.phone, soft = U.c.phoneSoft, onClick = { go.settings() })
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Shield, null, Modifier.size(15.dp).padding(top = 2.dp), tint = U.c.faint)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.mirror_footnote), style = UType.Small)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

/** The evening in two big bars. */
@Composable
private fun Big(m: Mirrored, house: House) {
    val c = ctx()
    val phone = m.e.phoneMinutes
    Row(verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PhoneAndroid, null, Modifier.size(16.dp), tint = U.c.phone)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.mirror_phone), style = UType.Muted)
            }
            Text(T.span(c, phone), style = UType.Title.copy(color = U.c.phone))
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.mirror_children), style = UType.Muted)
            Text(T.span(c, m.together), style = UType.Title.copy(color = U.c.lamp))
        }
    }
    Spacer(Modifier.height(12.dp))
    Bars(phone, m.together, m.window, 14.dp)
    Spacer(Modifier.height(12.dp))
    val kids = m.perChild.filter { !Jar.grown(it.first, m.e.date) }
    if (kids.size > 1) {
        kids.forEach { (k, min) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(glass(k.glass)))
                Spacer(Modifier.width(8.dp))
                Text(k.name, style = UType.Body, modifier = Modifier.weight(1f))
                Text(if (min == 0) "—" else T.span(c, min), style = UType.Body.copy(color = if (min == 0) U.c.faint else U.c.ink))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
    // The sentence, as plain as it gets.
    val names = T.names(c, kids.map { it.first })
    Text(
        stringResource(R.string.mirror_sentence, T.span(c, phone), if (m.together == 0) stringResource(R.string.mirror_zero) else T.span(c, m.together), names),
        style = UType.Italic,
    )
}

/** Phone above, children below, both out of the whole family window. */
@Composable
private fun Bars(phone: Int, together: Int, window: Int, h: androidx.compose.ui.unit.Dp) {
    Column {
        Track(phone.toFloat() / window, U.c.phone, h)
        Spacer(Modifier.height(5.dp))
        Track(together.toFloat() / window, U.c.lamp, h)
    }
}

@Composable
private fun Track(f: Float, color: Color, h: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxWidth().height(h).clip(RoundedCornerShape(50)).background(U.c.surface2)) {
        if (f > 0f) Box(Modifier.fillMaxWidth(f.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
private fun Legend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(U.c.phone))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.mirror_phone), style = UType.Small)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(U.c.lamp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.mirror_children), style = UType.Small)
    }
}

@Composable
private fun DayRow(m: Mirrored, house: House) {
    val c = ctx()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (m.e.date == house.today) stringResource(R.string.mirror_today_short) else T.dow(c, m.e.date),
            style = UType.Muted, modifier = Modifier.width(54.dp), maxLines = 1,
        )
        Column(Modifier.weight(1f)) { Bars(m.e.phoneMinutes, m.together, m.window, 7.dp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.width(66.dp), horizontalAlignment = Alignment.End) {
            Text(T.span(c, m.e.phoneMinutes), style = UType.Small.copy(color = U.c.phone), maxLines = 1)
            Text(T.span(c, m.together), style = UType.Small.copy(color = U.c.lamp), maxLines = 1)
        }
    }
}

/** This week against the one before, without a verdict. */
@Composable
private fun Totals(evenings: List<Mirrored>, house: House) {
    val c = ctx()
    val thisWeek = evenings.filter { !it.e.date.isBefore(house.today.minusDays(6)) }
    val before = evenings.filter { it.e.date.isBefore(house.today.minusDays(6)) && !it.e.date.isBefore(house.today.minusDays(13)) }
    val phone = thisWeek.sumOf { it.e.phoneMinutes }
    val kids = thisWeek.sumOf { it.together }
    Spacer(Modifier.height(10.dp))
    UCard {
        Text(stringResource(R.string.mirror_totals, thisWeek.size), style = UType.Eyebrow)
        Spacer(Modifier.height(6.dp))
        Row {
            Column(Modifier.weight(1f)) {
                Text(T.span(c, phone), style = UType.Number.copy(color = U.c.phone))
                Text(stringResource(R.string.mirror_phone), style = UType.Small)
            }
            Column(Modifier.weight(1f)) {
                Text(T.span(c, kids), style = UType.Number.copy(color = U.c.lamp))
                Text(stringResource(R.string.mirror_children), style = UType.Small)
            }
        }
        if (before.size >= 3) {
            // Compared per evening, since the two weeks may have a different number of them.
            val was = before.sumOf { it.e.phoneMinutes } / before.size
            val now = phone / thisWeek.size.coerceAtLeast(1)
            val wasKids = before.sumOf { it.together } / before.size
            val nowKids = kids / thisWeek.size.coerceAtLeast(1)
            Spacer(Modifier.height(10.dp))
            Text(change(c, R.string.mirror_change_phone, now - was) + " " + change(c, R.string.mirror_change_kids, nowKids - wasKids), style = UType.Muted)
        }
    }
}

private fun change(c: android.content.Context, id: Int, d: Int): String = when {
    d == 0 -> c.getString(id, c.getString(R.string.mirror_same))
    d > 0 -> c.getString(id, c.getString(R.string.mirror_more, T.span(c, d)))
    else -> c.getString(id, c.getString(R.string.mirror_less, T.span(c, -d)))
}

@Composable
private fun AppRow(pkg: String, minutes: Int, max: Int) {
    val c = ctx()
    var icon by remember(pkg) { mutableStateOf<Bitmap?>(null) }
    val label = remember(pkg) { Usage.label(c, pkg) }
    LaunchedEffect(pkg) { icon = withContext(Dispatchers.IO) { Usage.icon(c, pkg, 96) } }
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(U.RSm).background(U.c.surface2), contentAlignment = Alignment.Center) {
            icon?.let { Image(it.asImageBitmap(), null, Modifier.size(30.dp)) } ?: Icon(Icons.Rounded.PhoneAndroid, null, Modifier.size(16.dp), tint = U.c.faint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(label, style = UType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(T.span(c, minutes), style = UType.Body.copy(color = U.c.phone))
            }
            Spacer(Modifier.height(4.dp))
            Track(minutes.toFloat() / max.coerceAtLeast(1), U.c.phone.copy(alpha = 0.7f), 4.dp)
        }
    }
}
