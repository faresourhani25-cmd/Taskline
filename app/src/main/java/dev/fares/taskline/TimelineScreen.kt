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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val LETTERS = "MTWTFSS"

@Composable
fun TimelineScreen(state: AppState, onEdit: (String) -> Unit) {
    val p = LocalPalette.current
    val today = state.today
    val dated = remember(state.tasks) {
        state.tasks.filter { it.span() != null }.sortedWith(byStart)
    }
    val noDates = state.tasks.size - dated.size

    if (dated.isEmpty()) {
        EmptyState("no dated tasks", "give a task a start and an end date and it shows up here as a bar.")
        return
    }

    var zoom by rememberSaveable { mutableStateOf(1) }
    val dayW: Dp = when (zoom) {
        0 -> 22.dp
        1 -> 34.dp
        else -> 52.dp
    }
    val range = timelineRange(dated, today)
    val lo = range.first
    val n = daysBetween(lo, range.second) + 1
    val todayIdx = daysBetween(lo, today)
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportPx = with(density) { maxWidth.toPx() }
        val todayTarget = {
            val dayPx = with(density) { dayW.toPx() }
            ((todayIdx + 0.5f) * dayPx - viewportPx / 2f).toInt().coerceAtLeast(0)
        }
        LaunchedEffect(zoom, lo, viewportPx) {
            hScroll.scrollTo(todayTarget())
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Txt("# ${dated.size} scheduled", color = p.dim, size = 12.sp, modifier = Modifier.weight(1f))
                TermButton("−", onClick = { if (zoom > 0) zoom -= 1 }, enabled = zoom > 0)
                TermButton("+", onClick = { if (zoom < 2) zoom += 1 }, enabled = zoom < 2)
                TermButton("today", onClick = { scope.launch { hScroll.animateScrollTo(todayTarget()) } }, color = p.green)
            }

            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(vScroll)) {
                Box(Modifier.horizontalScroll(hScroll)) {
                    TimelineBody(
                        tasks = dated,
                        state = state,
                        lo = lo,
                        n = n,
                        today = today,
                        todayIdx = todayIdx,
                        dayW = dayW,
                        scroll = hScroll,
                        onEdit = onEdit,
                    )
                }
                if (noDates > 0) {
                    val word = if (noDates > 1) "tasks have" else "task has"
                    val pron = if (noDates > 1) "them" else "it"
                    Txt(
                        "# $noDates $word no dates. find $pron in ls.",
                        color = p.dim,
                        size = 12.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
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
    onEdit: (String) -> Unit,
) {
    val p = LocalPalette.current
    val months = remember(lo, n) { monthSegments(lo, n) }
    val weekendShade = p.fg.copy(alpha = 0.06f)
    val todayColor = p.green
    val lineColor = p.line

    Column(Modifier.width(dayW * n)) {
        // Month header
        Row(Modifier.fillMaxWidth().background(p.surface)) {
            for (m in months) {
                val wide = dayW * m.days
                val label = MONTHS[m.month - 1] + (if (wide > 90.dp) " " + m.year else "")
                Box(
                    modifier = Modifier.width(wide).height(26.dp).padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Txt(label, color = p.dim, size = 12.sp, bold = true, maxLines = 1)
                }
            }
        }
        // Day header
        Row(Modifier.fillMaxWidth().background(p.surface).dashedBottom(lineColor)) {
            for (i in 0 until n) {
                val d = lo.plusDays(i.toLong())
                val isToday = i == todayIdx
                val letterColor = if (isToday) p.bg else p.dim
                val numColor = if (isToday) p.bg else if (isWeekend(d)) p.dim else p.fg
                Column(
                    modifier = Modifier
                        .width(dayW)
                        .background(if (isToday) todayColor else Color.Transparent)
                        .padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Txt(LETTERS[d.dayOfWeek.value - 1].toString(), color = letterColor, size = 10.sp)
                    Txt(d.dayOfMonth.toString(), color = numColor, size = 11.sp)
                }
            }
        }
        // Rows, with weekend shading and the today line drawn behind them
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val w = dayW.toPx()
                    for (i in 0 until n) {
                        if (isWeekend(lo.plusDays(i.toLong()))) {
                            drawRect(color = weekendShade, topLeft = Offset(i * w, 0f), size = Size(w, size.height))
                        }
                    }
                    val x = (todayIdx + 0.5f) * w
                    drawLine(
                        color = todayColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                },
        ) {
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
    val color = if (goal != null) p.goal(goal.color) else p.dim
    val overdue = task.status(today) == Status.OVERDUE
    val shape = RoundedCornerShape(2.dp)
    val barWidth = dayW * len - 2.dp

    Box(Modifier.fillMaxWidth().height(58.dp).dashedBottom(p.line)) {
        // The title stays in view while the timeline scrolls sideways.
        Row(
            modifier = Modifier
                .offset { IntOffset(scroll.value + 8.dp.roundToPx(), 0) }
                .widthIn(max = 300.dp)
                .clickable(onClick = onEdit)
                .padding(top = 6.dp),
        ) {
            if (task.prio > 0) {
                Txt(
                    if (task.prio == 2) "!! " else "! ",
                    color = if (task.prio == 2) p.red else p.yellow,
                    bold = true,
                )
            }
            Txt(
                task.title,
                color = if (task.done) p.dim else p.fg,
                strike = task.done,
                maxLines = 1,
                size = 13.sp,
            )
        }
        Box(
            modifier = Modifier
                .offset(x = dayW * startIdx + 1.dp, y = 32.dp)
                .width(barWidth)
                .height(22.dp)
                .alpha(if (task.done) 0.4f else 1f)
                .clip(shape)
                .background(color)
                .then(if (overdue) Modifier.border(2.dp, p.red, shape) else Modifier)
                .clickable(onClick = onEdit)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (barWidth >= 38.dp) {
                Txt("${len}d", color = p.bg, size = 11.sp, maxLines = 1)
            }
        }
    }
}
