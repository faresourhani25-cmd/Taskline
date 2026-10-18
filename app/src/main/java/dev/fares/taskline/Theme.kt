package dev.fares.taskline

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors: a near-black background, soft gray cards, orange for "in progress" and green for "done".
 * The app is always dark.
 */
class Palette(
    val bg: Color,
    val surface: Color,
    val raised: Color,
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
    bg = Color(0xFF0A0A0B),
    surface = Color(0xFF161619),
    raised = Color(0xFF222226),
    line = Color(0xFF2A2A2F),
    fg = Color(0xFFF2F2F3),
    dim = Color(0xFF9B9BA3),
    mute = Color(0xFF63636B),
    green = Color(0xFF3FBF6E),
    red = Color(0xFFF2564F),
    yellow = Color(0xFFF2B84B),
    orange = Color(0xFFF58A2E),
    goals = listOf(
        Color(0xFF6FA8FF), Color(0xFFE98BB8), Color(0xFFF58A2E),
        Color(0xFF5CCBB5), Color(0xFFF2B84B), Color(0xFF3FBF6E),
    ),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun TasklineTheme(content: @Composable () -> Unit) {
    val p = DarkPalette
    val scheme = darkColorScheme(
        primary = p.fg,
        onPrimary = p.bg,
        background = p.bg,
        onBackground = p.fg,
        surface = p.surface,
        onSurface = p.fg,
        surfaceVariant = p.surface,
        onSurfaceVariant = p.dim,
        outline = p.line,
        error = p.red,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
