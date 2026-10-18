package dev.fares.taskline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private sealed class Entry {
    class Header(val text: String, val count: Int, val tone: Int) : Entry()
    class Item(val task: Task) : Entry()
    class DoneToggle(val count: Int, val open: Boolean) : Entry()
}

@Composable
fun ListScreen(state: AppState, onEdit: (String) -> Unit) {
    val p = LocalPalette.current
    val today = state.today
    var doneOpen by rememberSaveable { mutableStateOf(false) }

    if (state.tasks.isEmpty()) {
        EmptyState("Nothing here yet", "Tap + to add your first task.")
        return
    }

    val groups = remember(state.tasks, today) { groupTasks(state.tasks, today) }
    val entries = ArrayList<Entry>()
    fun section(label: String, tone: Int, list: List<Task>) {
        if (list.isEmpty()) return
        entries.add(Entry.Header(label, list.size, tone))
        for (t in list) entries.add(Entry.Item(t))
    }
    section("Overdue", 1, groups.overdue)
    section("In progress", 2, groups.active)
    section("Upcoming", 3, groups.upcoming)
    section("No dates", 0, groups.anytime)
    if (groups.done.isNotEmpty()) {
        entries.add(Entry.DoneToggle(groups.done.size, doneOpen))
        if (doneOpen) for (t in groups.done) entries.add(Entry.Item(t))
    }
    val openCount = groups.overdue.size + groups.active.size + groups.upcoming.size + groups.anytime.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 110.dp),
    ) {
        if (openCount == 0) {
            item { EmptyState("All clear", "No open tasks. Tap + to add one.") }
        }
        items(entries) { e ->
            when (e) {
                is Entry.Header -> {
                    val label = buildAnnotatedString {
                        append(e.text)
                        withStyle(SpanStyle(color = p.mute)) { append("  ${e.count}") }
                    }
                    TxtRich(
                        label,
                        color = if (e.tone == 1) p.red else p.fg,
                        size = 18.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
                    )
                }
                is Entry.Item -> TaskRow(
                    task = e.task,
                    goal = state.goalById(e.task.goalId),
                    today = today,
                    showGoal = true,
                    onToggle = { state.toggle(e.task.id) },
                    onEdit = { onEdit(e.task.id) },
                )
                is Entry.DoneToggle -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { doneOpen = !doneOpen }
                        .padding(top = 22.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt("Completed", color = p.dim, size = 18.sp)
                    Txt("  ${e.count}", color = p.mute, size = 18.sp, modifier = Modifier.weight(1f))
                    Txt(if (e.open) "Hide" else "Show", color = p.dim, size = 13.sp)
                }
            }
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    goal: Goal?,
    today: LocalDate,
    showGoal: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onCard: Boolean = false,
) {
    val p = LocalPalette.current
    val status = task.status(today)
    val statusColor = when (status) {
        Status.OVERDUE -> p.red
        Status.ACTIVE -> p.orange
        Status.DONE -> p.green
        Status.UPCOMING -> p.dim
        Status.ANYTIME -> p.dim
    }
    val statusText = when (status) {
        Status.OVERDUE -> "Overdue"
        Status.ACTIVE -> "In progress"
        Status.DONE -> "Completed"
        Status.UPCOMING -> "Upcoming"
        Status.ANYTIME -> "No dates"
    }
    val rel = task.relative(today)
    val dur = task.durationDays()
    val relColor = when (rel?.tone) {
        Tone.LATE -> p.red
        Tone.SOON -> p.yellow
        Tone.GO -> p.green
        else -> p.dim
    }
    val meta = buildAnnotatedString {
        withStyle(SpanStyle(color = statusColor)) { append(statusText) }
        if (status != Status.ANYTIME) {
            append("  •  ")
            append(task.datesLabel())
            if (dur != null) {
                append("  •  ")
                append(durationLabel(dur))
            }
        }
        if (rel != null) {
            append("  •  ")
            withStyle(SpanStyle(color = relColor)) { append(rel.text) }
        }
        if (showGoal && goal != null) {
            append("  •  ")
            withStyle(SpanStyle(color = p.goal(goal.color))) { append(goal.title) }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (onCard) p.raised else p.surface)
                .clickable(
                    onClickLabel = if (task.done) "Mark not done" else "Mark done",
                    role = Role.Checkbox,
                    onClick = onToggle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (task.done) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(p.green),
                    contentAlignment = Alignment.Center,
                ) {
                    Txt("✓", color = p.bg, size = 14.sp, bold = true)
                }
            } else {
                Box(Modifier.size(24.dp).border(2.dp, statusColor, CircleShape))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (task.prio > 0) {
                    FlagIcon(prioColor(p, task.prio), iconSize = 18.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Txt(
                    task.title,
                    modifier = Modifier.weight(1f),
                    color = if (task.done) p.dim else p.fg,
                    size = 16.sp,
                    strike = task.done,
                )
            }
            TxtRich(meta, color = p.dim, size = 13.sp, maxLines = 2)
        }
        val mins = task.minutes
        if (mins != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (onCard) p.raised else p.surface)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Txt(minutesLabel(mins), color = p.fg, size = 12.sp, bold = true, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        Txt("›", color = p.mute, size = 24.sp)
    }
}
