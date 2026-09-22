package com.danzuniga.vitalsbridge

import com.danzuniga.vitalsbridge.LocationZoneRepository.Coordinates
import com.danzuniga.vitalsbridge.LocationZoneRepository.Zone
import com.danzuniga.vitalsbridge.LocationZoneRepository.Companion.classifyZone
import com.danzuniga.vitalsbridge.LocationZoneRepository.Companion.haversineMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationZoneRepositoryTest {

    // Two points in Córdoba, Argentina, roughly 900m apart — used as fixed
    // reference values to sanity-check the Haversine math itself.
    private val plazaSanMartin = Coordinates(-31.4201, -64.1888)
    private val nearby = Coordinates(-31.4150, -64.1888)

    @Test
    fun `haversine distance for a known pair is in the right ballpark`() {
        val meters = haversineMeters(plazaSanMartin, nearby)
        // ~0.0051 degrees of latitude ≈ 567m — assert a wide-ish tolerance so
        // the test isn't fragile to the exact Earth-radius constant used.
        assertTrue("expected ~567m, got $meters", meters in 500.0..650.0)
    }

    @Test
    fun `haversine distance to self is zero`() {
        assertEquals(0.0, haversineMeters(plazaSanMartin, plazaSanMartin), 0.0001)
    }

    @Test
    fun `classifies as afuera when no zones are saved`() {
        assertEquals("afuera", classifyZone(plazaSanMartin, emptyList()))
    }

    @Test
    fun `classifies as afuera when outside every saved radius`() {
        val farAway = Coordinates(plazaSanMartin.lat + 1.0, plazaSanMartin.lng + 1.0)
        val saved = listOf(Zone.CASA to plazaSanMartin)
        assertEquals("afuera", classifyZone(farAway, saved))
    }

    @Test
    fun `classifies inside the radius as the saved zone`() {
        val here = Coordinates(plazaSanMartin.lat + 0.0005, plazaSanMartin.lng)
        val saved = listOf(Zone.CASA to plazaSanMartin)
        assertEquals("casa", classifyZone(here, saved, radiusMeters = 250.0))
    }

    @Test
    fun `escuela wins over an overlapping casa when escuela is listed first`() {
        val saved = listOf(Zone.ESCUELA to plazaSanMartin, Zone.CASA to plazaSanMartin)
        assertEquals("escuela", classifyZone(plazaSanMartin, saved))
    }

    @Test
    fun `a point just past the radius is afuera, not off-by-one inside`() {
        // ~300m north, with a 250m radius: should miss.
        val here = Coordinates(plazaSanMartin.lat + 0.0027, plazaSanMartin.lng)
        val saved = listOf(Zone.CASA to plazaSanMartin)
        assertEquals("afuera", classifyZone(here, saved, radiusMeters = 250.0))
    }
}
