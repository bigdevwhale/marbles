package app.marbles.ui.together

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PhonelinkErase
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.marbles.BuildConfig
import app.marbles.R
import app.marbles.container
import app.marbles.content.Ideas
import app.marbles.data.Child
import app.marbles.data.Sessions
import app.marbles.ui.Go
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.components.Chip
import app.marbles.ui.components.ConfirmDialog
import app.marbles.ui.components.Look
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.components.SoftButton
import app.marbles.ui.components.marble
import app.marbles.ui.components.rememberNow
import app.marbles.ui.ctx
import app.marbles.ui.theme.Cormorant
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.glass
import app.marbles.usage.Usage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Who is it with, and is it starting now or already over? */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartSheet(house: House, child: Child, go: Go, onDismiss: () -> Unit, idea: String = "") {
    val c = ctx()
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kids by remember { mutableStateOf(setOf(child.id)) }
    val picked = house.children.filter { it.id in kids }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = U.c.surface) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 26.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.start_title), style = UType.Title)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.start_with), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                house.children.forEach { k ->
                    Chip(k.name, k.id in kids, { kids = if (k.id in kids && kids.size > 1) kids - k.id else kids + k.id }, glass(k.glass))
                }
            }
            Ideas.byKey[idea]?.let {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.start_idea, it.title.get(c)), style = UType.Italic)
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(stringResource(R.string.start_now), {
                scope.launch {
                    Sessions.start(c, picked, idea)
                    onDismiss()
                    go.together()
                }
            }, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhonelinkErase)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(if (Usage.granted(c) || BuildConfig.DEMO) R.string.start_now_sub else R.string.start_now_sub_blind),
                style = UType.Small, modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(14.dp))
            SoftButton(stringResource(R.string.start_past), {
                onDismiss()
                go.log(0, kid = picked.singleOrNull()?.id ?: child.id, idea = idea)
            }, Modifier.fillMaxWidth(), icon = Icons.Rounded.EditNote)
        }
    }
}

/** The phone is put away: a big quiet clock, a marble slowly warming, and the way to finish. */
@Composable
fun TogetherScreen(house: House, go: Go) {
    val c = ctx()
    val s = house.settings.session
    val now = rememberNow(1000)
    val scope = rememberCoroutineScope()
    var cancel by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var busy by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (s.active) scope.launch { phone = withContext(Dispatchers.IO) { Sessions.phone(c, s.since, System.currentTimeMillis()) } }
    }
    if (!s.active) {
        // Finished from elsewhere (the notification, another screen): nothing to show here.
        androidx.compose.runtime.LaunchedEffect(Unit) { if (!busy) go.back() }
        Box(Modifier.fillMaxSize().background(U.c.bg))
        return
    }
    val kids = house.children.filter { ",${it.id}," in s.kids }
    val elapsed = (now - s.since).coerceAtLeast(0)
    val glow = rememberInfiniteTransition(label = "glow").animateFloat(0.55f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val lamp = U.c.lamp
    Column(
        Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(30.dp))
        Text(stringResource(R.string.together_with, T.names(c, kids)).uppercase(), style = UType.Eyebrow, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(0.5f))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            val color = kids.firstOrNull()?.let { glass(it.glass) } ?: U.c.accent
            // The marble warms up over the first hour.
            val fill = (elapsed / 3_600_000f).coerceIn(0.15f, 1f)
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(lamp.copy(alpha = 0.22f * glow.value), Color.Transparent), radius = size.minDimension / 2), radius = size.minDimension / 2)
                marble(Offset(size.width / 2, size.height / 2), size.minDimension * 0.27f, Look(color, fill, elapsed / 600_000f % 1f), shadow = true)
            }
        }
        Text(T.stopwatch(elapsed), style = UType.Hero.copy(fontFamily = Cormorant, fontSize = 72.sp), textAlign = TextAlign.Center)
        Ideas.byKey[s.idea]?.let { Text(it.title.get(c), style = UType.Italic, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.together_hint), style = UType.Sub, textAlign = TextAlign.Center)
        phone?.let { (m, n) ->
            Spacer(Modifier.height(10.dp))
            Text(
                if (n == 0) stringResource(R.string.together_clean) else stringResource(R.string.together_picked, c.resources.getQuantityString(R.plurals.times, n, n), T.span(c, m)),
                style = UType.Muted.copy(color = if (n == 0) U.c.lamp else U.c.phone), textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.together_finish), {
            if (busy) return@PrimaryButton
            busy = true
            scope.launch {
                val f = Sessions.finish(c)
                go.back()
                if (f != null) go.log(f.id) else android.widget.Toast.makeText(c, R.string.together_too_short, android.widget.Toast.LENGTH_SHORT).show()
            }
        }, Modifier.fillMaxWidth())
        TextButton(onClick = { cancel = true }, Modifier.padding(vertical = 8.dp)) { Text(stringResource(R.string.together_cancel), color = U.c.faint, style = UType.Button) }
    }
    if (cancel) ConfirmDialog(
        stringResource(R.string.together_cancel_title), stringResource(R.string.together_cancel_text), stringResource(R.string.together_cancel_go),
        onConfirm = { scope.launch { Sessions.cancel(c); c.container.changed() } }, onDismiss = { cancel = false }, danger = true,
    )
}
