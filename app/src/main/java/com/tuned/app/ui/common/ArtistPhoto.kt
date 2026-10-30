package com.tuned.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Artist photos from Deezer's public search, cached for the session ("" = none found). */
private val artistPhotoCache = ConcurrentHashMap<String, String>()

/** The artist's photo URL, or null until found (or when there is none). */
@Composable
fun rememberArtistPhoto(artist: String): String? {
    val key = artist.trim().lowercase()
    var url by remember(key) { mutableStateOf(artistPhotoCache[key]?.takeIf { it.isNotEmpty() }) }
    LaunchedEffect(key) {
        if (key.isEmpty() || artistPhotoCache.containsKey(key)) return@LaunchedEffect
        val found = withContext(Dispatchers.IO) { runCatching { lookUp(artist.trim()) }.getOrNull() }
        artistPhotoCache[key] = found.orEmpty()
        url = found
    }
    return url
}

private fun lookUp(artist: String): String? {
    val query = URLEncoder.encode(artist, "UTF-8")
    val connection = URL("https://api.deezer.com/search/artist?limit=5&q=$query").openConnection() as HttpURLConnection
    connection.connectTimeout = 6000
    connection.readTimeout = 6000
    return try {
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val data = org.json.JSONObject(body).optJSONArray("data") ?: return null
        val wanted = artist.lowercase()
        // Prefer an exact name match; Deezer ranks by popularity otherwise.
        val pick = (0 until data.length()).map { data.getJSONObject(it) }
            .firstOrNull { it.optString("name").lowercase() == wanted }
            ?: return null
        pick.optString("picture_xl").ifBlank { pick.optString("picture_big") }.ifBlank { null }
    } finally {
        connection.disconnect()
    }
}
