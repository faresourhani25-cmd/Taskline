package dev.fares.taskline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed class Editor {
    class EditTask(val id: String?, val goalId: String?) : Editor()
    class EditGoal(val id: String?) : Editor()
}

private val TAB_NAMES = listOf("1 ls", "2 timeline", "3 goals")
private val TAB_COMMANDS = listOf("ls -l --due", "cal --gantt", "tree goals/")

@Composable
fun TasklineApp(state: AppState) {
    TasklineTheme {
        val p = LocalPalette.current
        var tab by rememberSaveable { mutableStateOf(0) }
        var editor by remember { mutableStateOf<Editor?>(null) }

        BackHandler(enabled = tab != 0 && editor == null) { tab = 0 }

        Column(Modifier.fillMaxSize().background(p.bg)) {
            Header(state, tab)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> ListScreen(state, onEdit = { editor = Editor.EditTask(it, null) })
                    1 -> TimelineScreen(state, onEdit = { editor = Editor.EditTask(it, null) })
                    else -> GoalsScreen(
                        state,
                        onEditGoal = { editor = Editor.EditGoal(it) },
                        onAddTask = { editor = Editor.EditTask(null, it) },
                        onEditTask = { editor = Editor.EditTask(it, null) },
                    )
                }
            }
            BottomBar(
                tab = tab,
                newLabel = if (tab == 2) "+ goal" else "+ task",
                onTab = { tab = it },
                onNew = { editor = if (tab == 2) Editor.EditGoal(null) else Editor.EditTask(null, null) },
            )
        }

        when (val e = editor) {
            is Editor.EditTask -> {
                val existing = state.taskById(e.id)
                TaskDialog(
                    initial = existing,
                    presetGoalId = e.goalId,
                    goals = state.goals,
                    today = state.today,
                    onSave = {
                        state.saveTask(it)
                        editor = null
                    },
                    onDelete = {
                        if (e.id != null) state.deleteTask(e.id)
                        editor = null
                    },
                    onDismiss = { editor = null },
                )
            }
            is Editor.EditGoal -> {
                val existing = state.goalById(e.id)
                GoalDialog(
                    initial = existing,
                    colorCount = state.goals.size,
                    today = state.today,
                    onSave = {
                        state.saveGoal(it)
                        editor = null
                    },
                    onDelete = {
                        if (e.id != null) state.deleteGoal(e.id)
                        editor = null
                    },
                    onDismiss = { editor = null },
                )
            }
            null -> Unit
        }
    }
}

@Composable
private fun Header(state: AppState, tab: Int) {
    val p = LocalPalette.current
    val today = state.today
    val open = state.tasks.count { !it.done }
    val late = state.tasks.count { it.status(today) == Status.OVERDUE }
    val done = state.tasks.count { it.done }

    val prompt = buildAnnotatedString {
        withStyle(SpanStyle(color = p.goal(0))) { append("~/tasks") }
        append(" ")
        withStyle(SpanStyle(color = p.green)) { append("$") }
        append(" " + TAB_COMMANDS[tab.coerceIn(0, 2)])
        withStyle(SpanStyle(color = p.green)) { append(" █") }
    }
    val stats = buildAnnotatedString {
        withStyle(SpanStyle(color = p.green, fontWeight = FontWeight.Bold)) { append("$open") }
        append(" open   ")
        withStyle(SpanStyle(color = p.red, fontWeight = FontWeight.Bold)) { append("$late") }
        append(" overdue   ")
        withStyle(SpanStyle(color = p.fg, fontWeight = FontWeight.Bold)) { append("$done") }
        append(" done")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        TxtRich(prompt, size = 15.sp, maxLines = 1)
        TxtRich(stats, color = p.dim, size = 12.sp, maxLines = 1)
    }
}

@Composable
private fun BottomBar(tab: Int, newLabel: String, onTab: (Int) -> Unit, onNew: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(2.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in TAB_NAMES.indices) {
            val selected = i == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shape)
                    .background(if (selected) p.green else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onTab(i) },
                contentAlignment = Alignment.Center,
            ) {
                Txt(
                    TAB_NAMES[i],
                    color = if (selected) p.bg else p.dim,
                    bold = selected,
                    size = 13.sp,
                    maxLines = 1,
                )
            }
        }
        TermButton(newLabel, onClick = onNew, color = p.green)
    }
}
