package com.danzuniga.vitalsbridge

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/** Classifies the device's last known location into a coarse zone
 *  (casa/escuela/afuera) without ever publishing raw coordinates —
 *  the reference points a user saves with [saveCurrentAs] live only
 *  in this device's local storage; only the resulting zone label
 *  (a plain string) ever leaves the phone. */
class LocationZoneRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("location_zones", Context.MODE_PRIVATE)

    /** Saves the device's current location as the reference point for [zone]. */
    fun saveCurrentAs(zone: Zone): Boolean {
        val location = lastKnownLocation() ?: return false
        prefs.edit {
            putFloat("${zone.key}_lat", location.latitude.toFloat())
            putFloat("${zone.key}_lng", location.longitude.toFloat())
        }
        return true
    }

    fun isConfigured(zone: Zone): Boolean = prefs.contains("${zone.key}_lat")

    /** "escuela", "casa", or "afuera" (default when unconfigured/unavailable). Checked
     *  in enum order, so a saved ESCUELA point takes priority over an overlapping CASA one. */
    fun currentZone(): String {
        val here = lastKnownLocation() ?: return "afuera"
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

    private fun lastKnownLocation(): Location? {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    enum class Zone(val key: String) { ESCUELA("escuela"), CASA("casa") }

    companion object {
        private const val RADIUS_METERS = 150f
    }
}
