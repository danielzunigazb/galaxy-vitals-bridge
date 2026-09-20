package com.danzuniga.vitalsbridge

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Stores the GitHub PAT encrypted on-device. Never logged, never synced anywhere else. */
class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "vitals_bridge_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var githubToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var spotifyAccessToken: String?
        get() = prefs.getString(KEY_SPOTIFY_ACCESS, null)
        set(value) = prefs.edit().putString(KEY_SPOTIFY_ACCESS, value).apply()

    var spotifyRefreshToken: String?
        get() = prefs.getString(KEY_SPOTIFY_REFRESH, null)
        set(value) = prefs.edit().putString(KEY_SPOTIFY_REFRESH, value).apply()

    var spotifyExpiresAt: Long
        get() = prefs.getLong(KEY_SPOTIFY_EXPIRES, 0L)
        set(value) = prefs.edit().putLong(KEY_SPOTIFY_EXPIRES, value).apply()

    /** PKCE verifier stashed between launching the auth browser and getting
     *  the redirect back — the process can be killed while the browser is
     *  open, so this needs to survive past a plain in-memory variable. */
    var spotifyPendingVerifier: String?
        get() = prefs.getString(KEY_SPOTIFY_VERIFIER, null)
        set(value) = prefs.edit().putString(KEY_SPOTIFY_VERIFIER, value).apply()

    companion object {
        private const val KEY_TOKEN = "github_token"
        private const val KEY_SPOTIFY_ACCESS = "spotify_access_token"
        private const val KEY_SPOTIFY_REFRESH = "spotify_refresh_token"
        private const val KEY_SPOTIFY_EXPIRES = "spotify_expires_at"
        private const val KEY_SPOTIFY_VERIFIER = "spotify_pending_verifier"
    }
}
