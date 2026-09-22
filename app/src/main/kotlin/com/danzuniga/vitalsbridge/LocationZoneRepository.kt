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
        for (zone in Zone.entries) {
            if (!prefs.contains("${zone.key}_lat")) continue
            val saved = Location("saved").apply {
                latitude = prefs.getFloat("${zone.key}_lat", 0f).toDouble()
                longitude = prefs.getFloat("${zone.key}_lng", 0f).toDouble()
            }
            if (here.distanceTo(saved) <= RADIUS_METERS) return zone.key
        }
        return "afuera"
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
        private const val RADIUS_METERS = 250f
    }
}
