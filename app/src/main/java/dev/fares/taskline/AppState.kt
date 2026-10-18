package dev.fares.taskline

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/** All app data in one place. Every change is saved to the phone right away. */
class AppState(private val store: Store) {
    private val loaded = store.load()

    var tasks: List<Task> by mutableStateOf(loaded.first)
        private set

    var goals: List<Goal> by mutableStateOf(loaded.second)
        private set

    var today: LocalDate by mutableStateOf(LocalDate.now())
        private set

    fun refreshToday() {
        val now = LocalDate.now()
        if (now != today) today = now
    }

    fun taskById(id: String?): Task? = if (id == null) null else tasks.firstOrNull { it.id == id }

    fun goalById(id: String?): Goal? = if (id == null) null else goals.firstOrNull { it.id == id }

    fun saveTask(task: Task) {
        tasks = if (tasks.any { it.id == task.id }) {
            tasks.map { if (it.id == task.id) task else it }
        } else {
            tasks + task
        }
        persist()
    }

    fun toggle(id: String) {
        val t = taskById(id) ?: return
        saveTask(t.copy(done = !t.done, doneAt = if (t.done) null else System.currentTimeMillis()))
    }

    fun deleteTask(id: String) {
        tasks = tasks.filter { it.id != id }
        persist()
    }

    fun saveGoal(goal: Goal) {
        goals = if (goals.any { it.id == goal.id }) {
            goals.map { if (it.id == goal.id) goal else it }
        } else {
            goals + goal
        }
        persist()
    }

    fun deleteGoal(id: String) {
        tasks = tasks.map { if (it.goalId == id) it.copy(goalId = null) else it }
        goals = goals.filter { it.id != id }
        persist()
    }

    private fun persist() {
        store.save(tasks, goals)
    }
}
