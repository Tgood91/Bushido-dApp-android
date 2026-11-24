package io.github.tgood91.bushidodapp

import java.util.TimeZone

/**
 * Daily dojo check-ins: a streak of consecutive days and a lifetime total.
 * Plain Kotlin/JVM: where the state is kept is up to [storage] (on Android, [SharedPrefsCheckInStorage]).
 *
 * Days are "local epoch days" (days since 1970-01-01 in the device's time zone) rather than
 * java.time.LocalDate, which needs API 26 on Android and this app supports API 25.
 */
class CheckInStore(
    private val storage: Storage,
    private val today: () -> Long = { localEpochDay() },
) {

    /** [lastCheckIn] is a local epoch day, or null before the first check-in. */
    data class State(val streak: Int, val total: Int, val lastCheckIn: Long?)

    /** Where check-in state lives. [read] returns `State(0, 0, null)` when nothing is stored yet. */
    interface Storage {
        fun read(): State
        fun write(state: State)
    }

    /** Keeps state in memory only; for tests and previews. */
    class InMemoryStorage(private var state: State = State(0, 0, null)) : Storage {
        override fun read() = state
        override fun write(state: State) {
            this.state = state
        }
    }

    fun state(): State = displayed(storage.read(), today())

    fun checkedInToday(): Boolean = state().lastCheckIn == today()

    /** Records today's check-in and returns the new state. A second check-in the same day changes nothing. */
    fun checkIn(): State {
        val before = storage.read()
        val after = next(before, today())
        if (after != before) storage.write(after)
        return after
    }

    companion object {
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
