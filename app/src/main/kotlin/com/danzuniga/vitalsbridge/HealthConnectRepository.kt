package com.danzuniga.vitalsbridge

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectRepository(private val context: Context) {

    val permissions = setOf(HealthPermission.getReadPermission(HeartRateRecord::class))

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermissions(): Boolean {
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /** Latest heart rate sample from the last 30 minutes, or null if there's none. */
    suspend fun latestHeartRateBpm(): Int? {
        val client = HealthConnectClient.getOrCreate(context)
        val since = Instant.now().minus(30, ChronoUnit.MINUTES)

        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.after(since),
            )
        )

        val lastRecord = response.records.maxByOrNull { it.endTime } ?: return null
        val lastSample = lastRecord.samples.maxByOrNull { it.time } ?: return null
        return lastSample.beatsPerMinute.toInt()
    }
}
