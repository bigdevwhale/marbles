package app.marbles.ui.child

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marbles.R
import app.marbles.container
import app.marbles.data.Child
import app.marbles.data.Interest
import app.marbles.data.Together
import app.marbles.domain.Jar
import app.marbles.domain.Weeks
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.ConfirmDialog
import app.marbles.ui.components.InfoRow
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.Segmented
import app.marbles.ui.components.Stat
import app.marbles.ui.components.TopBar
import app.marbles.ui.components.UCard
import app.marbles.ui.components.UTextField
import app.marbles.ui.ctx
import app.marbles.ui.theme.Glasses
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import java.time.LocalDate

/** A child's page: what they're into now, a few numbers, and their name, birthday and colour. */
@Composable
fun ChildScreen(id: Long, house: House, go: Go, onPick: (Long) -> Unit) {
    val c = ctx()
    val container = c.container
    val child = house.child(id)
    var deleting by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(if (child == null) stringResource(R.string.child_new) else child.name, if (house.children.isEmpty()) null else go::back) {
            if (child != null) TextButton(onClick = { deleting = true }) { Text(stringResource(R.string.delete), color = U.c.muted) }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (child != null) {
                Numbers(child, house)
                Interests(child, house)
                SectionTitle(stringResource(R.string.child_about))
            }
            ChildForm(child, house.children.size, house.today) { k ->
                container.write {
                    if (child == null) {
                        val newId = db.dao().insert(k)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onPick(newId) }
                    } else db.dao().update(k)
                }
                go.back()
            }
        }
    }
    if (deleting && child != null) ConfirmDialog(
        stringResource(R.string.child_delete_title, child.name), stringResource(R.string.child_delete_text), stringResource(R.string.delete),
        onConfirm = {
            container.write {
                val dao = db.dao()
                dao.together().filter { it.of(child.id) }.forEach { t ->
                    val rest = t.kidIds - child.id
                    if (rest.isEmpty()) { dao.delete(t); vault.delete(t.photo) } else dao.update(t.copy(kids = Together.kidsOf(rest)))
                }
                dao.plans().filter { ",${child.id}," in it.kids }.forEach { p ->
                    val rest = p.kidIds - child.id
                    if (rest.isEmpty()) dao.delete(p) else dao.update(p.copy(kids = Together.kidsOf(rest)))
                }
                dao.deleteInterestsOf(child.id)
                dao.delete(child)
            }
            go.home()
        },
        onDismiss = { deleting = false }, danger = true,
    )
}

@Composable
private fun Numbers(child: Child, house: House) {
    val c = ctx()
    val month = Weeks.recent(child.id, house.together, house.now, 30)
    val lastAlone = Weeks.lastAlone(child.id, house.together)
    UCard {
        Row {
            Stat(if (Jar.grown(child, house.today)) "0" else "${Jar.left(child, house.today)}", stringResource(R.string.child_left), Modifier.weight(1f), U.c.lamp)
            Stat(T.span(c, month), stringResource(R.string.child_month), Modifier.weight(1f))
            Stat(lastAlone?.let { T.day(c, it.start) } ?: "—", stringResource(R.string.child_alone_last), Modifier.weight(1f))
        }
    }
}

/** What they're into right now: worth asking about, and worth seeing change over the years. */
@Composable
private fun Interests(child: Child, house: House) {
    val c = ctx()
    val container = c.container
    val all = house.interestsOf(child.id)
    val now = all.filter { it.current }
    val past = all.filter { !it.current }
    var text by remember { mutableStateOf("") }
    var showPast by remember { mutableStateOf(false) }
    SectionTitle(T.g(stringResource(R.string.tonight_into, child.name), child))
    UCard {
        Text(T.g(stringResource(R.string.into_why, child.name), child), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        now.forEach { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(i.text, style = UType.Body, modifier = Modifier.weight(1f))
                Text(T.day(c, i.since), style = UType.Small)
                IconButton(onClick = { container.write { db.dao().update(i.copy(until = System.currentTimeMillis())) } }) {
                    Icon(Icons.Rounded.History, stringResource(R.string.into_passed), tint = U.c.faint)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            UTextField(text, { text = it }, Modifier.weight(1f), placeholder = stringResource(R.string.into_hint))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                val t = text.trim()
                if (t.isNotEmpty()) container.write { db.dao().insert(Interest(childId = child.id, text = t, since = System.currentTimeMillis())) }
                text = ""
            }) { Icon(Icons.Rounded.Add, stringResource(R.string.into_add), tint = U.c.accent) }
        }
        if (past.isNotEmpty()) {
            TextButton(onClick = { showPast = !showPast }) {
                Text(c.resources.getQuantityString(R.plurals.into_past, past.size, past.size), color = U.c.muted, style = UType.Button)
            }
            if (showPast) past.forEach { i ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(i.text, style = UType.Body.copy(color = U.c.muted), modifier = Modifier.weight(1f))
                    Text(T.day(c, i.since) + " – " + T.day(c, i.until), style = UType.Small)
                    IconButton(onClick = { container.write { db.dao().delete(i) } }) { Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = U.c.faint) }
                }
            }
        }
    }
}

/** The form itself, also the onboarding's last page. */
@Composable
fun ChildForm(child: Child?, count: Int, today: LocalDate, modifier: Modifier = Modifier, button: String = "", onSave: (Child) -> Unit) {
    val c = ctx()
    var name by remember { mutableStateOf(child?.name ?: "") }
    var born by remember { mutableLongStateOf(child?.born ?: today.minusYears(4).toEpochDay()) }
    var gender by remember { mutableStateOf(child?.gender ?: "") }
    var glass by remember { mutableIntStateOf(child?.glass ?: (count % Glasses.size)) }
    var picking by remember { mutableStateOf(false) }
    var bornSet by remember { mutableStateOf(child != null) }
    Column(modifier) {
        Text(stringResource(R.string.child_name).uppercase(), style = UType.Eyebrow, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
        UTextField(name, { name = it }, placeholder = stringResource(R.string.child_name_hint))
        Spacer(Modifier.height(10.dp))
        InfoRow(
            Icons.Rounded.Cake, stringResource(R.string.child_born),
            if (bornSet) T.date(c, LocalDate.ofEpochDay(born)) + " · " + T.age(c, Jar.age(LocalDate.ofEpochDay(born), today)) else stringResource(R.string.child_born_pick),
            onClick = { picking = true },
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.child_gender_q).uppercase(), style = UType.Eyebrow, modifier = Modifier.padding(bottom = 8.dp))
        Segmented(listOf("GIRL", "BOY", ""), gender, {
            stringResource(when (it) { "GIRL" -> R.string.child_girl; "BOY" -> R.string.child_boy; else -> R.string.child_none })
        }, { gender = it })
        Text(stringResource(R.string.child_gender_why), style = UType.Small, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.child_glass).uppercase(), style = UType.Eyebrow, modifier = Modifier.padding(bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Glasses.forEachIndexed { i, col ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(col).then(if (i == glass) Modifier.border(3.dp, U.c.ink.copy(alpha = 0.7f), CircleShape) else Modifier)
                        .clickable { glass = i },
                    contentAlignment = Alignment.Center,
                ) { if (i == glass) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
        Spacer(Modifier.height(26.dp))
        PrimaryButton(
            button.ifEmpty { stringResource(R.string.save) },
            { onSave((child ?: Child(name = "", born = 0)).copy(name = name.trim(), born = born, gender = gender, glass = glass)) },
            Modifier.fillMaxWidth(), enabled = name.isNotBlank() && bornSet,
        )
        Spacer(Modifier.height(24.dp))
    }
    if (picking) BirthdayDialog(LocalDate.ofEpochDay(born), today, { picking = false }) { born = it.toEpochDay(); bornSet = true }
}

/** A date picker for the birthday: nothing in the future. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayDialog(initial: LocalDate, today: LocalDate, onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate().isAfter(today)
        },
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPicked(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.save), color = U.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted) } },
        colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = U.c.surface),
    ) { androidx.compose.material3.DatePicker(state, showModeToggle = true) }
}
