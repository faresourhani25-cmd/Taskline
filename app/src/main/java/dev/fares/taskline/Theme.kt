package dev.fares.taskline

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Terminal colors (Gruvbox). The app is always dark; the light palette is kept but unused. */
class Palette(
    val bg: Color,
    val surface: Color,
    val line: Color,
    val fg: Color,
    val dim: Color,
    val mute: Color,
    val green: Color,
    val red: Color,
    val yellow: Color,
    val orange: Color,
    val goals: List<Color>,
) {
    fun goal(index: Int): Color = goals[((index % goals.size) + goals.size) % goals.size]
}

val DarkPalette = Palette(
    bg = Color(0xFF282828),
    surface = Color(0xFF32302F),
    line = Color(0xFF504945),
    fg = Color(0xFFEBDBB2),
    dim = Color(0xFFA89984),
    mute = Color(0xFF7C6F64),
    green = Color(0xFFB8BB26),
    red = Color(0xFFFB4934),
    yellow = Color(0xFFFABD2F),
    orange = Color(0xFFFE8019),
    goals = listOf(
        Color(0xFF83A598), Color(0xFFD3869B), Color(0xFFFE8019),
        Color(0xFF8EC07C), Color(0xFFFABD2F), Color(0xFFB8BB26),
    ),
)

val LightPalette = Palette(
    bg = Color(0xFFFBF1C7),
    surface = Color(0xFFF2E5BC),
    line = Color(0xFFD5C4A1),
    fg = Color(0xFF3C3836),
    dim = Color(0xFF665C54),
    mute = Color(0xFF928374),
    green = Color(0xFF79740E),
    red = Color(0xFF9D0006),
    yellow = Color(0xFFB57614),
    orange = Color(0xFFAF3A03),
    goals = listOf(
        Color(0xFF076678), Color(0xFF8F3F71), Color(0xFFAF3A03),
        Color(0xFF427B58), Color(0xFFB57614), Color(0xFF79740E),
    ),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun TasklineTheme(content: @Composable () -> Unit) {
    val dark = true
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.green,
            onPrimary = p.bg,
            background = p.bg,
            onBackground = p.fg,
            surface = p.surface,
            onSurface = p.fg,
            surfaceVariant = p.surface,
            onSurfaceVariant = p.dim,
            outline = p.mute,
            error = p.red,
        )
    } else {
        lightColorScheme(
            primary = p.green,
            onPrimary = p.bg,
            background = p.bg,
            onBackground = p.fg,
            surface = p.surface,
            onSurface = p.fg,
            surfaceVariant = p.surface,
            onSurfaceVariant = p.dim,
            outline = p.mute,
            error = p.red,
        )
    }
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
