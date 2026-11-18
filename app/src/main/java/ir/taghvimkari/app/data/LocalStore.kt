package ir.taghvimkari.app.data

import android.content.Context
import ir.taghvimkari.app.domain.Category
import ir.taghvimkari.app.domain.Employment
import ir.taghvimkari.app.domain.Pattern
import ir.taghvimkari.app.domain.Person
import ir.taghvimkari.app.domain.WorkTypes
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** SharedPreferences-backed storage. The JSON format is unchanged from v1.0, so existing data still loads. */
class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<Person> {
        val array = runCatching { JSONArray(prefs.getString(KEY_PEOPLE, "[]")) }.getOrElse { JSONArray() }
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                val pattern = Pattern.valueOf(o.optString("pattern", Pattern.DAY_NIGHT_REST.name))
                Person(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    workType = WorkTypes.normalize(o.optString("workType", WorkTypes.default)),
                    employment = Employment.valueOf(o.optString("employment", Employment.OFFICIAL.name)),
                    pattern = pattern,
                    startDate = LocalDate.parse(o.getString("startDate")),
                    startWeekday = o.optInt("startWeekday", 0).coerceIn(0, 6),
                    active = o.optBoolean("active", true),
                    isLeader = o.optBoolean("isLeader", false),
                    leaderId = o.optString("leaderId", "").ifBlank { null },
                    // Older saves have no category: fixed office days mean administrative, shifts mean operational.
                    category = runCatching { Category.valueOf(o.getString("category")) }.getOrElse {
                        if (pattern == Pattern.OFFICIAL_DAYS) Category.ADMIN else Category.OPERATIONAL
                    }
                )
            }.getOrNull()
        }
    }

    fun save(people: List<Person>) {
        val array = JSONArray()
        people.forEach { p ->
            array.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("workType", p.workType)
                    put("employment", p.employment.name)
                    put("pattern", p.pattern.name)
                    put("startDate", p.startDate.toString())
                    put("startWeekday", p.startWeekday)
                    put("active", p.active)
                    put("isLeader", p.isLeader)
                    put("leaderId", p.leaderId ?: "")
                    put("category", p.category.name)
                }
            )
        }
        prefs.edit().putString(KEY_PEOPLE, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "taghvim_kari"
        const val KEY_PEOPLE = "people"
    }
}
