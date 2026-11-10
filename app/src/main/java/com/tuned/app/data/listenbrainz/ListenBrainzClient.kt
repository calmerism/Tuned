package com.tuned.app.data.listenbrainz

import android.util.Log
import com.tuned.app.data.local.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends listens to ListenBrainz with the listener's own user token. Off while the token is
 * blank. Failures are logged and dropped: a missed listen must never disturb playback.
 */
@Singleton
class ListenBrainzClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val settingsPreferences: SettingsPreferences,
) {
    suspend fun playingNow(artist: String, track: String, album: String?) =
        submit(listenBody("playing_now", artist, track, album, listenedAt = null))

    suspend fun listen(artist: String, track: String, album: String?, listenedAtSec: Long) =
        submit(listenBody("single", artist, track, album, listenedAtSec))

    /** True when [token] is accepted by ListenBrainz; null when it couldn't be asked. */
    suspend fun validate(token: String): Boolean? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$API/validate-token").header("Authorization", "Token ${token.trim()}").build()
            okHttpClient.newCall(request).execute().use { it.isSuccessful && it.body?.string().orEmpty().contains("\"valid\": true") }
        }.getOrNull()
    }

    private suspend fun submit(body: JsonObject): Boolean = withContext(Dispatchers.IO) {
        val token = runCatching { settingsPreferences.settings.first().listenBrainzToken }.getOrDefault("")
        if (token.isBlank()) return@withContext false
        runCatching {
            val request = Request.Builder()
                .url("$API/submit-listens")
                .header("Authorization", "Token $token")
                .post(body.toString().toRequestBody(JSON))
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) Log.w(TAG, "ListenBrainz refused a listen (HTTP ${response.code})")
                response.isSuccessful
            }
        }.getOrElse {
            Log.w(TAG, "ListenBrainz unreachable: ${it.message}")
            false
        }
    }

    companion object {
        private const val TAG = "ListenBrainz"
        private const val API = "https://api.listenbrainz.org/1"
        private val JSON = "application/json".toMediaType()

        /** The submit-listens payload. "playing_now" carries no time; "single" must. */
        internal fun listenBody(type: String, artist: String, track: String, album: String?, listenedAt: Long?): JsonObject =
            buildJsonObject {
                put("listen_type", type)
                putJsonArray("payload") {
                    add(
                        buildJsonObject {
                            if (listenedAt != null) put("listened_at", listenedAt)
                            putJsonObject("track_metadata") {
                                put("artist_name", artist)
                                put("track_name", track)
                                album?.takeIf { it.isNotBlank() }?.let { put("release_name", it) }
                                putJsonObject("additional_info") {
                                    put("media_player", "Tuned")
                                    put("submission_client", "Tuned")
                                }
                            }
                        },
                    )
                }
            }
    }
}
