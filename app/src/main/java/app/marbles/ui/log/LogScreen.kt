package app.marbles.ui.log

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.marbles.R
import app.marbles.container
import app.marbles.content.Ideas
import app.marbles.content.Questions
import app.marbles.data.Kind
import app.marbles.data.Together
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.Chip
import app.marbles.ui.components.ConfirmDialog
import app.marbles.ui.components.DateTimeDialog
import app.marbles.ui.components.InfoRow
import app.marbles.ui.components.Look
import app.marbles.ui.components.MarbleDot
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.SoftButton
import app.marbles.ui.components.Stepper
import app.marbles.ui.components.SwitchRow
import app.marbles.ui.components.Thumb
import app.marbles.ui.components.TopBar
import app.marbles.ui.components.UCard
import app.marbles.ui.components.UTextField
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.color
import app.marbles.ui.theme.glass
import java.io.File

/**
 * Writing down time together, new or just finished: with whom, what it was, how long, one picture
 * from the phone's camera, and what the child said.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(id: Long, kid: Long, ideaKey: String, questionKey: String, house: House, go: Go) {
    val c = ctx()
    val container = c.container
    val old = house.entry(id)
    val idea = Ideas.byKey[old?.idea ?: ideaKey]
    val question = Questions.byKey[old?.question ?: questionKey]
    val firstKid = house.child(kid) ?: house.young.firstOrNull() ?: house.children.firstOrNull()
    val defaultMinutes = idea?.minutes ?: if (question != null) 10 else 30

    var kids by rememberSaveable { mutableStateOf(old?.kidIds?.toList() ?: listOfNotNull(firstKid?.id)) }
    var kind by rememberSaveable { mutableStateOf(old?.kindOf ?: idea?.kind ?: if (question != null) Kind.TALK else Kind.PLAY) }
    var minutes by rememberSaveable { mutableIntStateOf(old?.minutes ?: defaultMinutes) }
    var start by rememberSaveable { mutableLongStateOf(old?.start ?: (System.currentTimeMillis() - defaultMinutes * 60_000L)) }
    var note by rememberSaveable { mutableStateOf(old?.note ?: "") }
    var quote by rememberSaveable { mutableStateOf(old?.quote ?: "") }
    var photo by rememberSaveable { mutableStateOf(old?.photo ?: "") }
    var first by rememberSaveable { mutableStateOf(old?.first ?: false) }
    var alone by rememberSaveable { mutableStateOf(old?.alone ?: false) }
    var pickDate by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    // Pictures taken here but not saved yet, to clean up if the screen is left.
    var fresh by remember { mutableStateOf(listOf<String>()) }
    val one = house.children.size > 1 && kids.size == 1
    val pickedKids = house.children.filter { it.id in kids }
    val about = pickedKids.singleOrNull()

    var shooting by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = shooting
        shooting = null
        if (ok && f != null && f.length() > 0) {
            val name = container.vault.adoptFile(f, "jpg")
            fresh = fresh + name
            photo = name
        } else f?.delete()
    }
    fun shoot() {
        val f = container.vault.capture()
        shooting = f
        val uri: Uri = FileProvider.getUriForFile(c, c.packageName + ".files", f)
        runCatching { camera.launch(uri) }.onFailure { android.widget.Toast.makeText(c, R.string.no_camera, android.widget.Toast.LENGTH_LONG).show() }
    }

    fun save() {
        val t = Together(
            id = old?.id ?: 0, kids = Together.kidsOf(kids), start = start, minutes = minutes.coerceAtLeast(1),
            timed = old?.timed ?: false, phoneMinutes = old?.phoneMinutes ?: 0, kind = kind.name, note = note.trim(), quote = quote.trim(),
            photo = photo, idea = old?.idea ?: ideaKey, question = old?.question ?: questionKey, alone = alone && one, first = first,
        )
        val dropped = (fresh + listOfNotNull(old?.photo)).filter { it.isNotEmpty() && it != photo }
        container.write {
            if (old == null) db.dao().insert(t) else db.dao().update(t)
            dropped.forEach { vault.delete(it) }
        }
        go.back()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (old == null) R.string.log_new else R.string.log_title), {
            fresh.forEach { container.vault.delete(it) }
            go.back()
        }) {
            if (old != null) IconButton(onClick = { delete = true }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            question?.let {
                UCard {
                    Text(stringResource(R.string.log_question).uppercase(), style = UType.Eyebrow)
                    Spacer(Modifier.height(4.dp))
                    Text(T.g(it.text.get(c), about), style = UType.Title)
                }
            }
            idea?.let {
                Text(it.title.get(c), style = UType.Title)
                Text(it.text.get(c), style = UType.Muted)
            }
            if (old?.timed == true) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PhoneAndroid, null, Modifier.size(18.dp), tint = if (old.phoneMinutes > 0) U.c.phone else U.c.lamp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (old.phoneMinutes > 0) stringResource(R.string.log_phone, T.span(c, old.phoneMinutes)) else stringResource(R.string.log_phone_none),
                        style = UType.Muted,
                    )
                }
            }

            SectionTitle(stringResource(R.string.log_with))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                house.children.forEach { k ->
                    Chip(k.name, k.id in kids, { kids = if (k.id in kids && kids.size > 1) kids - k.id else (kids + k.id).distinct() }, glass(k.glass))
                }
            }

            SectionTitle(stringResource(R.string.log_what))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kind.entries.forEach { k -> KindChip(k, k == kind) { kind = k } }
            }

            SectionTitle(stringResource(R.string.log_when))
            UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                InfoRow(Icons.Rounded.Event, stringResource(R.string.log_started), T.weekday(c, T.local(start).toLocalDate()) + ", " + T.time(c, start), onClick = { pickDate = true })
                Stepper(stringResource(R.string.log_minutes), T.span(c, minutes), { minutes = (minutes - if (minutes > 60) 15 else 5).coerceAtLeast(5) }, { minutes += if (minutes >= 60) 15 else 5 })
            }

            SectionTitle(stringResource(R.string.log_memory))
            if (photo.isNotEmpty()) {
                Box {
                    Thumb(photo, Modifier.fillMaxWidth().height(220.dp).clip(U.RMd).border(1.dp, U.c.line, U.RMd), full = true)
                    IconButton(onClick = { photo = "" }, Modifier.align(Alignment.TopEnd).padding(6.dp).clip(U.RSm).background(U.c.bg.copy(alpha = 0.7f))) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.log_photo_remove), tint = U.c.ink)
                    }
                }
                Spacer(Modifier.height(10.dp))
            } else {
                SoftButton(stringResource(R.string.log_photo), ::shoot, Modifier.fillMaxWidth(), icon = Icons.Rounded.CameraAlt)
                Spacer(Modifier.height(10.dp))
            }
            UTextField(
                quote, { quote = it }, minLines = 2,
                placeholder = if (question != null) stringResource(R.string.log_answer_hint) else T.g(stringResource(R.string.log_quote_hint), about),
            )
            Spacer(Modifier.height(10.dp))
            UTextField(note, { note = it }, placeholder = stringResource(R.string.log_note_hint), minLines = 2)
            Spacer(Modifier.height(6.dp))
            SwitchRow(stringResource(R.string.log_first), stringResource(R.string.log_first_sub), first, { first = it })
            if (one) SwitchRow(stringResource(R.string.log_alone), stringResource(R.string.log_alone_sub), alone, { alone = it })
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(stringResource(R.string.save), ::save, Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), enabled = kids.isNotEmpty())
    }
    if (pickDate) DateTimeDialog(start, { pickDate = false }) { start = it }
    if (delete && old != null) ConfirmDialog(
        stringResource(R.string.log_delete_title), stringResource(R.string.log_delete_text), stringResource(R.string.delete),
        onConfirm = {
            container.write {
                db.dao().delete(old)
                vault.delete(old.photo)
            }
            go.back()
        },
        onDismiss = { delete = false }, danger = true,
    )
}

@Composable
fun KindChip(k: Kind, on: Boolean, onClick: () -> Unit) {
    val c = ctx()
    Row(
        Modifier.clip(U.RSm).background(if (on) k.color().copy(alpha = 0.16f) else U.c.surface)
            .border(1.dp, if (on) k.color().copy(alpha = 0.6f) else U.c.line, U.RSm)
            .clickable(onClick = onClick).padding(start = 8.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarbleDot(Look(k.color(), if (on) 1f else 0.55f, k.ordinal / 9f), size = 20.dp)
        Spacer(Modifier.width(6.dp))
        Text(T.kind(c, k), style = UType.Body.copy(color = if (on) U.c.ink else U.c.muted))
    }
}
