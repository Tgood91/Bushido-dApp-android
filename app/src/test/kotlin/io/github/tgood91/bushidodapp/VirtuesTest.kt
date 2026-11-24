package io.github.tgood91.bushidodapp

import org.junit.Assert.assertEquals
import org.junit.Test

class VirtuesTest {
    @Test
    fun `eight virtues, one per day, cycling in order`() {
        assertEquals(8, Virtues.ALL.size)
        val start = 20_000L - Math.floorMod(20_000L, 8L) // a day that maps to the first virtue
        for (i in 0 until 16) {
            assertEquals(Virtues.ALL[i % 8], Virtues.ofTheDay(start + i))
        }
    }

    @Test
    fun `days before 1970 still map to a virtue`() {
        assertEquals(Virtues.ALL[7], Virtues.ofTheDay(-1))
    }
}
