package com.danzuniga.vitalsbridge

import android.app.Activity
import android.content.Context
import com.samsung.android.sdk.health.data.HealthDataService
import com.samsung.android.sdk.health.data.HealthDataStore
import com.samsung.android.sdk.health.data.data.Field
import com.samsung.android.sdk.health.data.data.HealthDataPoint
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import com.samsung.android.sdk.health.data.permission.AccessType
import com.samsung.android.sdk.health.data.permission.Permission
import com.samsung.android.sdk.health.data.request.DataType
import com.samsung.android.sdk.health.data.request.DataTypes
import com.samsung.android.sdk.health.data.request.InstantTimeFilter
import com.samsung.android.sdk.health.data.request.LocalTimeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ExerciseSummary(
    val type: String,
    val durationMinutes: Long,
    val calories: Float?,
    val meanHeartRateBpm: Float?,
)

data class VitalsSnapshot(
    val heartRateBpm: Int?,
    val oxygenSaturationPct: Float?,
    val stepsToday: Long?,
    val floorsClimbedToday: Float?,
    val sleepDurationMinutes: Long?,
    val sleepScore: Int?,
    val lastExercise: ExerciseSummary?,
)

/** Reads wellness data straight from the Samsung Health app via the Samsung
 *  Health Data SDK, instead of Health Connect: on this device Samsung Health
 *  never actually syncs data into Health Connect even with write permission
 *  granted, so Health Connect always reported zero records. */
class SamsungHealthRepository(context: Context) {

    private val store: HealthDataStore = HealthDataService.getStore(context)

    private val permissions = setOf(
        Permission.of(DataTypes.HEART_RATE, AccessType.READ),
        Permission.of(DataTypes.BLOOD_OXYGEN, AccessType.READ),
        Permission.of(DataTypes.STEPS, AccessType.READ),
        Permission.of(DataTypes.FLOORS_CLIMBED, AccessType.READ),
        Permission.of(DataTypes.SLEEP, AccessType.READ),
        Permission.of(DataTypes.EXERCISE, AccessType.READ),
    )

    suspend fun hasPermission(): Boolean =
        store.getGrantedPermissions(permissions).containsAll(permissions)

    /** Shows Samsung Health's consent screen. Needs the foreground Activity. */
    suspend fun requestPermission(activity: Activity): Boolean {
        return try {
            store.requestPermissions(permissions, activity).containsAll(permissions)
        } catch (e: ResolvablePlatformException) {
            e.resolve(activity)
            false
        }
    }

    suspend fun readVitalsSnapshot(): VitalsSnapshot {
        val todayStart = LocalDate.now().atStartOfDay()
        val sleep = latestSleep()

        return VitalsSnapshot(
            heartRateBpm = latestHeartRateBpm(),
            oxygenSaturationPct = latestOxygenSaturationPct(),
            stepsToday = totalStepsSince(todayStart),
            floorsClimbedToday = totalFloorsClimbedSince(todayStart.atZone(ZoneId.systemDefault()).toInstant()),
            sleepDurationMinutes = sleep?.first,
            sleepScore = sleep?.second,
            lastExercise = lastExercise(),
        )
    }

    private suspend fun latestHeartRateBpm(): Int? {
        val since = Instant.now().minus(30, ChronoUnit.MINUTES)
        val request = DataTypes.HEART_RATE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.since(since))
            .build()

        val samples = store.readData(request).dataList
            .flatMap { it.getValueOrDefault(DataType.HeartRateType.SERIES_DATA, emptyList()) }

        return samples.maxByOrNull { it.startTime }?.heartRate?.toInt()
    }

    private suspend fun latestOxygenSaturationPct(): Float? {
        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        val request = DataTypes.BLOOD_OXYGEN.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.since(since))
            .build()

        val samples = store.readData(request).dataList
            .flatMap { it.getValueOrDefault(DataType.BloodOxygenType.SERIES_DATA, emptyList()) }

        return samples.maxByOrNull { it.startTime }?.oxygenSaturation
    }

    /** Sleep sessions span the previous night, so look back 48h to always catch the last one. */
    private suspend fun latestSleep(): Pair<Long, Int?>? {
        val since = Instant.now().minus(48, ChronoUnit.HOURS)
        val request = DataTypes.SLEEP.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.since(since))
            .build()

        val point = store.readData(request).dataList.maxByOrNull { it.endTime ?: Instant.MIN } ?: return null
        val duration = point.valueOrNull(DataType.SleepType.DURATION) ?: return null
        val score = point.valueOrNull(DataType.SleepType.SLEEP_SCORE)
        return duration.toMinutes() to score
    }

    private suspend fun totalStepsSince(since: LocalDateTime): Long {
        val request = DataType.StepsType.TOTAL.requestBuilder
            .setLocalTimeFilter(LocalTimeFilter.of(since, LocalDateTime.now()))
            .build()

        return store.aggregateData(request).dataList.sumOf { it.getValueOrDefault(0L) }
    }

    private suspend fun totalFloorsClimbedSince(since: Instant): Float {
        val request = DataTypes.FLOORS_CLIMBED.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.since(since))
            .build()

        return store.readData(request).dataList
            .sumOf { (it.valueOrNull(DataType.FloorsClimbedType.FLOOR) ?: 0f).toDouble() }
            .toFloat()
    }

    /** Last logged workout in the past week, or null if none. */
    private suspend fun lastExercise(): ExerciseSummary? {
        val since = Instant.now().minus(7, ChronoUnit.DAYS)
        val request = DataTypes.EXERCISE.readDataRequestBuilder
            .setInstantTimeFilter(InstantTimeFilter.since(since))
            .build()

        val point = store.readData(request).dataList.maxByOrNull { it.endTime ?: Instant.MIN } ?: return null
        val session = point.getValueOrDefault(DataType.ExerciseType.SESSIONS, emptyList())
            .maxByOrNull { it.endTime } ?: return null
        val exerciseType = point.valueOrNull(DataType.ExerciseType.EXERCISE_TYPE)

        return ExerciseSummary(
            type = exerciseType?.name ?: "OTHER",
            durationMinutes = session.duration.toMinutes(),
            calories = session.calories,
            meanHeartRateBpm = session.meanHeartRate,
        )
    }

    private fun <T> HealthDataPoint.valueOrNull(field: Field<T>): T? =
        runCatching { getValue(field) }.getOrNull()
}
