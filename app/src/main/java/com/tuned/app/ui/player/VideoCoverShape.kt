package com.tuned.app.ui.player

import com.tuned.app.data.artwork.ArtworkNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How a YouTube video's frame shows as the cover while its audio plays: cropped to a square
 * like an album cover, or whole at 16:9. The mini player and the full player read the same
 * value, so the cover keeps its shape through the morph between them. Saved in settings by
 * [PlayerViewModel].
 */
object VideoCoverShape {
    private val _square = MutableStateFlow(true)
    val square: StateFlow<Boolean> = _square.asStateFlow()

    fun set(square: Boolean) {
        _square.value = square
    }

    /** True when [coverUrl] is a video frame and the listener wants it whole. */
    fun isWide(coverUrl: String?, square: Boolean): Boolean = !square && ArtworkNormalizer.isVideoThumbnail(coverUrl)
}

/** Width over height of a video frame. */
internal const val VIDEO_COVER_ASPECT = 16f / 9f
