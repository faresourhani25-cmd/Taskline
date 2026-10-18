package dev.fares.taskline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** App text. Sans-serif by default, serif for titles. */
@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: TextUnit = 14.sp,
    bold: Boolean = false,
    strike: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    serif: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        lineHeight = size * 1.3f,
        fontFamily = if (serif) FontFamily.Serif else FontFamily.Default,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Same as [Txt] for text with several colors. */
@Composable
fun TxtRich(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: TextUnit = 14.sp,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        lineHeight = size * 1.3f,
        fontFamily = FontFamily.Default,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A rounded pill button. */
@Composable
fun TermButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    enabled: Boolean = true,
    filled: Boolean = false,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    val c = if (enabled) color else p.mute
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (filled) c else p.raised)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, color = if (filled) p.bg else c, bold = filled, maxLines = 1)
    }
}

/** A thin line along the bottom edge, used as a row separator. */
fun Modifier.lineBottom(color: Color): Modifier = this.drawBehind {
    val y = size.height - 0.5f
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1f,
    )
}

/** A rounded progress bar. */
@Composable
fun ProgressTrack(percent: Int, color: Color, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(p.raised),
    ) {
        Box(
            Modifier
                .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color),
        )
    }
}

@Composable
fun EmptyState(title: String, text: String) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Txt(title, size = 22.sp, serif = true)
        Spacer(Modifier.height(6.dp))
        Txt(text, color = p.dim)
    }
}

/** Priority levels: 0 none, 1 low, 2 medium, 3 high. */
fun prioColor(p: Palette, level: Int): Color = when (level) {
    1 -> p.green
    2 -> p.yellow
    3 -> p.red
    else -> p.mute
}

fun prioName(level: Int): String = when (level) {
    1 -> "Low"
    2 -> "Medium"
    3 -> "High"
    else -> "None"
}

/** A flag drawn from a pole and a pennant. Just an outline when not filled. */
@Composable
fun FlagIcon(color: Color, modifier: Modifier = Modifier, filled: Boolean = true, iconSize: Dp = 20.dp) {
    Canvas(modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.09f
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.10f),
            end = Offset(w * 0.22f, h * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val flag = Path().apply {
            moveTo(w * 0.22f, h * 0.16f)
            lineTo(w * 0.88f, h * 0.16f)
            lineTo(w * 0.68f, h * 0.38f)
            lineTo(w * 0.88f, h * 0.60f)
            lineTo(w * 0.22f, h * 0.60f)
            close()
        }
        if (filled) {
            drawPath(flag, color)
        } else {
            drawPath(flag, color, style = Stroke(width = stroke, join = StrokeJoin.Round))
        }
    }
}
