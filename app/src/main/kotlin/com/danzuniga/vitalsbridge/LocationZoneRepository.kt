package com.danzuniga.vitalsbridge

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Classifies the device's current location into a coarse zone
 *  (casa/escuela/afuera) without ever publishing raw coordinates —
 *  the reference points a user saves with [saveCurrentAs] live only
 *  in this device's local storage; only the resulting zone label
 *  (a plain string) ever leaves the phone. */
class LocationZoneRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("location_zones", Context.MODE_PRIVATE)

    /** Saves the device's current location as the reference point for [zone]. */
    suspend fun saveCurrentAs(zone: Zone): Boolean {
        val location = freshLocation() ?: return false
        prefs.edit {
            putFloat("${zone.key}_lat", location.latitude.toFloat())
            putFloat("${zone.key}_lng", location.longitude.toFloat())
        }
        return true
    }

    fun isConfigured(zone: Zone): Boolean = prefs.contains("${zone.key}_lat")

    /** "escuela", "casa", or "afuera" (default when unconfigured/unavailable/no fix).
     *  Checked in enum order, so a saved ESCUELA point takes priority over an
     *  overlapping CASA one. */
    suspend fun currentZone(): String {
        val here = freshLocation() ?: return "afuera"
        val saved = Zone.entries
            .filter { prefs.contains("${it.key}_lat") }
            .map { zone ->
                zone to Coordinates(
                    prefs.getFloat("${zone.key}_lat", 0f).toDouble(),
                    prefs.getFloat("${zone.key}_lng", 0f).toDouble(),
                )
            }
        return classifyZone(Coordinates(here.latitude, here.longitude), saved)
    }

    /** Requests an actual fresh fix instead of trusting getLastKnownLocation(), which
     *  can hand back whatever's cached from any provider — network or GPS, seconds or
     *  hours old, off by anywhere from tens to hundreds of meters. That's exactly what
     *  caused real "afuera" misclassifications while standing at home. */
    private suspend fun freshLocation(): Location? {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        return suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            runCatching {
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
    }

    enum class Zone(val key: String) { ESCUELA("escuela"), CASA("casa") }

    companion object {
        // GPS/network fixes routinely drift tens to low-hundreds of meters, especially
        // indoors — 150m was tight enough that normal jitter alone flipped "casa" to
        // "afuera" without actually moving.
        private const val RADIUS_METERS = 250.0

        /** Checked in [saved] order, so a saved ESCUELA point takes priority over
         *  an overlapping CASA one. Pure function (no Android Location, no I/O) so
         *  it's unit-testable without an emulator or Robolectric. */
        fun classifyZone(here: Coordinates, saved: List<Pair<Zone, Coordinates>>, radiusMeters: Double = RADIUS_METERS): String {
            for ((zone, point) in saved) {
                if (haversineMeters(here, point) <= radiusMeters) return zone.key
            }
            return "afuera"
        }

        /** Great-circle distance between two points, in meters. Used instead of
         *  android.location.Location#distanceTo so the classification logic has
         *  no dependency on the Android framework (that method is backed by
         *  native code that isn't available under plain JUnit). */
        fun haversineMeters(a: Coordinates, b: Coordinates): Double {
            val earthRadiusMeters = 6_371_000.0
            val dLat = Math.toRadians(b.lat - a.lat)
            val dLng = Math.toRadians(b.lng - a.lng)
            val lat1 = Math.toRadians(a.lat)
            val lat2 = Math.toRadians(b.lat)
            val h = Math.sin(dLat / 2).let { it * it } +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2).let { it * it }
            return 2 * earthRadiusMeters * Math.asin(Math.sqrt(h))
        }
    }

    data class Coordinates(val lat: Double, val lng: Double)
}
