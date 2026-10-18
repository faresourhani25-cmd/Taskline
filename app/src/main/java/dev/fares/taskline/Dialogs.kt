package dev.fares.taskline

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate

private fun pickDate(context: Context, current: String?, today: LocalDate, onPicked: (String) -> Unit) {
    val d = parseDate(current) ?: today
    val listener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        onPicked(LocalDate.of(year, month + 1, day).toString())
    }
    DatePickerDialog(context, listener, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

@Composable
private fun FieldLabel(text: String) {
    Txt(text, color = LocalPalette.current.dim, size = 12.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

@Composable
private fun Chip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) color else p.raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, color = if (selected) p.bg else p.fg, size = 12.sp, bold = selected, maxLines = 1)
    }
}

@Composable
private fun DateField(label: String, value: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    val text = parseDate(value)?.let { formatDate(it) + " " + it.year } ?: "none"
    Column(modifier) {
        FieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(p.raised)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Txt(text, color = if (value == null) p.mute else p.fg)
        }
    }
}

private val fieldStyle = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp)

@Composable
fun TaskDialog(
    initial: Task?,
    presetGoalId: String?,
    goals: List<Goal>,
    today: LocalDate,
    onSave: (Task) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var goalId by remember { mutableStateOf<String?>(initial?.goalId ?: presetGoalId) }
    var start by remember { mutableStateOf<String?>(if (initial == null) today.toString() else initial.start) }
    var end by remember { mutableStateOf<String?>(if (initial == null) today.plusDays(2).toString() else initial.end) }
    var prio by remember { mutableStateOf(initial?.prio ?: 0) }
    var flagsOpen by remember { mutableStateOf(false) }
    var timeText by remember { mutableStateOf(initial?.minutes?.let { minutesLabel(it) } ?: "") }
    var error by remember { mutableStateOf("") }
    var armed by remember { mutableStateOf(false) }

    fun save() {
        val t = title.trim()
        val s = start
        val e = end
        if (t.isEmpty()) {
            error = "Title is empty"
            return
        }
        if (s != null && e != null && e < s) {
            error = "End is before start"
            return
        }
        val mins = if (timeText.isBlank()) null else parseMinutes(timeText)
        if (timeText.isNotBlank() && mins == null) {
            error = "Time needed: try 4h, 90m or 1h 30m"
            return
        }
        onSave(
            Task(
                id = initial?.id ?: newId("t"),
                title = t,
                goalId = goalId,
                start = s,
                end = e,
                prio = prio,
                note = initial?.note ?: "",
                done = initial?.done ?: false,
                doneAt = initial?.doneAt,
                created = initial?.created ?: System.currentTimeMillis(),
                minutes = mins,
            ),
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = p.surface,
            contentColor = p.fg,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Txt(
                        if (initial == null) "New task" else "Edit task",
                        size = 24.sp,
                        serif = true,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (flagsOpen) p.line else p.raised)
                            .clickable(onClickLabel = "Choose a priority flag") { flagsOpen = !flagsOpen },
                        contentAlignment = Alignment.Center,
                    ) {
                        FlagIcon(prioColor(p, prio), filled = prio > 0, iconSize = 22.dp)
                    }
                }
                if (flagsOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (level in 0..3) {
                            val c = prioColor(p, level)
                            val selected = prio == level
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (selected) c.copy(alpha = 0.22f) else p.raised)
                                    .clickable { prio = level; flagsOpen = false }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FlagIcon(c, filled = level > 0, iconSize = 28.dp)
                                Txt(
                                    prioName(level),
                                    color = if (selected) p.fg else p.dim,
                                    size = 12.sp,
                                    bold = selected,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                FieldLabel("Title")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = fieldStyle,
                )

                if (goals.isNotEmpty()) {
                    FieldLabel("Goal")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Chip("None", goalId == null, p.fg) { goalId = null }
                        for (g in goals) {
                            Chip(g.title, goalId == g.id, p.goal(g.color)) { goalId = g.id }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField("Start", start, onClick = {
                        pickDate(context, start, today) { start = it; error = "" }
                    }, modifier = Modifier.weight(1f))
                    DateField("End", end, onClick = {
                        pickDate(context, end ?: start, today) { end = it; error = "" }
                    }, modifier = Modifier.weight(1f))
                }

                val startDate = parseDate(start)
                val endDate = parseDate(end)
                if (startDate != null && endDate != null && !endDate.isBefore(startDate)) {
                    Txt(
                        "Duration: " + durationLabel(daysBetween(startDate, endDate) + 1),
                        color = p.orange,
                        size = 13.sp,
                        bold = true,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt("End:", color = p.dim, size = 12.sp)
                    for ((label, days) in listOf("+1d" to 1L, "+3d" to 3L, "+1w" to 7L, "+1m" to 30L)) {
                        Chip(label, false, p.fg) {
                            val base = parseDate(start) ?: today
                            end = base.plusDays(days).toString()
                            error = ""
                        }
                    }
                    Chip("Clear end", false, p.fg) { end = null }
                    Chip("Clear start", false, p.fg) { start = null }
                }

                FieldLabel("Time needed")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Chip("None", timeText.isBlank(), p.fg) { timeText = ""; error = "" }
                    for ((label, m) in listOf("30m" to 30, "1h" to 60, "2h" to 120, "4h" to 240, "8h" to 480)) {
                        Chip(label, parseMinutes(timeText) == m, p.orange) { timeText = label; error = "" }
                    }
                }
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it; error = "" },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = fieldStyle,
                    placeholder = { Txt("or type it: 3h, 90m, 1h 30m", color = p.mute, size = 14.sp) },
                )

                Txt(error, color = p.red, size = 12.sp, modifier = Modifier.padding(top = 10.dp).heightIn(min = 18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TermButton("Save", onClick = { save() }, modifier = Modifier.width(120.dp), color = p.green, filled = true)
                    TermButton("Cancel", onClick = onDismiss, modifier = Modifier.width(120.dp))
                }
                if (initial != null) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                        TermButton(
                            if (armed) "Sure?" else "Delete",
                            onClick = { if (armed) onDelete() else armed = true },
                            color = p.red,
                            filled = armed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalDialog(
    initial: Goal?,
    colorCount: Int,
    today: LocalDate,
    onSave: (Goal) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var target by remember { mutableStateOf<String?>(initial?.target) }
    var color by remember { mutableStateOf(initial?.color ?: (colorCount % 6)) }
    var error by remember { mutableStateOf("") }
    var armed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = p.surface,
            contentColor = p.fg,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Txt(if (initial == null) "New goal" else "Edit goal", size = 24.sp, serif = true)

                FieldLabel("Title")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = fieldStyle,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    DateField("Target date (optional)", target, onClick = {
                        pickDate(context, target, today) { target = it }
                    }, modifier = Modifier.weight(1f))
                    TermButton("Clear", onClick = { target = null })
                }

                FieldLabel("Color")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..5) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(shape)
                                .background(p.goal(i))
                                .then(if (color == i) Modifier.border(3.dp, p.fg, shape) else Modifier)
                                .clickable { color = i },
                        )
                    }
                }

                Txt(error, color = p.red, size = 12.sp, modifier = Modifier.padding(top = 10.dp).heightIn(min = 18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TermButton(
                        "Save",
                        modifier = Modifier.width(120.dp),
                        onClick = {
                            val t = title.trim()
                            if (t.isEmpty()) {
                                error = "Title is empty"
                            } else {
                                onSave(
                                    Goal(
                                        id = initial?.id ?: newId("g"),
                                        title = t,
                                        target = target,
                                        color = color,
                                        created = initial?.created ?: System.currentTimeMillis(),
                                    ),
                                )
                            }
                        },
                        color = p.green,
                        filled = true,
                    )
                    TermButton("Cancel", onClick = onDismiss, modifier = Modifier.width(120.dp))
                }
                if (initial != null) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                        TermButton(
                            if (armed) "Sure?" else "Delete",
                            onClick = { if (armed) onDelete() else armed = true },
                            color = p.red,
                            filled = armed,
                        )
                    }
                }
            }
        }
    }
}
