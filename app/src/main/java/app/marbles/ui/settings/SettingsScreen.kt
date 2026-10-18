package app.marbles.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.marbles.BuildConfig
import app.marbles.R
import app.marbles.container
import app.marbles.data.Backup
import app.marbles.data.Everything
import app.marbles.data.Hours
import app.marbles.domain.Nudges
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.Language
import app.marbles.ui.T
import app.marbles.ui.components.ConfirmDialog
import app.marbles.ui.components.InfoRow
import app.marbles.ui.components.SectionTitle
import app.marbles.ui.components.Segmented
import app.marbles.ui.components.Stepper
import app.marbles.ui.components.SwitchRow
import app.marbles.ui.components.TimeDialog
import app.marbles.ui.components.TopBar
import app.marbles.ui.components.UCard
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.usage.Usage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Pick { WD_FROM, WD_TO, WE_FROM, WE_TO, EVENING }

@Composable
fun SettingsScreen(house: House, go: Go) {
    val c = ctx()
    val container = c.container
    val s = house.settings
    var pick by remember { mutableStateOf<Pick?>(null) }
    var restoring by remember { mutableStateOf<android.net.Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var granted by remember { mutableStateOf(Usage.granted(c)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = Usage.granted(c) }
    fun set(change: (app.marbles.data.Settings) -> app.marbles.data.Settings) = container.write { settings.update(change) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        container.scope.launch {
            val dao = container.db.dao()
            val ok = runCatching {
                withContext(Dispatchers.IO) { Backup.write(c, uri, Everything(dao.children(), dao.together(), dao.interests(), dao.plans(), dao.evenings()), container.vault) }
            }.isSuccess
            withContext(Dispatchers.Main) { message = c.getString(if (ok) R.string.backup_done else R.string.backup_failed) }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoring = uri }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionTitle(stringResource(R.string.set_jar))
            UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                Stepper(stringResource(R.string.set_goal), T.span(c, s.weekGoal), { set { it.copy(weekGoal = (it.weekGoal - 30).coerceAtLeast(30)) } }, { set { it.copy(weekGoal = (it.weekGoal + 30).coerceAtMost(20 * 60)) } })
                Text(stringResource(R.string.set_goal_sub), style = UType.Muted, modifier = Modifier.padding(bottom = 8.dp))
            }

            SectionTitle(stringResource(R.string.set_hours))
            UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.set_hours_sub), style = UType.Muted, modifier = Modifier.padding(top = 8.dp))
                InfoRow(Icons.Rounded.NightsStay, stringResource(R.string.set_weekdays), T.hours(c, s.weekdayHours.from, s.weekdayHours.to), onClick = { pick = Pick.WD_FROM })
                InfoRow(Icons.Rounded.WbSunny, stringResource(R.string.set_weekends), T.hours(c, s.weekendHours.from, s.weekendHours.to), onClick = { pick = Pick.WE_FROM })
            }

            SectionTitle(stringResource(R.string.mirror_title))
            UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                InfoRow(
                    Icons.Rounded.Visibility, stringResource(R.string.set_usage),
                    stringResource(if (granted || BuildConfig.DEMO) R.string.set_usage_on else R.string.set_usage_off),
                    tint = U.c.phone, soft = U.c.phoneSoft,
                    onClick = { runCatching { c.startActivity(Usage.askIntent(c)) }.onFailure { runCatching { c.startActivity(Usage.askFallback()) } } },
                )
                SwitchRow(stringResource(R.string.shield_switch), stringResource(R.string.shield_switch_sub, s.shieldMinutes), s.shield, { on -> set { it.copy(shield = on) } }, color = U.c.phone)
                if (s.shield) Stepper(
                    stringResource(R.string.set_shield_minutes), T.span(c, s.shieldMinutes),
                    { set { it.copy(shieldMinutes = (it.shieldMinutes - 5).coerceAtLeast(5)) } }, { set { it.copy(shieldMinutes = (it.shieldMinutes + 5).coerceAtMost(15)) } },
                    color = U.c.phone,
                )
            }

            SectionTitle(stringResource(R.string.set_reminders))
            UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                SwitchRow(stringResource(R.string.set_evening), stringResource(R.string.set_evening_sub), s.evening, { on -> set { it.copy(evening = on) } })
                if (s.evening) {
                    val learned = Nudges.eveningAt(house.together, s.eveningAt, house.now, house.zone)
                    InfoRow(
                        Icons.Rounded.Schedule, stringResource(R.string.set_evening_at),
                        if (learned != s.eveningAt) stringResource(R.string.set_evening_learned, T.clock(c, learned)) else T.clock(c, s.eveningAt),
                        onClick = { pick = Pick.EVENING },
                    )
                }
                SwitchRow(stringResource(R.string.set_weekend), stringResource(R.string.set_weekend_sub), s.weekend, { on -> set { it.copy(weekend = on) } })
                SwitchRow(stringResource(R.string.set_letter), stringResource(R.string.set_letter_sub), s.letter, { on -> set { it.copy(letter = on) } })
            }

            SectionTitle(stringResource(R.string.set_language))
            var lang by remember { mutableStateOf(Language.current()) }
            Segmented(listOf(Language.SYSTEM, Language.EN, Language.RU), lang, {
                when (it) { Language.EN -> "English"; Language.RU -> "Русский"; else -> stringResource(R.string.set_lang_system) }
            }, { lang = it; Language.set(it) })

            SectionTitle(stringResource(R.string.set_data))
            UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                InfoRow(Icons.Rounded.Download, stringResource(R.string.backup_export), stringResource(R.string.backup_export_sub), onClick = { export.launch("marbles-" + java.time.LocalDate.now() + ".zip") })
                InfoRow(Icons.Rounded.Upload, stringResource(R.string.backup_import), stringResource(R.string.backup_import_sub), onClick = { import.launch(arrayOf("application/zip", "application/octet-stream")) })
            }
            message?.let { Text(it, style = UType.Body.copy(color = U.c.accent), modifier = Modifier.padding(top = 10.dp)) }

            SectionTitle(stringResource(R.string.set_about))
            Text(stringResource(R.string.set_privacy), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            Text("Marbles " + BuildConfig.VERSION_NAME + " · Stillcraft", style = UType.Small)
            Spacer(Modifier.height(30.dp))
        }
    }
    when (pick) {
        // Family hours are picked as a pair: the start, then the end.
        Pick.WD_FROM -> TimeDialog(s.weekdayHours.from, { pick = null }) { m -> set { it.copy(weekdayHours = Hours(m, maxOf(it.weekdayHours.to, m + 30))) }; pick = Pick.WD_TO }
        Pick.WD_TO -> TimeDialog(s.weekdayHours.to.coerceAtMost(23 * 60 + 59), { pick = null }) { m -> set { it.copy(weekdayHours = Hours(it.weekdayHours.from, maxOf(m, it.weekdayHours.from + 15))) } }
        Pick.WE_FROM -> TimeDialog(s.weekendHours.from, { pick = null }) { m -> set { it.copy(weekendHours = Hours(m, maxOf(it.weekendHours.to, m + 30))) }; pick = Pick.WE_TO }
        Pick.WE_TO -> TimeDialog(s.weekendHours.to.coerceAtMost(23 * 60 + 59), { pick = null }) { m -> set { it.copy(weekendHours = Hours(it.weekendHours.from, maxOf(m, it.weekendHours.from + 15))) } }
        Pick.EVENING -> TimeDialog(s.eveningAt, { pick = null }) { m -> set { it.copy(eveningAt = m) } }
        null -> Unit
    }
    restoring?.let { uri ->
        ConfirmDialog(
            stringResource(R.string.backup_import_title), stringResource(R.string.backup_import_text), stringResource(R.string.backup_import_go),
            onConfirm = {
                container.scope.launch {
                    val ok = runCatching {
                        val (all, files) = withContext(Dispatchers.IO) { Backup.read(c, uri) }
                        container.vault.wipe()
                        container.vault.adopt(files)
                        container.db.dao().replaceAll(all)
                        container.settings.update { it.copy(onboarded = true, startedAt = if (it.startedAt > 0) it.startedAt else System.currentTimeMillis()) }
                    }.isSuccess
                    container.changed()
                    withContext(Dispatchers.Main) { message = c.getString(if (ok) R.string.backup_restored else R.string.backup_bad) }
                }
            },
            onDismiss = { restoring = null }, danger = true,
        )
    }
}
