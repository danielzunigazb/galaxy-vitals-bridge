package com.danzuniga.vitalsbridge

import com.danzuniga.vitalsbridge.VitalsWidgetProvider.Companion.formatUpdatedAt
import com.danzuniga.vitalsbridge.VitalsWidgetProvider.Companion.isStale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class VitalsWidgetProviderTest {

    private val now = Instant.parse("2026-09-20T20:00:00Z")

    @Test
    fun `null updated_at is treated as stale`() {
        assertTrue(isStale(null, now))
    }

    @Test
    fun `unparseable updated_at is treated as stale`() {
        assertTrue(isStale("not-a-date", now))
    }

    @Test
    fun `fresh sync within the periodic window is not stale`() {
        val tenMinutesAgo = now.minusSeconds(10 * 60).toString()
        assertFalse(isStale(tenMinutesAgo, now))
    }

    @Test
    fun `a single missed 15-min sync is not yet flagged stale`() {
        val thirtyMinutesAgo = now.minusSeconds(30 * 60).toString()
        assertFalse(isStale(thirtyMinutesAgo, now))
    }

    @Test
    fun `three or more missed syncs is flagged stale`() {
        val fiftyMinutesAgo = now.minusSeconds(50 * 60).toString()
        assertTrue(isStale(fiftyMinutesAgo, now))
    }

    @Test
    fun `formatUpdatedAt shows a warning marker only when stale`() {
        val text = formatUpdatedAt("2026-09-20T19:58:45Z", stale = false)
        val staleText = formatUpdatedAt("2026-09-20T19:58:45Z", stale = true)

        assertFalse(text.contains("⚠"))
        assertTrue(staleText.contains("⚠"))
    }

    @Test
    fun `formatUpdatedAt handles never-synced state`() {
        assertTrue(formatUpdatedAt(null, stale = true).contains("sin datos"))
    }
}
