package com.danzuniga.vitalsbridge

import android.content.Context
import androidx.core.content.edit

/** Local-only cache of the last published snapshot, purely so the home
 *  screen widget can render instantly without its own network/IPC calls —
 *  it just reads whatever the last successful sync (manual or periodic)
 *  left here. */
class WidgetStateStore(context: Context) {

    private val prefs = context.getSharedPreferences("widget_state", Context.MODE_PRIVATE)

    fun save(heartRateBpm: Int?, stepsToday: Long?, locationZone: String?, updatedAt: String) {
        prefs.edit {
            putString(KEY_HEART_RATE, heartRateBpm?.toString())
            putString(KEY_STEPS, stepsToday?.toString())
            putString(KEY_ZONE, locationZone)
            putString(KEY_UPDATED_AT, updatedAt)
        }
    }

    fun load(): State = State(
        heartRate = prefs.getString(KEY_HEART_RATE, null),
        steps = prefs.getString(KEY_STEPS, null),
        zone = prefs.getString(KEY_ZONE, null),
        updatedAt = prefs.getString(KEY_UPDATED_AT, null),
    )

    data class State(val heartRate: String?, val steps: String?, val zone: String?, val updatedAt: String?)

    private companion object {
        const val KEY_HEART_RATE = "heart_rate"
        const val KEY_STEPS = "steps"
        const val KEY_ZONE = "zone"
        const val KEY_UPDATED_AT = "updated_at"
    }
}
