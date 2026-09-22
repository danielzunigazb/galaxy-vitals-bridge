package com.danzuniga.vitalsbridge

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Publishes vitals to danzuniga.xyz/status via GitHub's Contents API, same
 *  endpoint and schema the Linux BLE sync script (sync_vitals.py) writes to. */
class GitHubPublisher(private val token: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun publish(
        snapshot: VitalsSnapshot,
        nowPlaying: NowPlaying? = null,
        locationZone: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sha = currentSha()
            val vitals = buildVitalsJson(snapshot, nowPlaying, locationZone, Instant.now())

            val body = JSONObject()
                .put("message", "vitals @ ${vitals.getString("updated_at")}")
                .put("content", Base64.encodeToString(vitals.toString(2).toByteArray(), Base64.NO_WRAP))
                .put("branch", BRANCH)
            if (sha != null) body.put("sha", sha)

            val request = Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .put(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "PUT ${response.code}: ${response.body?.string()}" }
            }
        }
    }

    private fun currentSha(): String? {
        val request = Request.Builder()
            .url("$API_URL?ref=$BRANCH")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            val json = JSONObject(text)
            return if (json.has("sha")) json.getString("sha") else null
        }
    }

    companion object {
        private const val REPO = "danielzunigazb/portfolio"
        private const val BRANCH = "data"
        private const val API_URL = "https://api.github.com/repos/$REPO/contents/vitals.json"

        /** Pure JSON-shaping, split out from [publish] so the schema (which
         *  fields are present, when nulls vs. omission happen) can be unit
         *  tested without a network call or an Android runtime. */
        fun buildVitalsJson(
            snapshot: VitalsSnapshot,
            nowPlaying: NowPlaying?,
            locationZone: String?,
            now: Instant,
        ): JSONObject {
            // JSONObject.put(key, null) removes the key instead of writing JSON null
            // (unlike Python's json.dumps), so nulls need JSONObject.NULL explicitly.
            val vitals = JSONObject()
                .put("heart_rate_bpm", snapshot.heartRateBpm ?: JSONObject.NULL)
                .put("oxygen_saturation_pct", snapshot.oxygenSaturationPct ?: JSONObject.NULL)
                .put("steps_today", snapshot.stepsToday ?: JSONObject.NULL)
                .put("floors_climbed_today", snapshot.floorsClimbedToday ?: JSONObject.NULL)
                .put("sleep_duration_minutes", snapshot.sleepDurationMinutes ?: JSONObject.NULL)
                .put("sleep_score", snapshot.sleepScore ?: JSONObject.NULL)
                .put("last_exercise", snapshot.lastExercise?.let {
                    JSONObject()
                        .put("type", it.type)
                        .put("duration_minutes", it.durationMinutes)
                        .put("calories", it.calories ?: JSONObject.NULL)
                        .put("mean_heart_rate_bpm", it.meanHeartRateBpm ?: JSONObject.NULL)
                } ?: JSONObject.NULL)
                .put("battery_pct", JSONObject.NULL)
                // Coarse category only ("casa" / "escuela" / "afuera") — never raw
                // coordinates. The reference points never leave the device.
                .put("location_zone", locationZone ?: JSONObject.NULL)
                .put("updated_at", now.toString())
                .put("source", "galaxy-fit3-samsunghealth")

            // Only set when we actually have a Spotify connection to ask — if
            // it's not connected yet this key is simply left out, same as any
            // other not-yet-available field.
            if (nowPlaying != null) {
                vitals.put("now_playing", JSONObject()
                    .put("is_playing", nowPlaying.isPlaying)
                    .put("track", nowPlaying.track ?: JSONObject.NULL)
                    .put("artists", nowPlaying.artists ?: JSONObject.NULL)
                    .put("album", nowPlaying.album ?: JSONObject.NULL)
                    .put("cover_url", nowPlaying.coverUrl ?: JSONObject.NULL)
                    .put("url", nowPlaying.url ?: JSONObject.NULL)
                    .put("updated_at", now.toString())
                )
            }

            return vitals
        }
    }
}
