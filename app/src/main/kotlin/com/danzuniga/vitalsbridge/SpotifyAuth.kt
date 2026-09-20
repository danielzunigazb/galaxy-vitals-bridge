package com.danzuniga.vitalsbridge

import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/** Spotify's client-side OAuth (Authorization Code + PKCE) — no client secret
 *  needed, safe to ship inside the app. Set SpotifyConfig.CLIENT_ID to your
 *  own app's Client ID from developer.spotify.com/dashboard. */
object SpotifyConfig {
    const val CLIENT_ID = "3c28af16a4954660b057232bfef7ea27"
    const val REDIRECT_URI = "galaxyvitalsbridge://callback"
    const val SCOPE = "user-read-currently-playing"
}

class SpotifyAuth(private val tokenStore: TokenStore) {
    private val client = OkHttpClient()

    /** Returns the authorize URL to open in a browser, and the PKCE verifier
     *  to keep around (stashed in TokenStore) until the redirect comes back. */
    fun buildAuthUrl(): String {
        val verifier = generateVerifier()
        tokenStore.spotifyPendingVerifier = verifier
        val challenge = challengeFor(verifier)

        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", SpotifyConfig.CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", SpotifyConfig.REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("scope", SpotifyConfig.SCOPE)
            .build()
            .toString()
    }

    /** Call with the `code` query param from the redirect intent. */
    suspend fun exchangeCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val verifier = tokenStore.spotifyPendingVerifier
                ?: error("no pending PKCE verifier — auth flow wasn't started from this app")

            val body = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", SpotifyConfig.REDIRECT_URI)
                .add("client_id", SpotifyConfig.CLIENT_ID)
                .add("code_verifier", verifier)
                .build()
            val request = Request.Builder().url(TOKEN_URL).post(body).build()

            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "token exchange ${response.code}: ${response.body?.string()}" }
                val json = JSONObject(response.body!!.string())
                tokenStore.spotifyAccessToken = json.getString("access_token")
                tokenStore.spotifyRefreshToken = json.getString("refresh_token")
                tokenStore.spotifyExpiresAt = System.currentTimeMillis() + json.getInt("expires_in") * 1000L
            }
            tokenStore.spotifyPendingVerifier = null
        }
    }

    /** A usable access token, refreshing it first if it's expired (or about
     *  to). Null if Spotify was never connected. */
    suspend fun ensureValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val refresh = tokenStore.spotifyRefreshToken ?: return@withContext null
        if (tokenStore.spotifyExpiresAt > System.currentTimeMillis() + 30_000) {
            return@withContext tokenStore.spotifyAccessToken
        }

        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refresh)
            .add("client_id", SpotifyConfig.CLIENT_ID)
            .build()
        val request = Request.Builder().url(TOKEN_URL).post(body).build()

        runCatching {
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "refresh ${response.code}" }
                val json = JSONObject(response.body!!.string())
                val access = json.getString("access_token")
                tokenStore.spotifyAccessToken = access
                tokenStore.spotifyExpiresAt = System.currentTimeMillis() + json.getInt("expires_in") * 1000L
                if (json.has("refresh_token")) tokenStore.spotifyRefreshToken = json.getString("refresh_token")
                access
            }
        }.getOrNull()
    }

    fun isConnected(): Boolean = tokenStore.spotifyRefreshToken != null

    private fun generateVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun challengeFor(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    companion object {
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
    }
}
