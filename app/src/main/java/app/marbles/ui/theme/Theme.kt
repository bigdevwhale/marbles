package app.marbles.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.marbles.R
import app.marbles.data.Kind

/**
 * A table in the evening: dark walnut, a lamp's warm circle of light, a glass jar, and marbles in
 * every colour. The phone, in the mirror, is the one cold colour in the house.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val lamp: Color,
    val glass: Color,
    val grey: Color,
    val phone: Color,
    val phoneSoft: Color,
    val danger: Color,
)

val LampPalette = Palette(
    bg = Color(0xFF1B1512),
    surface = Color(0xFF241C17),
    surface2 = Color(0xFF2E241E),
    line = Color(0xFF3B2F27),
    ink = Color(0xFFF3E7D7),
    muted = Color(0xFFB9A794),
    faint = Color(0xFF7F6F61),
    accent = Color(0xFFE9A23B),
    accentSoft = Color(0xFF3B2B18),
    lamp = Color(0xFFFFCF87),
    glass = Color(0xFFA9CBD0),
    grey = Color(0xFF5E5550),
    phone = Color(0xFF7194C2),
    phoneSoft = Color(0xFF1F2733),
    danger = Color(0xFFE0705A),
)

/** The marble colour of each kind of time together. */
fun Kind.color(): Color = when (this) {
    Kind.PLAY -> Color(0xFFE2574C)
    Kind.OUTSIDE -> Color(0xFF5FA85A)
    Kind.TALK -> Color(0xFFEBA53C)
    Kind.BOOK -> Color(0xFF4F7FCB)
    Kind.KITCHEN -> Color(0xFFE5803C)
    Kind.MAKE -> Color(0xFF9C6CC4)
    Kind.SPORT -> Color(0xFF34AEA6)
    Kind.ROUTINE -> Color(0xFFD6B46E)
    Kind.TRIP -> Color(0xFFD7608F)
}

/** One glass colour per child, for their name and their jar's lid. */
val Glasses = listOf(
    Color(0xFFEBA53C), Color(0xFF6FA8DC), Color(0xFF7CBF6E), Color(0xFFE07A9B), Color(0xFFA98BD6), Color(0xFF4DBDB4),
)

fun glass(i: Int): Color = Glasses[((i % Glasses.size) + Glasses.size) % Glasses.size]

val LocalPalette = staticCompositionLocalOf { LampPalette }

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** An old-book serif for the big numbers and titles. */
val Cormorant = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Handwriting, for what the children said. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(26.dp)
    val RLg = RoundedCornerShape(20.dp)
    val RMd = RoundedCornerShape(16.dp)
    val RSm = RoundedCornerShape(11.dp)
}

object UType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 64.sp, lineHeight = 64.sp, color = U.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp, color = U.c.ink)
    val Name: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp, color = U.c.ink)
    val Italic: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 24.sp, color = U.c.muted)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.14.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = U.c.faint)
    val Hand: TextStyle @Composable get() = TextStyle(fontFamily = app.marbles.ui.theme.Hand, fontSize = 24.sp, lineHeight = 27.sp, color = U.c.lamp)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = U.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.sp)
}

@Composable
fun MarblesTheme(content: @Composable () -> Unit) {
    val p = LampPalette
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = p.accent,
                onPrimary = p.bg,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondary = p.glass,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface2,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
