package com.danzuniga.vitalsbridge

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
            views.setTextViewText(R.id.widget_updated, formatUpdatedAt(state.updatedAt))

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

        private fun formatUpdatedAt(iso: String?): String {
            if (iso == null) return "sin datos todavía"
            return runCatching { "sync ${TIME_FORMAT.format(Instant.parse(iso))}" }.getOrDefault("sin datos todavía")
        }
    }
}
