package app.marbles.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marbles.R
import app.marbles.data.Child
import app.marbles.ui.child.ChildForm
import app.marbles.ui.components.PrimaryButton
import app.marbles.ui.jar.JarView
import app.marbles.ui.theme.U
import app.marbles.ui.theme.UType
import java.time.LocalDate

/** Two pages of why (the jar, the mirror), then the first child. */
@Composable
fun OnboardingScreen(onDone: (Child) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (page < 2) {
            Column(Modifier.weight(1f).padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center) {
                if (page == 0) JarView(939, U.c.accent, Modifier.fillMaxWidth().height(280.dp))
                else MirrorPicture()
                Spacer(Modifier.height(26.dp))
                Text(stringResource(if (page == 0) R.string.welcome_1_title else R.string.welcome_2_title), style = UType.Display)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(if (page == 0) R.string.welcome_1_text else R.string.welcome_2_text), style = UType.Sub.copy(fontSize = 16.sp, lineHeight = 24.sp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalArrangement = Arrangement.Center) {
                repeat(3) { i -> Box(Modifier.padding(4.dp).size(if (i == page) 9.dp else 7.dp).clip(CircleShape).background(if (i == page) U.c.accent else U.c.line)) }
            }
            PrimaryButton(stringResource(R.string.next), { page++ }, Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp))
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(horizontal = 28.dp, vertical = 16.dp)) {
                    Text(stringResource(R.string.welcome_3_title), style = UType.Display)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.welcome_3_text), style = UType.Sub)
                }
                ChildForm(null, 0, LocalDate.now(), Modifier.padding(horizontal = 28.dp), button = stringResource(R.string.welcome_start), onSave = onDone)
            }
        }
    }
}

/** Two evenings side by side: the phone's bar long and cold, the children's short and warm. */
@Composable
private fun MirrorPicture() {
    val rows = listOf(0.72f to 0.12f, 0.55f to 0.3f, 0.3f to 0.58f)
    Column(Modifier.fillMaxWidth().height(280.dp), verticalArrangement = Arrangement.Center) {
        rows.forEach { (phone, kids) ->
            Bar(phone, U.c.phone)
            Spacer(Modifier.height(6.dp))
            Bar(kids, U.c.lamp)
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun Bar(f: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(50)).background(U.c.surface2)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}
