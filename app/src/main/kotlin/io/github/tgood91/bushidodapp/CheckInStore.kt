package io.github.tgood91.bushidodapp

import android.content.Context
import java.util.TimeZone

/**
 * Daily dojo check-ins: a streak of consecutive days and a lifetime total, kept on the device.
 *
 * Days are "local epoch days" (days since 1970-01-01 in the phone's time zone) rather than
 * java.time.LocalDate, which needs API 26 and this app supports API 25.
 */
class CheckInStore(context: Context, private val today: () -> Long = ::localEpochDay) {

    private val prefs = context.getSharedPreferences("dojo_checkin", Context.MODE_PRIVATE)

    /** [lastCheckIn] is a local epoch day, or null before the first check-in. */
    data class State(val streak: Int, val total: Int, val lastCheckIn: Long?)

    fun state(): State = displayed(stored(), today())

    fun checkedInToday(): Boolean = state().lastCheckIn == today()

    /** Records today's check-in and returns the new state. A second check-in the same day changes nothing. */
    fun checkIn(): State {
        val before = stored()
        val after = next(before, today())
        if (after != before) {
            prefs.edit()
                .putInt(KEY_STREAK, after.streak)
                .putInt(KEY_TOTAL, after.total)
                .putLong(KEY_LAST, after.lastCheckIn!!)
                .apply()
        }
        return after
    }

    private fun stored() = State(
        streak = prefs.getInt(KEY_STREAK, 0),
        total = prefs.getInt(KEY_TOTAL, 0),
        lastCheckIn = if (prefs.contains(KEY_LAST)) prefs.getLong(KEY_LAST, 0) else null,
    )

    companion object {
        private const val KEY_STREAK = "streak"
        private const val KEY_TOTAL = "total"
        private const val KEY_LAST = "last_day"

        /** Days since 1970-01-01 in [zone], at [millis]. */
        fun localEpochDay(
            millis: Long = System.currentTimeMillis(),
            zone: TimeZone = TimeZone.getDefault(),
        ): Long = Math.floorDiv(millis + zone.getOffset(millis), 86_400_000L)

        /** The streak as it should be shown on [today]: a missed day breaks it. */
        internal fun displayed(s: State, today: Long): State {
            val last = s.lastCheckIn ?: return s
            return if (last >= today - 1) s else s.copy(streak = 0)
        }

        /**
         * The state after checking in on [today]. If the last check-in is today or later
         * (the clock went backwards), nothing changes, so the stored day never moves back.
         */
        internal fun next(s: State, today: Long): State {
            val last = s.lastCheckIn
            if (last != null && last >= today) return s
            val streak = if (last == today - 1) s.streak + 1 else 1
            return State(streak, s.total + 1, today)
        }

        fun rankFor(streak: Int): String = when {
            streak >= 90 -> "Shogun"
            streak >= 75 -> "Bushido Master"
            streak >= 60 -> "Samurai"
            streak >= 1 -> "Tsurano"
            else -> "—"
        }
    }
}
