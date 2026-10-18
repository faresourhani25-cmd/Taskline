package dev.fares.taskline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
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

private val TAB_NAMES = listOf("List", "Timeline", "Goals")
private val TAB_TITLES = listOf("Tasks", "Timeline", "Goals")

@Composable
fun TasklineApp(state: AppState) {
    TasklineTheme {
        val p = LocalPalette.current
        var tab by rememberSaveable { mutableStateOf(0) }
        var editor by remember { mutableStateOf<Editor?>(null) }

        BackHandler(enabled = tab != 0 && editor == null) { tab = 0 }

        Box(Modifier.fillMaxSize().background(p.bg)) {
            Column(Modifier.fillMaxSize()) {
                Header(state, tab, onTab = { tab = it })
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
            }
            AddButton(
                onClick = { editor = if (tab == 2) Editor.EditGoal(null) else Editor.EditTask(null, null) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
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
private fun Header(state: AppState, tab: Int, onTab: (Int) -> Unit) {
    val p = LocalPalette.current
    val today = state.today
    val open = state.tasks.count { !it.done }
    val late = state.tasks.count { it.status(today) == Status.OVERDUE }
    val done = state.tasks.count { it.done }

    val stats = buildAnnotatedString {
        withStyle(SpanStyle(color = if (late > 0) p.red else p.dim, fontWeight = FontWeight.SemiBold)) {
            append("$late overdue")
        }
        append("  ·  ")
        withStyle(SpanStyle(color = p.green, fontWeight = FontWeight.SemiBold)) {
            append("$done done")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(
                TAB_TITLES[tab.coerceIn(0, 2)],
                size = 34.sp,
                serif = true,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            OpenPill(open)
        }
        TxtRich(stats, color = p.dim, size = 13.sp, maxLines = 1)
        Spacer(Modifier.height(14.dp))
        Segmented(tab, onTab)
    }
}

@Composable
private fun OpenPill(open: Int) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(p.surface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(p.orange))
        Spacer(Modifier.width(8.dp))
        Txt("$open", color = p.orange, bold = true)
        Spacer(Modifier.width(5.dp))
        Txt("open", color = p.dim, size = 13.sp)
    }
}

@Composable
private fun Segmented(tab: Int, onTab: (Int) -> Unit) {
    val p = LocalPalette.current
    val pill = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(pill)
            .background(p.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in TAB_NAMES.indices) {
            val selected = i == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(pill)
                    .background(if (selected) p.raised else Color.Transparent)
                    .clickable { onTab(i) },
                contentAlignment = Alignment.Center,
            ) {
                Txt(
                    TAB_NAMES[i],
                    color = if (selected) p.fg else p.dim,
                    bold = selected,
                    size = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The white pill with a plus, floating at the bottom of the screen. */
@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .width(104.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(p.fg)
            .clickable(onClickLabel = "Add", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt("+", color = p.bg, size = 30.sp)
    }
}
