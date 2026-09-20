package com.danzuniga.vitalsbridge

import android.util.Base64
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

    fun publish(heartRateBpm: Int?): Result<Unit> = runCatching {
        val sha = currentSha()

        // JSONObject.put(key, null) removes the key instead of writing JSON null
        // (unlike Python's json.dumps), so nulls need JSONObject.NULL explicitly.
        val vitals = JSONObject()
            .put("heart_rate_bpm", heartRateBpm ?: JSONObject.NULL)
            .put("battery_pct", JSONObject.NULL)
            .put("updated_at", Instant.now().toString())
            .put("source", "galaxy-fit3-healthconnect")

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
            return JSONObject(text).optString("sha", null)
        }
    }

    companion object {
        private const val REPO = "danielzunigazb/portfolio"
        private const val BRANCH = "data"
        private const val API_URL = "https://api.github.com/repos/$REPO/contents/vitals.json"
    }
}
