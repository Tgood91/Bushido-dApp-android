package io.github.tgood91.bushidodapp

import android.content.Context

/** The Android side of [CheckInStore]: keeps check-in state in SharedPreferences. */
class SharedPrefsCheckInStorage(context: Context) : CheckInStore.Storage {

    private val prefs = context.getSharedPreferences("dojo_checkin", Context.MODE_PRIVATE)

    override fun read() = CheckInStore.State(
        streak = prefs.getInt(KEY_STREAK, 0),
        total = prefs.getInt(KEY_TOTAL, 0),
        lastCheckIn = if (prefs.contains(KEY_LAST)) prefs.getLong(KEY_LAST, 0) else null,
    )

    override fun write(state: CheckInStore.State) {
        val editor = prefs.edit()
            .putInt(KEY_STREAK, state.streak)
            .putInt(KEY_TOTAL, state.total)
        val last = state.lastCheckIn
        if (last != null) editor.putLong(KEY_LAST, last) else editor.remove(KEY_LAST)
        editor.apply()
    }

    private companion object {
        const val KEY_STREAK = "streak"
        const val KEY_TOTAL = "total"
        const val KEY_LAST = "last_day"
    }
}
