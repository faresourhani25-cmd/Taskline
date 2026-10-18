package dev.fares.taskline

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private sealed class Entry {
    class Header(val text: String, val tone: Int) : Entry()
    class Item(val task: Task) : Entry()
    class DoneToggle(val count: Int, val open: Boolean) : Entry()
}

@Composable
fun ListScreen(state: AppState, onEdit: (String) -> Unit) {
    val p = LocalPalette.current
    val today = state.today
    var doneOpen by rememberSaveable { mutableStateOf(false) }

    if (state.tasks.isEmpty()) {
        EmptyState("nothing here yet", "press + task to add the first one.")
        return
    }

    val groups = remember(state.tasks, today) { groupTasks(state.tasks, today) }
    val entries = ArrayList<Entry>()
    fun section(label: String, tone: Int, list: List<Task>) {
        if (list.isEmpty()) return
        entries.add(Entry.Header("# $label (${list.size})", tone))
        for (t in list) entries.add(Entry.Item(t))
    }
    section("overdue", 1, groups.overdue)
    section("in progress", 2, groups.active)
    section("upcoming", 3, groups.upcoming)
    section("no dates", 0, groups.anytime)
    if (groups.done.isNotEmpty()) {
        entries.add(Entry.DoneToggle(groups.done.size, doneOpen))
        if (doneOpen) for (t in groups.done) entries.add(Entry.Item(t))
    }
    val openCount = groups.overdue.size + groups.active.size + groups.upcoming.size + groups.anytime.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        if (openCount == 0) {
            item { EmptyState("all clear", "no open tasks. add one with + task.") }
        }
        items(entries) { e ->
            when (e) {
                is Entry.Header -> {
                    val c = when (e.tone) {
                        1 -> p.red
                        2 -> p.green
                        3 -> p.goal(0)
                        else -> p.dim
                    }
                    Txt(e.text, color = c, size = 12.sp, bold = true, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                }
                is Entry.Item -> TaskRow(
                    task = e.task,
                    goal = state.goalById(e.task.goalId),
                    today = today,
                    showGoal = true,
                    onToggle = { state.toggle(e.task.id) },
                    onEdit = { onEdit(e.task.id) },
                )
                is Entry.DoneToggle -> Txt(
                    "# done (${e.count}) " + (if (e.open) "▾" else "▸"),
                    color = p.dim,
                    size = 12.sp,
                    bold = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { doneOpen = !doneOpen }
                        .padding(top = 20.dp, bottom = 12.dp),
                )
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
) {
    val p = LocalPalette.current
    val rel = task.relative(today)
    val relColor = when (rel?.tone) {
        Tone.LATE -> p.red
        Tone.SOON -> p.yellow
        Tone.GO -> p.green
        else -> p.dim
    }
    val meta = buildAnnotatedString {
        append(task.datesLabel())
        if (rel != null) {
            append("  ")
            withStyle(SpanStyle(color = relColor)) { append(rel.text) }
        }
        if (showGoal && goal != null) {
            append("  ")
            withStyle(SpanStyle(color = p.goal(goal.color))) { append("#" + slug(goal.title)) }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().dashedBottom(p.line),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    onClickLabel = if (task.done) "Mark not done" else "Mark done",
                    role = Role.Checkbox,
                    onClick = onToggle,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Txt(if (task.done) "[x]" else "[ ]", color = if (task.done) p.green else p.dim)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row {
                if (task.prio > 0) {
                    Txt(
                        if (task.prio == 2) "!!" else "!",
                        color = if (task.prio == 2) p.red else p.yellow,
                        bold = true,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Txt(
                    task.title,
                    modifier = Modifier.weight(1f),
                    color = if (task.done) p.dim else p.fg,
                    strike = task.done,
                )
            }
            TxtRich(meta, color = p.dim, size = 12.sp)
            if (task.note.isNotBlank()) {
                Txt(task.note, color = p.dim, size = 12.sp)
            }
        }
    }
}
