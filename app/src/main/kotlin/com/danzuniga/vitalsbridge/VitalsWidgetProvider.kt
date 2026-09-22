package com.danzuniga.vitalsbridge

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Home screen widget: shows the last published snapshot (from
 *  [WidgetStateStore]) and a button that fires an immediate sync without
 *  opening the app. [VitalsSyncWorker] calls [updateAllWidgets] after every
 *  successful publish — manual or periodic — so the widget stays in sync
 *  either way. */
class VitalsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<VitalsSyncWorker>().build())
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.danzuniga.vitalsbridge.WIDGET_REFRESH"
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

        // 3x the 15-minute sync period: past this, a single missed run
        // wouldn't explain it — something's actually stuck (background
        // throttling, revoked permission, no connectivity).
        val STALE_AFTER: Duration = Duration.ofMinutes(45)

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VitalsWidgetProvider::class.java))
            ids.forEach { id -> updateWidget(context, manager, id) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val state = WidgetStateStore(context).load()
            val views = RemoteViews(context.packageName, R.layout.widget_vitals)

            views.setTextViewText(R.id.widget_heart, "♥ ${state.heartRate ?: "--"} bpm")
            views.setTextViewText(R.id.widget_steps, "👟 ${state.steps ?: "--"} pasos")
            views.setTextViewText(R.id.widget_zone, "📍 ${zoneLabel(state.zone)}")

            val stale = isStale(state.updatedAt)
            views.setTextViewText(R.id.widget_updated, formatUpdatedAt(state.updatedAt, stale))
            views.setTextColor(R.id.widget_updated, if (stale) Color.parseColor("#ff2d78") else Color.parseColor("#7a7591"))

            val refreshIntent = Intent(context, VitalsWidgetProvider::class.java).apply { action = ACTION_REFRESH }
            val pendingIntent = PendingIntent.getBroadcast(
                context, id, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, pendingIntent)

            manager.updateAppWidget(id, views)
        }

        private fun zoneLabel(zone: String?) = when (zone) {
            "casa" -> "en casa"
            "escuela" -> "en la escuela"
            "afuera" -> "afuera"
            else -> "?"
        }

        fun formatUpdatedAt(iso: String?, stale: Boolean): String {
            if (iso == null) return "sin datos todavía"
            val time = runCatching { TIME_FORMAT.format(Instant.parse(iso)) }.getOrNull() ?: return "sin datos todavía"
            return if (stale) "⚠ desactualizado — último sync $time" else "sync $time"
        }

        /** True once [iso] is older than [STALE_AFTER], or unparseable/missing
         *  entirely — both mean the widget can't vouch for what it's showing. */
        fun isStale(iso: String?, now: Instant = Instant.now()): Boolean {
            val updatedAt = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return true
            return Duration.between(updatedAt, now) > STALE_AFTER
        }
    }
}
