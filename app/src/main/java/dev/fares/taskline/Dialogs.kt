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
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) color else Color.Transparent)
            .border(1.dp, if (selected) color else p.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Txt(label, color = if (selected) p.bg else p.fg, size = 12.sp, bold = selected, maxLines = 1)
    }
}

@Composable
private fun DateField(label: String, value: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(2.dp)
    val text = parseDate(value)?.let { formatDate(it) + " " + it.year } ?: "none"
    Column(modifier) {
        FieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .border(1.dp, p.mute, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Txt(text, color = if (value == null) p.mute else p.fg)
        }
    }
}

private val fieldStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp)

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
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf("") }
    var armed by remember { mutableStateOf(false) }

    fun save() {
        val t = title.trim()
        val s = start
        val e = end
        if (t.isEmpty()) {
            error = "title is empty"
            return
        }
        if (s != null && e != null && e < s) {
            error = "end is before start"
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
                note = note.trim(),
                done = initial?.done ?: false,
                doneAt = initial?.doneAt,
                created = initial?.created ?: System.currentTimeMillis(),
            ),
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = p.surface,
            contentColor = p.fg,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Txt(if (initial == null) ":new task" else ":edit task", color = p.green, bold = true)

                FieldLabel("title")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = fieldStyle,
                )

                if (goals.isNotEmpty()) {
                    FieldLabel("goal")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Chip("none", goalId == null, p.fg) { goalId = null }
                        for (g in goals) {
                            Chip(g.title, goalId == g.id, p.goal(g.color)) { goalId = g.id }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField("start", start, onClick = {
                        pickDate(context, start, today) { start = it; error = "" }
                    }, modifier = Modifier.weight(1f))
                    DateField("end", end, onClick = {
                        pickDate(context, end ?: start, today) { end = it; error = "" }
                    }, modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt("end:", color = p.dim, size = 12.sp)
                    for ((label, days) in listOf("+1d" to 1L, "+3d" to 3L, "+1w" to 7L, "+1m" to 30L)) {
                        Chip(label, false, p.fg) {
                            val base = parseDate(start) ?: today
                            end = base.plusDays(days).toString()
                            error = ""
                        }
                    }
                    Chip("clear end", false, p.fg) { end = null }
                    Chip("clear start", false, p.fg) { start = null }
                }

                FieldLabel("priority")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val names = listOf("low", "med", "high")
                    for (i in 0..2) {
                        TermButton(
                            label = names[i],
                            onClick = { prio = i },
                            modifier = Modifier.weight(1f),
                            color = if (i == 2) p.red else if (i == 1) p.yellow else p.green,
                            filled = prio == i,
                        )
                    }
                }

                FieldLabel("note")
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                    maxLines = 4,
                    textStyle = fieldStyle,
                )

                Txt(error, color = p.red, size = 12.sp, modifier = Modifier.padding(top = 10.dp).heightIn(min = 18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TermButton(":w save", onClick = { save() }, color = p.green, filled = true)
                    TermButton(":q cancel", onClick = onDismiss)
                    if (initial != null) {
                        Spacer(Modifier.weight(1f))
                        TermButton(
                            if (armed) ":rm sure?" else ":rm delete",
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
    val shape = RoundedCornerShape(2.dp)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = p.surface,
            contentColor = p.fg,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Txt(if (initial == null) ":new goal" else ":edit goal", color = p.green, bold = true)

                FieldLabel("title")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = fieldStyle,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    DateField("target date (optional)", target, onClick = {
                        pickDate(context, target, today) { target = it }
                    }, modifier = Modifier.weight(1f))
                    TermButton("clear", onClick = { target = null })
                }

                FieldLabel("color")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TermButton(
                        ":w save",
                        onClick = {
                            val t = title.trim()
                            if (t.isEmpty()) {
                                error = "title is empty"
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
                    TermButton(":q cancel", onClick = onDismiss)
                    if (initial != null) {
                        Spacer(Modifier.weight(1f))
                        TermButton(
                            if (armed) ":rm sure?" else ":rm delete",
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
