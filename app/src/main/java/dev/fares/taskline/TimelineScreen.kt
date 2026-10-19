package dev.fares.taskline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val LETTERS = "MTWTFSS"
private val RANGE_LABELS = listOf("1w", "2w", "3w")
private val RANGE_DAYS = listOf(7, 14, 21)

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

    var zoom by rememberSaveable { mutableStateOf(1) }
    val range = timelineRange(dated, today)
    val lo = range.first
    val n = daysBetween(lo, range.second) + 1
    val todayIdx = daysBetween(lo, today)
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val todayColor = p.goal(0)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The range pills decide how many days fit across the screen.
        val dayW: Dp = maxWidth / RANGE_DAYS[zoom.coerceIn(0, 2)]
        val dayPx = with(density) { dayW.toPx() }
        val viewportPx = with(density) { maxWidth.toPx() }
        val todayTarget = {
            ((todayIdx + 0.5f) * dayPx - viewportPx / 2f).toInt().coerceAtLeast(0)
        }
        LaunchedEffect(zoom, lo, viewportPx) {
            hScroll.scrollTo(todayTarget())
        }

        // The month title follows the part of the timeline you are looking at.
        val firstVisible by remember(dayPx, n, lo) {
            derivedStateOf { ((hScroll.value / dayPx).toInt() + 1).coerceIn(0, n - 1) }
        }
        val shown = lo.plusDays(firstVisible.toLong())
        val monthLabel = shown.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) +
            " " + shown.year

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Txt(monthLabel, size = 20.sp, serif = true, maxLines = 1, modifier = Modifier.weight(1f))
                RangePills(zoom, onZoom = { zoom = it })
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
}

@Composable
private fun RangePills(zoom: Int, onZoom: (Int) -> Unit) {
    val p = LocalPalette.current
    val pill = RoundedCornerShape(50)
    Row(
        modifier = Modifier.clip(pill).background(p.surface).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in RANGE_LABELS.indices) {
            val selected = i == zoom
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(34.dp)
                    .clip(pill)
                    .background(if (selected) p.raised else Color.Transparent)
                    .clickable { onZoom(i) },
                contentAlignment = Alignment.Center,
            ) {
                Txt(RANGE_LABELS[i], color = if (selected) p.fg else p.dim, size = 12.sp, bold = selected, maxLines = 1)
            }
        }
    }
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
        // Day header: it stays at the top while the rows scroll.
        Row(Modifier.fillMaxWidth().lineBottom(p.line)) {
            for (i in 0 until n) {
                val d = lo.plusDays(i.toLong())
                val isToday = i == todayIdx
                Column(
                    modifier = Modifier.width(dayW).padding(top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Txt(
                        LETTERS[d.dayOfWeek.value - 1].toString(),
                        color = if (isToday) todayColor else p.mute,
                        size = 10.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) todayColor else Color.Transparent)
                            .padding(horizontal = if (compact) 1.dp else 5.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt(
                            d.dayOfMonth.toString(),
                            color = if (isToday) p.bg else if (isWeekend(d)) p.dim else p.fg,
                            size = if (compact) 9.sp else 12.sp,
                            bold = isToday,
                            maxLines = 1,
                        )
                    }
                }
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
    val shape = RoundedCornerShape(12.dp)
    val barLeft = dayW * startIdx + 1.5.dp
    val barWidth = dayW * len - 3.dp

    Box(Modifier.fillMaxWidth().height(54.dp)) {
        // The bar: a dark rounded block with a colored edge on its left.
        Box(
            modifier = Modifier
                .offset(x = barLeft, y = 8.dp)
                .width(barWidth)
                .height(38.dp)
                .alpha(if (task.done) 0.5f else 1f)
                .clip(shape)
                .background(p.raised)
                .then(if (status == Status.OVERDUE) Modifier.border(1.5.dp, p.red, shape) else Modifier)
                .clickable(onClick = onEdit),
        ) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(accent))
        }

        // Flag, title and time sit on the bar. The title stays in view while you scroll along a long bar.
        Row(
            modifier = Modifier
                .offset {
                    val pad = 14.dp.roundToPx()
                    val start = barLeft.roundToPx() + pad
                    val end = (barLeft + barWidth).roundToPx() - 64.dp.roundToPx()
                    val sticky = scroll.value + 8.dp.roundToPx()
                    IntOffset(sticky.coerceIn(start, maxOf(start, end)), 8.dp.roundToPx())
                }
                .height(38.dp)
                .clickable(onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (task.prio > 0) {
                FlagIcon(prioColor(p, task.prio), iconSize = 15.dp)
                Spacer(Modifier.width(5.dp))
            }
            Txt(
                task.title,
                color = if (task.done) p.dim else p.fg,
                size = 13.sp,
                bold = true,
                strike = task.done,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 190.dp),
            )
            val mins = task.minutes
            if (mins != null) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(p.line)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Txt(minutesLabel(mins), color = p.fg, size = 11.sp, bold = true, maxLines = 1)
                }
            }
        }
    }
}
