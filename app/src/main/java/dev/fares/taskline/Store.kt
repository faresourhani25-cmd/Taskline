package dev.fares.taskline

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Keeps tasks and goals on the phone, as one JSON text in SharedPreferences. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("taskline", Context.MODE_PRIVATE)

    fun load(): Pair<List<Task>, List<Goal>> {
        val raw = prefs.getString("data", null) ?: return Pair(emptyList(), emptyList())
        return try {
            val root = JSONObject(raw)
            val taskArr = root.optJSONArray("tasks") ?: JSONArray()
            val goalArr = root.optJSONArray("goals") ?: JSONArray()
            val tasks = ArrayList<Task>()
            for (i in 0 until taskArr.length()) tasks.add(taskFrom(taskArr.getJSONObject(i)))
            val goals = ArrayList<Goal>()
            for (i in 0 until goalArr.length()) goals.add(goalFrom(goalArr.getJSONObject(i)))
            Pair(tasks, goals)
        } catch (e: Exception) {
            // Keep the unreadable text so nothing is lost, then start clean.
            prefs.edit().putString("data_unreadable", raw).apply()
            Pair(emptyList(), emptyList())
        }
    }

    fun save(tasks: List<Task>, goals: List<Goal>) {
        val taskArr = JSONArray()
        for (t in tasks) taskArr.put(taskTo(t))
        val goalArr = JSONArray()
        for (g in goals) goalArr.put(goalTo(g))
        val root = JSONObject()
        root.put("tasks", taskArr)
        root.put("goals", goalArr)
        prefs.edit().putString("data", root.toString()).apply()
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (isNull(key)) return null
        val v = optString(key, "")
        return if (v.isBlank()) null else v
    }

    private fun taskFrom(o: JSONObject): Task = Task(
        id = o.getString("id"),
        title = o.optString("title", ""),
        goalId = o.optNullableString("goalId"),
        start = o.optNullableString("start"),
        end = o.optNullableString("end"),
        prio = o.optInt("prio", 0),
        note = o.optString("note", ""),
        done = o.optBoolean("done", false),
        doneAt = if (o.isNull("doneAt")) null else o.optLong("doneAt"),
        created = o.optLong("created", 0L),
        minutes = if (o.isNull("minutes")) null else o.optInt("minutes").takeIf { it > 0 },
    )

    private fun goalFrom(o: JSONObject): Goal = Goal(
        id = o.getString("id"),
        title = o.optString("title", ""),
        target = o.optNullableString("target"),
        color = o.optInt("color", 0),
        created = o.optLong("created", 0L),
    )

    private fun taskTo(t: Task): JSONObject {
        val o = JSONObject()
        o.put("id", t.id)
        o.put("title", t.title)
        if (t.goalId != null) o.put("goalId", t.goalId)
        if (t.start != null) o.put("start", t.start)
        if (t.end != null) o.put("end", t.end)
        o.put("prio", t.prio)
        o.put("note", t.note)
        o.put("done", t.done)
        if (t.doneAt != null) o.put("doneAt", t.doneAt)
        o.put("created", t.created)
        if (t.minutes != null) o.put("minutes", t.minutes)
        return o
    }

    private fun goalTo(g: Goal): JSONObject {
        val o = JSONObject()
        o.put("id", g.id)
        o.put("title", g.title)
        if (g.target != null) o.put("target", g.target)
        o.put("color", g.color)
        o.put("created", g.created)
        return o
    }
}
