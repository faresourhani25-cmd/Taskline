package dev.fares.taskline

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Random

// Pure Kotlin: no Android classes here, so it can be tested on any JVM.

data class Goal(
    val id: String,
    val title: String,
    val target: String?,
    val color: Int,
    val created: Long,
)

data class Task(
    val id: String,
    val title: String,
    val goalId: String?,
    val start: String?,
    val end: String?,
    val prio: Int,
    val note: String,
    val done: Boolean,
    val doneAt: Long?,
    val created: Long,
)

enum class Status { OVERDUE, ACTIVE, UPCOMING, ANYTIME, DONE }

enum class Tone { NONE, LATE, SOON, GO }

data class Rel(val text: String, val tone: Tone)

data class Span(val start: LocalDate, val end: LocalDate)

data class Groups(
    val overdue: List<Task>,
    val active: List<Task>,
    val upcoming: List<Task>,
    val anytime: List<Task>,
    val done: List<Task>,
)

data class Progress(val done: Int, val total: Int, val percent: Int)

val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

private val random = Random()

fun newId(prefix: String): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    val tail = StringBuilder()
    repeat(4) { tail.append(chars[random.nextInt(chars.length)]) }
    return prefix + System.currentTimeMillis().toString(36) + tail.toString()
}

fun parseDate(s: String?): LocalDate? {
    if (s.isNullOrBlank()) return null
    return try {
        LocalDate.parse(s)
    } catch (e: Exception) {
        null
    }
}

fun formatDate(d: LocalDate): String = MONTHS[d.monthValue - 1] + " " + d.dayOfMonth

/** The first and last day of a task, or null when it has no dates. */
fun Task.span(): Span? {
    val s = parseDate(start)
    val e = parseDate(end)
    val a = s ?: e ?: return null
    val b = e ?: s ?: return null
    return if (a.isAfter(b)) Span(b, a) else Span(a, b)
}

fun Task.status(today: LocalDate): Status {
    if (done) return Status.DONE
    val s = parseDate(start)
    val e = parseDate(end)
    // Only a due date: the task is in progress until that day passes.
    if (s == null && e != null) return if (e.isBefore(today)) Status.OVERDUE else Status.ACTIVE
    // Only a start date: it never becomes overdue.
    if (s != null && e == null) return if (s.isAfter(today)) Status.UPCOMING else Status.ACTIVE
    val sp = span() ?: return Status.ANYTIME
    if (sp.end.isBefore(today)) return Status.OVERDUE
    if (!sp.start.isAfter(today)) return Status.ACTIVE
    return Status.UPCOMING
}

fun Task.relative(today: LocalDate): Rel? {
    if (done) return null
    val s = parseDate(start)
    val e = parseDate(end)
    if (s != null && e == null && !s.isAfter(today)) return null
    if (s == null && e != null && !e.isBefore(today)) {
        val n = ChronoUnit.DAYS.between(today, e)
        val text = if (n == 0L) "ends today" else "ends in ${n}d"
        return Rel(text, if (n <= 1L) Tone.SOON else Tone.GO)
    }
    val sp = span() ?: return null
    if (sp.end.isBefore(today)) {
        val n = ChronoUnit.DAYS.between(sp.end, today)
        return Rel("${n}d late", Tone.LATE)
    }
    if (!sp.start.isAfter(today)) {
        val n = ChronoUnit.DAYS.between(today, sp.end)
        val text = if (n == 0L) "ends today" else "ends in ${n}d"
        return Rel(text, if (n <= 1L) Tone.SOON else Tone.GO)
    }
    val m = ChronoUnit.DAYS.between(today, sp.start)
    return Rel(if (m == 1L) "starts tomorrow" else "starts in ${m}d", Tone.NONE)
}

fun Task.datesLabel(): String {
    val s = parseDate(start)
    val e = parseDate(end)
    return when {
        s != null && e != null -> if (s == e) formatDate(s) else formatDate(s) + " → " + formatDate(e)
        e != null -> "due " + formatDate(e)
        s != null -> "from " + formatDate(s)
        else -> "no dates"
    }
}

private val BIG = LocalDate.of(9999, 1, 1)

private fun endOf(t: Task): LocalDate = t.span()?.end ?: BIG
private fun startOf(t: Task): LocalDate = t.span()?.start ?: BIG

val byEnd: Comparator<Task> = Comparator { a, b ->
    val c1 = endOf(a).compareTo(endOf(b))
    if (c1 != 0) return@Comparator c1
    val c2 = b.prio.compareTo(a.prio)
    if (c2 != 0) return@Comparator c2
    a.title.compareTo(b.title)
}

val byStart: Comparator<Task> = Comparator { a, b ->
    val c1 = startOf(a).compareTo(startOf(b))
    if (c1 != 0) return@Comparator c1
    byEnd.compare(a, b)
}

fun groupTasks(tasks: List<Task>, today: LocalDate): Groups {
    val by = tasks.groupBy { it.status(today) }
    return Groups(
        overdue = (by[Status.OVERDUE] ?: emptyList()).sortedWith(byEnd),
        active = (by[Status.ACTIVE] ?: emptyList()).sortedWith(byEnd),
        upcoming = (by[Status.UPCOMING] ?: emptyList()).sortedWith(byStart),
        anytime = (by[Status.ANYTIME] ?: emptyList()).sortedWith(byEnd),
        done = (by[Status.DONE] ?: emptyList()).sortedByDescending { it.doneAt ?: 0L },
    )
}

fun goalProgress(goal: Goal, tasks: List<Task>): Progress {
    val mine = tasks.filter { it.goalId == goal.id }
    val done = mine.count { it.done }
    val pct = if (mine.isEmpty()) 0 else Math.round(done * 100f / mine.size)
    return Progress(done, mine.size, pct)
}

/** Text progress bar such as [████░░░░░░░░░░]. */
fun progressBar(percent: Int, width: Int = 14): String {
    val filled = Math.round(percent / 100f * width).coerceIn(0, width)
    return "[" + "█".repeat(filled) + "░".repeat(width - filled) + "]"
}

fun targetLabel(goal: Goal, today: LocalDate): String {
    val t = parseDate(goal.target) ?: return ""
    val left = ChronoUnit.DAYS.between(today, t)
    val tail = if (left >= 0) "${left}d left" else "${-left}d past"
    return " · target " + formatDate(t) + " · " + tail
}

/** First and last day shown on the timeline, with room around today and the tasks. */
fun timelineRange(dated: List<Task>, today: LocalDate): Pair<LocalDate, LocalDate> {
    var lo = today
    var hi = today
    for (t in dated) {
        val sp = t.span() ?: continue
        if (sp.start.isBefore(lo)) lo = sp.start
        if (sp.end.isAfter(hi)) hi = sp.end
    }
    lo = lo.minusDays(3)
    hi = hi.plusDays(7)
    if (ChronoUnit.DAYS.between(lo, hi) < 27) hi = lo.plusDays(27)
    return Pair(lo, hi)
}

fun daysBetween(a: LocalDate, b: LocalDate): Int = ChronoUnit.DAYS.between(a, b).toInt()

fun isWeekend(d: LocalDate): Boolean = d.dayOfWeek.value >= 6

data class MonthSeg(val month: Int, val year: Int, val days: Int)

/** Splits n days starting at lo into runs that share a month, for the timeline header. */
fun monthSegments(lo: LocalDate, n: Int): List<MonthSeg> {
    val out = ArrayList<MonthSeg>()
    var i = 0
    while (i < n) {
        val d = lo.plusDays(i.toLong())
        var j = i
        while (j < n && lo.plusDays(j.toLong()).monthValue == d.monthValue) j++
        out.add(MonthSeg(d.monthValue, d.year, j - i))
        i = j
    }
    return out
}

fun slug(s: String): String = s.lowercase().trim().replace(Regex("\\s+"), "-")
