package com.danzuniga.vitalsbridge

import com.danzuniga.vitalsbridge.SamsungHealthRepository.Companion.pickLongestSleep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SamsungHealthRepositoryTest {

    @Test
    fun `returns null when there are no sleep entries`() {
        assertNull(pickLongestSleep(emptyList()))
    }

    @Test
    fun `picks the longer entry over the shorter one, regardless of order`() {
        val nap = SleepSample(durationMinutes = 68, score = null)
        val realSleep = SleepSample(durationMinutes = 452, score = 64)

        assertEquals(realSleep, pickLongestSleep(listOf(nap, realSleep)))
        assertEquals(realSleep, pickLongestSleep(listOf(realSleep, nap)))
    }

    @Test
    fun `an afternoon nap never overwrites a real night's sleep`() {
        // This is the exact bug reported live: a 1h08m nap logged after a
        // full night's sleep must not become the published value.
        val realSleep = SleepSample(durationMinutes = 452, score = 64)
        val nap = SleepSample(durationMinutes = 68, score = null)

        val result = pickLongestSleep(listOf(realSleep, nap))

        assertEquals(452L, result?.durationMinutes)
    }

    @Test
    fun `a single entry is returned as-is`() {
        val onlyEntry = SleepSample(durationMinutes = 300, score = 70)
        assertEquals(onlyEntry, pickLongestSleep(listOf(onlyEntry)))
    }
}
