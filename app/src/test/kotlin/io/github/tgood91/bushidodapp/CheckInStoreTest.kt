package io.github.tgood91.bushidodapp

import io.github.tgood91.bushidodapp.CheckInStore.Companion.displayed
import io.github.tgood91.bushidodapp.CheckInStore.Companion.localEpochDay
import io.github.tgood91.bushidodapp.CheckInStore.Companion.next
import io.github.tgood91.bushidodapp.CheckInStore.Companion.rankFor
import io.github.tgood91.bushidodapp.CheckInStore.State
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInStoreTest {
    private val day = 20_000L

    @Test
    fun `first check-in starts a streak of one`() {
        assertEquals(State(1, 1, day), next(State(0, 0, null), day))
    }

    @Test
    fun `consecutive days extend the streak`() {
        assertEquals(State(6, 11, day), next(State(5, 10, day - 1), day))
    }

    @Test
    fun `a missed day restarts the streak but keeps the total`() {
        assertEquals(State(1, 11, day), next(State(5, 10, day - 2), day))
    }

    @Test
    fun `same day or a clock moved backwards changes nothing`() {
        val s = State(5, 10, day)
        assertEquals(s, next(s, day))
        assertEquals(s, next(s, day - 3))
    }

    @Test
    fun `displayed streak survives until a day is missed`() {
        val s = State(5, 10, day)
        assertEquals(5, displayed(s, day).streak)
        assertEquals(5, displayed(s, day + 1).streak)
        assertEquals(0, displayed(s, day + 2).streak)
        assertEquals(10, displayed(s, day + 2).total)
    }

    @Test
    fun `local epoch day follows the time zone`() {
        val utcMidnight = 19_000L * 86_400_000L
        assertEquals(19_000L, localEpochDay(utcMidnight, TimeZone.getTimeZone("UTC")))
        // One minute before UTC midnight is already the next day in Tokyo, still the previous one in New York.
        assertEquals(19_000L, localEpochDay(utcMidnight - 60_000, TimeZone.getTimeZone("Asia/Tokyo")))
        assertEquals(18_999L, localEpochDay(utcMidnight - 60_000, TimeZone.getTimeZone("America/New_York")))
        // Before 1970 rounds down, not toward zero.
        assertEquals(-1L, localEpochDay(-1L, TimeZone.getTimeZone("UTC")))
    }

    @Test
    fun `ranks by streak`() {
        assertEquals("—", rankFor(0))
        assertEquals("Tsurano", rankFor(1))
        assertEquals("Tsurano", rankFor(59))
        assertEquals("Samurai", rankFor(60))
        assertEquals("Bushido Master", rankFor(75))
        assertEquals("Shogun", rankFor(90))
    }

    @Test
    fun `store checks in through its storage on plain JVM`() {
        var now = day
        val storage = CheckInStore.InMemoryStorage()
        val store = CheckInStore(storage) { now }

        assertFalse(store.checkedInToday())
        assertEquals(State(1, 1, day), store.checkIn())
        assertTrue(store.checkedInToday())
        assertEquals(State(1, 1, day), store.checkIn())

        now = day + 1
        assertEquals(State(2, 2, day + 1), store.checkIn())
        assertEquals(State(2, 2, day + 1), storage.read())

        now = day + 3
        assertEquals(0, store.state().streak)
        assertEquals(State(1, 3, day + 3), store.checkIn())
    }
}
