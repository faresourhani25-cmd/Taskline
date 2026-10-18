package dev.fares.taskline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
        EmptyState("No goals yet", "A goal groups tasks and tracks progress. Tap + to create one.")
        return
    }

    val hidden = remember { mutableStateMapOf<String, Boolean>() }
    val goals = remember(state.goals) { state.goals.sortedBy { it.created } }
    val shape = RoundedCornerShape(28.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(goals, key = { it.id }) { goal ->
            val progress = goalProgress(goal, state.tasks)
            val mine = state.tasks.filter { it.goalId == goal.id }.sortedWith(byStart)
            val open = hidden[goal.id] != true
            val color = p.goal(goal.color)
            val planned = mine.sumOf { it.minutes ?: 0 }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(p.surface)
                    .padding(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                    Txt(goal.title, size = 20.sp, serif = true, modifier = Modifier.weight(1f))
                    Txt("${progress.percent}%", color = color, bold = true)
                }
                ProgressTrack(progress.percent, color, Modifier.padding(top = 12.dp))
                Txt(
                    "${progress.done}/${progress.total} tasks done" +
                        (if (planned > 0) " · " + minutesLabel(planned) + " planned" else "") +
                        targetLabel(goal, today),
                    color = p.dim,
                    size = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TermButton("Add task", onClick = { onAddTask(goal.id) }, color = p.green)
                    TermButton("Edit", onClick = { onEditGoal(goal.id) })
                    if (mine.isNotEmpty()) {
                        TermButton(
                            if (open) "Hide" else "Show",
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
                            onCard = true,
                        )
                    }
                }
            }
        }
    }
}
