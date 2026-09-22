package com.danzuniga.vitalsbridge

import com.danzuniga.vitalsbridge.GitHubPublisher.Companion.buildVitalsJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GitHubPublisherTest {

    private val now = Instant.parse("2026-09-20T19:58:45Z")

    private fun emptySnapshot() = VitalsSnapshot(
        heartRateBpm = null,
        oxygenSaturationPct = null,
        stepsToday = null,
        floorsClimbedToday = null,
        sleepDurationMinutes = null,
        sleepScore = null,
        lastExercise = null,
    )

    @Test
    fun `missing fields are published as JSON null, not omitted`() {
        val json = buildVitalsJson(emptySnapshot(), nowPlaying = null, locationZone = null, now = now)

        assertTrue(json.has("heart_rate_bpm"))
        assertTrue(json.isNull("heart_rate_bpm"))
        assertTrue(json.isNull("last_exercise"))
        assertTrue(json.isNull("location_zone"))
    }

    @Test
    fun `battery_pct is always null - no Health SDK exposes it`() {
        val json = buildVitalsJson(emptySnapshot(), nowPlaying = null, locationZone = "casa", now = now)
        assertTrue(json.isNull("battery_pct"))
    }

    @Test
    fun `now_playing is omitted entirely when Spotify isn't connected`() {
        val json = buildVitalsJson(emptySnapshot(), nowPlaying = null, locationZone = null, now = now)
        assertFalse(json.has("now_playing"))
    }

    @Test
    fun `now_playing is included when provided`() {
        val nowPlaying = NowPlaying(
            isPlaying = true,
            track = "WAQI",
            artists = "GROSSOMODDO, Montaigne",
            album = "WAQI",
            coverUrl = "https://i.scdn.co/image/x",
            url = "https://open.spotify.com/track/x",
        )

        val json = buildVitalsJson(emptySnapshot(), nowPlaying, locationZone = null, now = now)

        assertTrue(json.has("now_playing"))
        val np = json.getJSONObject("now_playing")
        assertEquals("WAQI", np.getString("track"))
        assertTrue(np.getBoolean("is_playing"))
    }

    @Test
    fun `location_zone publishes only the coarse category, never coordinates`() {
        val json = buildVitalsJson(emptySnapshot(), nowPlaying = null, locationZone = "escuela", now = now)

        assertEquals("escuela", json.getString("location_zone"))
        // Nothing resembling a lat/lng key should ever be present.
        assertFalse(json.keys().asSequence().any { it.contains("lat") || it.contains("lng") })
    }

    @Test
    fun `snapshot values pass through with the documented field names`() {
        val snapshot = VitalsSnapshot(
            heartRateBpm = 80,
            oxygenSaturationPct = 98f,
            stepsToday = 4600,
            floorsClimbedToday = 3f,
            sleepDurationMinutes = 452,
            sleepScore = 64,
            lastExercise = ExerciseSummary("RUNNING", 32, 210.5f, 142f),
        )

        val json = buildVitalsJson(snapshot, nowPlaying = null, locationZone = "casa", now = now)

        assertEquals(80, json.getInt("heart_rate_bpm"))
        assertEquals(4600L, json.getLong("steps_today"))
        assertEquals(452L, json.getLong("sleep_duration_minutes"))
        val exercise = json.getJSONObject("last_exercise")
        assertEquals("RUNNING", exercise.getString("type"))
        assertEquals(32L, exercise.getLong("duration_minutes"))
    }

    @Test
    fun `updated_at reflects the injected clock, not wall time`() {
        val json = buildVitalsJson(emptySnapshot(), nowPlaying = null, locationZone = null, now = now)
        assertEquals(now.toString(), json.getString("updated_at"))
    }
}
