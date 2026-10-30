package com.tuned.app.data.library

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class FavoriteArtist(
    val name: String,
    val browseId: String = "",
    val artworkUrl: String? = null,
    val addedAtMillis: Long = 0L,
)

/**
 * Artists the user starred on an artist page (Apple Music's Favourite). Kept on the device,
 * newest first, keyed by name so the same artist from search and from a song menu is one entry.
 */
@Singleton
class FavoriteArtistsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(FavoriteArtist.serializer())

    private val _artists = MutableStateFlow(load())
    val artists: StateFlow<List<FavoriteArtist>> = _artists.asStateFlow()

    fun isFavorite(name: String): Boolean = _artists.value.any { it.name.equals(name, ignoreCase = true) }

    /** Adds or removes [name]; returns true when it is now a favourite. */
    fun toggle(name: String, browseId: String, artworkUrl: String?): Boolean {
        val current = _artists.value
        val next = if (current.any { it.name.equals(name, ignoreCase = true) }) {
            current.filterNot { it.name.equals(name, ignoreCase = true) }
        } else {
            listOf(FavoriteArtist(name, browseId, artworkUrl, System.currentTimeMillis())) + current
        }
        _artists.value = next
        prefs.edit().putString(KEY, json.encodeToString(serializer, next)).apply()
        return next.size > current.size
    }

    private fun load(): List<FavoriteArtist> =
        prefs.getString(KEY, null)
            ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            .orEmpty()

    private companion object {
        const val PREFS = "favorite_artists"
        const val KEY = "artists"
    }
}
