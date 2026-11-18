package ir.taghvimkari.app.data

import android.content.Context

/** Settings of the online backup (kept apart from the people data). */
class BackupPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("taghvim_backup", Context.MODE_PRIVATE)

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(v) = prefs.edit().putString("token", v.trim()).apply()

    var gistId: String
        get() = prefs.getString("gistId", "") ?: ""
        set(v) = prefs.edit().putString("gistId", v.trim()).apply()

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    /** True while there are local changes that are not yet in the online backup. */
    var dirty: Boolean
        get() = prefs.getBoolean("dirty", false)
        set(v) = prefs.edit().putBoolean("dirty", v).apply()

    var lastSuccess: Long
        get() = prefs.getLong("lastSuccess", 0L)
        set(v) = prefs.edit().putLong("lastSuccess", v).apply()
}
