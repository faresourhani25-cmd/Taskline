package dev.fares.taskline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val LETTERS = "MTWTFSS"
private const val DAYS_IN_VIEW = 7

@Composable
fun TimelineScreen(state: AppState, onEdit: (String) -> Unit) {
    val p = LocalPalette.current
    val today = state.today
    val dated = remember(state.tasks) {
        state.tasks.filter { it.span() != null }.sortedWith(byStart)
    }
    val noDates = state.tasks.size - dated.size

    if (dated.isEmpty()) {
        EmptyState("No dated tasks", "Give a task a start and an end date and it shows up here as a bar.")
        return
    }

    val range = remember(dated, today) { timelineRange(dated, today) }
    val lo = range.first
    val n = daysBetween(range.first, range.second) + 1
    val todayIdx = daysBetween(lo, today)
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val todayColor = p.goal(0)

    // One week fits across the screen.
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val dayW: Dp = screenW / DAYS_IN_VIEW
    val dayPx = with(density) { dayW.toPx() }
    val viewportPx = with(density) { screenW.toPx() }
    val todayTarget = {
        ((todayIdx + 0.5f) * dayPx - viewportPx / 2f).toInt().coerceAtLeast(0)
    }
    LaunchedEffect(lo, viewportPx) {
        hScroll.scrollTo(todayTarget())
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MonthTitle(hScroll, dayPx, lo, n, Modifier.weight(1f))
            TodayPill(todayColor) { scope.launch { hScroll.animateScrollTo(todayTarget()) } }
        }
        if (noDates > 0) {
            val word = if (noDates > 1) "tasks have" else "task has"
            val pron = if (noDates > 1) "them" else "it"
            Txt(
                "$noDates $word no dates. Find $pron in List.",
                color = p.mute,
                size = 12.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth().horizontalScroll(hScroll)) {
            TimelineBody(
                tasks = dated,
                state = state,
                lo = lo,
                n = n,
                today = today,
                todayIdx = todayIdx,
                dayW = dayW,
                scroll = hScroll,
                vScroll = vScroll,
                onEdit = onEdit,
            )
        }
    }
}

/** The month title follows the part of the timeline you are looking at. Only this small part redraws while scrolling. */
@Composable
private fun MonthTitle(scroll: ScrollState, dayPx: Float, lo: LocalDate, n: Int, modifier: Modifier) {
    val first by remember(dayPx, n) {
        derivedStateOf { ((scroll.value / dayPx).toInt() + 1).coerceIn(0, n - 1) }
    }
    val shown = lo.plusDays(first.toLong())
    val label = shown.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) +
        " " + shown.year
    Txt(label, size = 20.sp, serif = true, maxLines = 1, modifier = modifier)
}

@Composable
private fun TodayPill(color: Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(p.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt("Today", color = color, size = 12.sp, bold = true, maxLines = 1)
    }
}

@Composable
private fun TimelineBody(
    tasks: List<Task>,
    state: AppState,
    lo: LocalDate,
    n: Int,
    today: LocalDate,
    todayIdx: Int,
    dayW: Dp,
    scroll: ScrollState,
    vScroll: ScrollState,
    onEdit: (String) -> Unit,
) {
    val p = LocalPalette.current
    val weekendShade = p.fg.copy(alpha = 0.04f)
    val gridColor = p.fg.copy(alpha = 0.05f)
    val monthColor = p.fg.copy(alpha = 0.14f)
    val todayColor = p.goal(0)
    val compact = dayW < 22.dp

    // Text for the day header is measured once (5 letters and 31 numbers) and reused for every day.
    val measurer = rememberTextMeasurer()
    val letterLayouts = remember(measurer) {
        val style = TextStyle(fontSize = 10.sp)
        "MTWFS".associate { c -> c to measurer.measure(c.toString(), style) }
    }
    val numberLayouts = remember(measurer, compact) {
        val style = TextStyle(fontSize = if (compact) 9.sp else 12.sp)
        (1..31).map { measurer.measure(it.toString(), style) }
    }

    Column(
        Modifier
            .width(dayW * n)
            .fillMaxHeight()
            .drawBehind {
                val w = dayW.toPx()
                for (i in 0 until n) {
                    val d = lo.plusDays(i.toLong())
                    if (isWeekend(d)) {
                        drawRect(
                            color = weekendShade,
                            topLeft = Offset(i * w, 0f),
                            size = Size(w, size.height),
                        )
                    }
                    drawLine(
                        color = if (d.dayOfMonth == 1) monthColor else gridColor,
                        start = Offset(i * w, 0f),
                        end = Offset(i * w, size.height),
                        strokeWidth = 1f,
                    )
                }
                val x = (todayIdx + 0.5f) * w
                drawLine(
                    color = todayColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f,
                )
            },
    ) {
        // Day header, drawn in one pass. It stays at the top while the rows scroll.
        Canvas(Modifier.fillMaxWidth().height(54.dp).lineBottom(p.line)) {
            val w = dayW.toPx()
            val chipTop = 27.dp.toPx()
            val chipH = 20.dp.toPx()
            val radius = CornerRadius(8.dp.toPx())
            for (i in 0 until n) {
                val d = lo.plusDays(i.toLong())
                val isToday = i == todayIdx
                val left = i * w

                val letter = letterLayouts[LETTERS[d.dayOfWeek.value - 1]]
                if (letter != null) {
                    drawText(
                        textLayoutResult = letter,
                        color = if (isToday) todayColor else p.mute,
                        topLeft = Offset(left + (w - letter.size.width) / 2f, 9.dp.toPx()),
                    )
                }

                val num = numberLayouts[d.dayOfMonth - 1]
                if (isToday) {
                    val chipW = (num.size.width + 12.dp.toPx()).coerceAtMost(w)
                    drawRoundRect(
                        color = todayColor,
                        topLeft = Offset(left + (w - chipW) / 2f, chipTop),
                        size = Size(chipW, chipH),
                        cornerRadius = radius,
                    )
                }
                drawText(
                    textLayoutResult = num,
                    color = if (isToday) p.bg else if (isWeekend(d)) p.dim else p.fg,
                    topLeft = Offset(
                        left + (w - num.size.width) / 2f,
                        chipTop + (chipH - num.size.height) / 2f,
                    ),
                )
            }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(vScroll)) {
            for (t in tasks) {
                TimelineRow(
                    task = t,
                    goal = state.goalById(t.goalId),
                    lo = lo,
                    today = today,
                    dayW = dayW,
                    scroll = scroll,
                    onEdit = { onEdit(t.id) },
                )
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}

private fun shortDate(d: LocalDate): String =
    MONTHS[d.monthValue - 1].replaceFirstChar { it.uppercase() } + " " + d.dayOfMonth

private fun rangeLabel(sp: Span): String =
    if (sp.start == sp.end) shortDate(sp.start) else shortDate(sp.start) + " \u2013 " + shortDate(sp.end)

@Composable
private fun TimelineRow(
    task: Task,
    goal: Goal?,
    lo: LocalDate,
    today: LocalDate,
    dayW: Dp,
    scroll: ScrollState,
    onEdit: () -> Unit,
) {
    val p = LocalPalette.current
    val sp = task.span() ?: return
    val startIdx = daysBetween(lo, sp.start)
    val len = daysBetween(sp.start, sp.end) + 1
    val status = task.status(today)
    val accent = if (goal != null) p.goal(goal.color) else when (status) {
        Status.OVERDUE -> p.red
        Status.ACTIVE -> p.orange
        Status.DONE -> p.green
        else -> p.dim
    }
    val shape = RoundedCornerShape(10.dp)
    val barLeft = dayW * startIdx + 1.5.dp
    // A short task still gets a card wide enough to read.
    val barWidth = maxOf(dayW * len - 3.dp, 176.dp)
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val wide = barWidth >= 230.dp

    val meta = buildAnnotatedString {
        append(rangeLabel(sp))
        if (task.done) {
            append("  ·  ")
            withStyle(SpanStyle(color = p.green)) { append("Completed") }
        } else if (status == Status.OVERDUE) {
            append("  ·  ")
            withStyle(SpanStyle(color = p.red)) { append("Overdue") }
        }
        val mins = task.minutes
        if (mins != null) {
            append("  ·  ")
            withStyle(SpanStyle(color = p.fg, fontWeight = FontWeight.SemiBold)) { append(minutesLabel(mins)) }
        }
    }

    Box(Modifier.fillMaxWidth().height(76.dp)) {
        // The card. Everything inside it is clipped to its rounded shape.
        Box(
            modifier = Modifier
                .offset(x = barLeft, y = 6.dp)
                .width(barWidth)
                .height(64.dp)
                .alpha(if (task.done) 0.55f else 1f)
                .clip(shape)
                .background(p.raised)
                .then(if (status == Status.OVERDUE) Modifier.border(1.5.dp, p.red, shape) else Modifier)
                .clickable(onClick = onEdit),
        ) {
            // The short colored line at the top left.
            Box(
                Modifier
                    .padding(start = 16.dp, top = 10.dp)
                    .width(22.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent),
            )

            // Title and dates. They stay in view while you scroll along a long card,
            // and they are always cut to the space inside the card.
            Column(
                modifier = Modifier.layout { measurable, constraints ->
                    val cardW = barWidth.roundToPx()
                    val padL = 16.dp.roundToPx()
                    val padR = (if (wide) 48.dp else 12.dp).roundToPx()
                    val top = 20.dp.roundToPx()
                    val sticky = scroll.value + 8.dp.roundToPx() - barLeft.roundToPx()
                    val maxX = maxOf(padL, cardW - padR - 120.dp.roundToPx())
                    val x = sticky.coerceIn(padL, maxX)
                    val avail = (cardW - x - padR).coerceAtLeast(0)
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = avail))
                    layout(x + placeable.width, top + placeable.height) {
                        placeable.place(x, top)
                    }
                },
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Txt(
                    task.title,
                    color = if (task.done) p.dim else p.fg,
                    size = 14.sp,
                    bold = true,
                    strike = task.done,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlagIcon(
                        color = if (task.prio > 0) prioColor(p, task.prio) else p.mute,
                        filled = task.prio > 0,
                        iconSize = 13.dp,
                    )
                    Spacer(Modifier.width(5.dp))
                    TxtRich(meta, color = p.dim, size = 11.sp, maxLines = 1)
                }
            }

            // The small menu circle at the right end, level with the title.
            if (wide) {
                Box(
                    modifier = Modifier
                        .offset {
                            val size = 24.dp.roundToPx()
                            val atEnd = barWidth.roundToPx() - 14.dp.roundToPx() - size
                            val inView = scroll.value + screenW.roundToPx() - barLeft.roundToPx() -
                                14.dp.roundToPx() - size
                            IntOffset(maxOf(minOf(atEnd, inView), 150.dp.roundToPx()), 17.dp.roundToPx())
                        }
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(p.line),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(12.dp)) {
                        val r = 1.4.dp.toPx()
                        for (k in 0..2) {
                            drawCircle(
                                color = p.dim,
                                radius = r,
                                center = Offset(size.width / 2f, size.height * (0.16f + 0.34f * k)),
                            )
                        }
                    }
                }
            }
        }
    }
}
