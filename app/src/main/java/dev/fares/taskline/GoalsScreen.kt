package dev.fares.taskline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GoalsScreen(
    state: AppState,
    onEditGoal: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
) {
    val p = LocalPalette.current
    val today = state.today

    if (state.goals.isEmpty()) {
        EmptyState("no goals yet", "a goal groups tasks and tracks progress. press + goal to create one.")
        return
    }

    val hidden = remember { mutableStateMapOf<String, Boolean>() }
    val goals = remember(state.goals) { state.goals.sortedBy { it.created } }
    val shape = RoundedCornerShape(2.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(goals, key = { it.id }) { goal ->
            val progress = goalProgress(goal, state.tasks)
            val mine = state.tasks.filter { it.goalId == goal.id }.sortedWith(byStart)
            val open = hidden[goal.id] != true
            val color = p.goal(goal.color)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(p.surface)
                    .border(1.dp, p.line, shape)
                    .padding(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Txt(goal.title, color = color, bold = true, modifier = Modifier.weight(1f))
                    Txt("${progress.percent}%", bold = true)
                }
                Txt(progressBar(progress.percent), color = color, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                Txt(
                    "${progress.done}/${progress.total} tasks done" + targetLabel(goal, today),
                    color = p.dim,
                    size = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TermButton("+ task", onClick = { onAddTask(goal.id) }, color = p.green)
                    TermButton("edit", onClick = { onEditGoal(goal.id) })
                    if (mine.isNotEmpty()) {
                        TermButton(
                            if (open) "▾ hide" else "▸ show",
                            onClick = { hidden[goal.id] = open },
                        )
                    }
                }
                if (open) {
                    for (t in mine) {
                        TaskRow(
                            task = t,
                            goal = goal,
                            today = today,
                            showGoal = false,
                            onToggle = { state.toggle(t.id) },
                            onEdit = { onEditTask(t.id) },
                        )
                    }
                }
            }
        }
    }
}
