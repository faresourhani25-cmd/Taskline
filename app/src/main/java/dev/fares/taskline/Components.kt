package dev.fares.taskline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Monospace text in the terminal style. */
@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.fg,
    size: TextUnit = 14.sp,
    bold: Boolean = false,
    strike: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        lineHeight = size * 1.4f,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
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
        lineHeight = size * 1.4f,
        fontFamily = FontFamily.Monospace,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

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
    val shape = RoundedCornerShape(2.dp)
    val c = if (enabled) color else p.mute
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (filled) c else Color.Transparent)
            .border(1.dp, c, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, color = if (filled) p.bg else c, bold = filled)
    }
}

/** A dashed line along the bottom edge, like a row separator in a terminal table. */
fun Modifier.dashedBottom(color: Color): Modifier = this.drawBehind {
    val y = size.height - 0.5f
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
    )
}

@Composable
fun EmptyState(title: String, text: String) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Txt("# $title", bold = true)
        Spacer(Modifier.height(4.dp))
        Txt(text, color = p.dim)
    }
}
