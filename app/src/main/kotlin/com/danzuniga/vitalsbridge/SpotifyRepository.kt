package com.danzuniga.vitalsbridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class NowPlaying(
    val isPlaying: Boolean,
    val track: String? = null,
    val artists: String? = null,
    val album: String? = null,
    val coverUrl: String? = null,
    val url: String? = null,
)

class SpotifyRepository(private val auth: SpotifyAuth) {
    private val client = OkHttpClient()

    /** Null means "couldn't tell" (not connected, or the call failed) — the
     *  caller should leave now_playing out of the publish rather than claim
     *  silence. A non-null NowPlaying(isPlaying = false) means we did check
     *  and nothing is playing right now. */
    suspend fun currentlyPlaying(): NowPlaying? = withContext(Dispatchers.IO) {
        val token = auth.ensureValidAccessToken() ?: return@withContext null

        val request = Request.Builder()
            .url("https://api.spotify.com/v1/me/player/currently-playing")
            .header("Authorization", "Bearer $token")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.code == 204) return@use NowPlaying(isPlaying = false)
                if (!response.isSuccessful) return@use null
                val text = response.body?.string()
                if (text.isNullOrBlank()) return@use NowPlaying(isPlaying = false)

                val json = JSONObject(text)
                if (!json.optBoolean("is_playing", false)) return@use NowPlaying(isPlaying = false)

                val item = json.optJSONObject("item") ?: return@use NowPlaying(isPlaying = false)
                val artistNames = item.getJSONArray("artists").let { arr ->
                    (0 until arr.length()).joinToString(", ") { arr.getJSONObject(it).getString("name") }
                }
                val album = item.getJSONObject("album")
                val images = album.getJSONArray("images")
                // Spotify lists images largest-first; the middle one (index 1) is
                // a good thumbnail size when present, otherwise take whatever's there.
                val cover = images.optJSONObject(1) ?: images.optJSONObject(0)

                NowPlaying(
                    isPlaying = true,
                    track = item.getString("name"),
                    artists = artistNames,
                    album = album.getString("name"),
                    coverUrl = cover?.getString("url"),
                    url = item.getJSONObject("external_urls").getString("spotify"),
                )
            }
        }.getOrNull()
    }
}
