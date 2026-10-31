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
enum class PinKind { SONG, ALBUM, ARTIST }

/** Something pinned to the top of Library, with what's needed to show and open it offline. */
@Serializable
data class PinnedItem(
    val kind: PinKind,
    val title: String,
    val artist: String = "",
    val artworkUrl: String? = null,
    val browseId: String? = null,
    val videoId: String? = null,
) {
    /** Same thing however it was reached: by id when there is one, otherwise by name. */
    val key: String get() = "$kind|${videoId ?: browseId ?: "$title|$artist".lowercase()}"
}

/** Apple Music's Pins: songs, albums and artists kept at the top of Library. Newest first, on the device. */
@Singleton
class PinsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(PinnedItem.serializer())

    private val _pins = MutableStateFlow(load())
    val pins: StateFlow<List<PinnedItem>> = _pins.asStateFlow()

    fun isPinned(item: PinnedItem): Boolean = _pins.value.any { it.key == item.key }

    /** Pins or unpins [item]; returns true when it is now pinned. */
    fun toggle(item: PinnedItem): Boolean {
        val pinned = isPinned(item)
        save(if (pinned) _pins.value.filterNot { it.key == item.key } else listOf(item) + _pins.value)
        return !pinned
    }

    private fun load(): List<PinnedItem> = runCatching {
        prefs.getString(KEY, null)?.let { json.decodeFromString(serializer, it) }
    }.getOrNull().orEmpty()

    private fun save(items: List<PinnedItem>) {
        _pins.value = items
        prefs.edit().putString(KEY, json.encodeToString(serializer, items)).apply()
    }

    private companion object {
        const val PREFS = "library_pins"
        const val KEY = "pins"
    }
}
