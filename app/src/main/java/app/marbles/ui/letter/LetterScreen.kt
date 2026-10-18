package app.marbles.ui.letter

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.marbles.R
import app.marbles.container
import app.marbles.domain.Letter
import app.marbles.make.Almanac
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.Bar
import app.marbles.ui.components.Look
import app.marbles.ui.components.MarbleDot
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.Thumb
import app.marbles.ui.components.TopBar
import app.marbles.ui.components.UCard
import app.marbles.ui.components.titleOf
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.color
import app.marbles.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

/** The month's letter (or the year's almanac): what went into the jar, said back to you. */
@Composable
fun LetterScreen(start: YearMonth, year: Boolean, house: House, go: Go) {
    val c = ctx()
    val container = c.container
    var month by remember { mutableStateOf(start) }
    var making by remember { mutableStateOf(false) }
    val from = if (year) YearMonth.of(month.year, 1) else month
    val to = if (year) YearMonth.of(month.year, 12) else month
    val letter = remember(from, to, house) { Letter.of(from, to, house.children, house.together, house.evenings, house.settings, house.zone) }
    val title = if (year) "${month.year}" else T.month(c, month)
    val now = YearMonth.from(house.today)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(if (year) R.string.memories_almanac else R.string.memories_letter), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = if (year) month.minusYears(1) else month.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.earlier), tint = U.c.muted)
                }
                Text(title, style = UType.Display, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                val later = if (year) month.year < now.year else month.isBefore(now)
                IconButton(onClick = { if (later) month = if (year) month.plusYears(1) else month.plusMonths(1) }, enabled = later) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.later), tint = if (later) U.c.muted else U.c.line)
                }
            }
            if (letter.empty) {
                Spacer(Modifier.height(30.dp))
                Text(stringResource(R.string.letter_empty), style = UType.Sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                return@Column
            }
            Text(stringResource(R.string.letter_dear), style = UType.Italic, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    R.string.letter_opening,
                    c.resources.getQuantityString(R.plurals.days_together, letter.days, letter.days),
                    T.span(c, letter.minutes),
                    c.resources.getQuantityString(R.plurals.marbles, letter.entries.size, letter.entries.size),
                ),
                style = UType.Body,
            )
            letter.longest?.let {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.letter_longest, titleOf(it), T.day(c, it.start), T.span(c, it.minutes)), style = UType.Body)
            }

            // Every stretch of time as a marble, in the order it happened.
            Spacer(Modifier.height(14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                letter.entries.forEach { t -> MarbleDot(Look(t.kindOf.color(), 1f, (t.id % 11) / 11f), size = 22.dp) }
            }

            SectionTitle(stringResource(R.string.letter_with_whom))
            UCard {
                val max = letter.perChild.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                letter.perChild.forEach { (k, m) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(k.name, style = UType.Strong, modifier = Modifier.weight(1f))
                        Text(if (m == 0) "—" else T.span(c, m), style = UType.Body)
                    }
                    Spacer(Modifier.height(4.dp))
                    Bar(m.toFloat() / max, color = glass(k.glass), track = U.c.surface2)
                    Spacer(Modifier.height(10.dp))
                }
                letter.kinds.take(3).let { top ->
                    Text(stringResource(R.string.letter_mostly, top.joinToString(", ") { T.kind(c, it.first).lowercase() }), style = UType.Muted)
                }
            }

            if (letter.quotes.isNotEmpty()) {
                SectionTitle(stringResource(R.string.letter_said))
                letter.quotes.forEach { t ->
                    val k = house.kidsOf(t).firstOrNull()
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text("«" + t.quote + "»", style = UType.Hand)
                        Text("— " + (k?.name ?: "") + ", " + T.day(c, t.start), style = UType.Small)
                    }
                }
            }
            if (letter.firsts.isNotEmpty()) {
                SectionTitle(stringResource(R.string.letter_firsts))
                letter.firsts.forEach { t ->
                    Row(Modifier.padding(vertical = 5.dp).clip(U.RSm).clickable { go.log(t.id) }, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp), tint = U.c.lamp)
                        Spacer(Modifier.width(10.dp))
                        Text(titleOf(t) + " · " + T.names(c, house.kidsOf(t)) + " · " + T.day(c, t.start), style = UType.Body)
                    }
                }
            }
            if (letter.photos.isNotEmpty()) {
                SectionTitle(stringResource(R.string.letter_pictures))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    letter.photos.forEach { t ->
                        Thumb(t.photo, Modifier.size(120.dp).clip(U.RMd).border(1.dp, U.c.line, U.RMd).clickable { go.log(t.id) })
                    }
                }
            }
            if (letter.evenings > 0) {
                SectionTitle(stringResource(R.string.mirror_title))
                UCard {
                    Text(stringResource(R.string.letter_mirror, letter.evenings, T.span(c, letter.phoneMinutes), T.span(c, letter.familyTogether)), style = UType.Body)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            stringResource(if (making) R.string.letter_making else R.string.letter_share), {
                if (making || letter.empty) return@PrimaryButton
                making = true
                container.scope.launch {
                    val f = withContext(Dispatchers.IO) { runCatching { Almanac(c, container.vault).make(letter, house.children.filter { k -> letter.perChild.any { it.first.id == k.id && it.second > 0 } }, title) }.getOrNull() }
                    withContext(Dispatchers.Main) {
                        making = false
                        if (f == null) return@withContext
                        val uri = FileProvider.getUriForFile(c, c.packageName + ".files", f)
                        val send = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        runCatching { c.startActivity(Intent.createChooser(send, null)) }
                    }
                }
            },
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            enabled = !letter.empty, icon = Icons.Rounded.IosShare,
        )
    }
}
