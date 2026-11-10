package com.tuned.app.data.music

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The ranking findBestMatch applies, on YouTube Music's real results for Justice. */
class VersionPickTest {
    private val results = listOf(
        YouTubeMusicTrack("orig", "D.A.N.C.E.", "Justice", isVideo = false),
        YouTubeMusicTrack("www", "D.A.N.C.E. (WWW)", "Justice", isVideo = false),
        YouTubeMusicTrack("medley", "D.A.N.C.E. x Fire x Safe and Sound (WWW)", "Justice", isVideo = false),
        YouTubeMusicTrack("demo", "D.A.N.C.E. (Demo)", "Justice", isVideo = false),
    )

    private fun pick(title: String) = results.maxBy { TextMatch.matchScore(it, title, "Justice") }.videoId

    @Test
    fun askingForTheWwwVersionGetsIt() {
        assertThat(pick("D.A.N.C.E. (WWW)")).isEqualTo("www")
    }

    @Test
    fun askingForThePlainSongGetsThePlainSong() {
        assertThat(pick("D.A.N.C.E.")).isEqualTo("orig")
    }
}

class AlbumSongVersionTest {
    private fun song(id: String, title: String, album: String, seconds: Int) =
        YouTubeMusicTrack(id, title, "Justice", album, null, seconds, isVideo = false)

    // YouTube calls the WWW video plain "D.A.N.C.E."; the album page names it properly.
    private val wwwVideo = YouTubeMusicTrack("otzcum1UKW4", "D.A.N.C.E. (WWW)", "Justice", null, null, 197, isVideo = true)
    private val results = listOf(
        song("orig", "D.A.N.C.E.", "Cross", 243),
        song("www", "D.A.N.C.E. (WWW)", "Woman Worldwide", 197),
    )

    @Test
    fun theAlbumPagesTitleFindsTheWwwSong() {
        assertThat(pickSongVersion(wwwVideo, results, album = "Woman Worldwide")?.videoId).isEqualTo("www")
    }

    @Test
    fun theSameTitleOnTwoAlbumsPrefersThePagesAlbum() {
        val video = YouTubeMusicTrack("v", "Genesis", "Justice", null, null, 234, isVideo = true)
        val both = listOf(song("comp", "Genesis", "Best Of", 234), song("cross", "Genesis", "Cross", 234))
        assertThat(pickSongVersion(video, both, album = "Cross")?.videoId).isEqualTo("cross")
        assertThat(pickSongVersion(video, both, album = null)?.videoId).isEqualTo("comp")
    }
}
