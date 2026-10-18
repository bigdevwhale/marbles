package app.marbles.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PhoneAndroid
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marbles.R
import app.marbles.container
import app.marbles.content.Ideas
import app.marbles.content.Questions
import app.marbles.data.Together
import app.marbles.ui.House
import app.marbles.ui.T
import app.marbles.ui.ctx
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import app.marbles.ui.theme.color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A picture from the vault, loaded off the main thread. */
@Composable
fun Thumb(name: String, modifier: Modifier = Modifier, full: Boolean = false) {
    val c = ctx()
    var bmp by remember(name) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(name) {
        bmp = withContext(Dispatchers.IO) { if (full) c.container.vault.full(name, 1600) else c.container.vault.small(name) }
    }
    Box(modifier.background(U.c.surface2)) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
    }
}

/** What an entry is called: the idea it came from, the question it answers, its note if short, or its kind. */
fun titleText(c: Context, t: Together): String =
    Ideas.byKey[t.idea]?.title?.get(c)
        ?: Questions.byKey[t.question]?.let { c.getString(R.string.entry_question) }
        ?: t.note.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() && it.length <= 40 }
        ?: T.kind(c, t.kindOf)

@Composable
fun titleOf(t: Together): String = titleText(ctx(), t)

/** One stretch of time together in a list: its marble, what it was, with whom, and what was said. */
@Composable
fun EntryRow(t: Together, house: House, onClick: () -> Unit, showKids: Boolean = true, size: Dp = 34.dp) {
    val c = ctx()
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MarbleDot(Look(t.kindOf.color(), 1f, (t.id % 17) / 17f), size = size)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titleOf(t), style = UType.Strong, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (t.first) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.log_first), Modifier.size(15.dp), tint = U.c.lamp)
                }
            }
            val who = if (showKids) T.names(c, house.kidsOf(t)) + " · " else ""
            Text(who + T.day(c, t.start) + " · " + T.span(c, t.minutes), style = UType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.timed && t.phoneMinutes > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PhoneAndroid, null, Modifier.size(13.dp), tint = U.c.phone)
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.entry_phone, T.span(c, t.phoneMinutes)), style = UType.Small.copy(color = U.c.phone))
            }
            if (t.quote.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("«" + t.quote + "»", style = UType.Hand.copy(fontSize = 21.sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        if (t.photo.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            Thumb(t.photo, Modifier.size(58.dp).clip(U.RSm).border(1.dp, U.c.line, U.RSm))
        }
    }
}
