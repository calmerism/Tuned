package com.tuned.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.tuned.app.data.local.db.SongPlayStatsEntity
import org.junit.Test

class LocalStatsFromPlaysTest {
    private fun play(title: String, artist: String, plays: Int, ms: Long, at: Long) =
        SongPlayStatsEntity(trackKey = "$title|$artist", title = title, artist = artist, playCount = plays, totalPlayTimeMs = ms, lastPlayedAtMillis = at)

    @Test
    fun historyIsWhatWasPlayedNewestFirst() {
        val stats = buildLocalStats(listOf(play("Old", "A", 5, 60_000, 1), play("New", "B", 0, 30_000, 9)), emptyMap())
        assertThat(stats.recentTracks.map { it.name }).containsExactly("New", "Old").inOrder()
        assertThat(stats.recentTracks.first().timestampMillis).isEqualTo(9)
    }

    @Test
    fun topTracksRankByPlays() {
        val stats = buildLocalStats(listOf(play("Once", "A", 1, 10_000, 1), play("Often", "A", 7, 10_000, 2)), emptyMap())
        assertThat(stats.topTracks.first().name).isEqualTo("Often")
        assertThat(stats.stats.scrobbles).isEqualTo(8)
        assertThat(stats.stats.listenedSeconds).isEqualTo(20)
    }

    @Test
    fun albumsOnlyComeFromPlayedSongs() {
        val stats = buildLocalStats(listOf(play("Song", "Artist", 2, 10_000, 1)), mapOf("song|artist" to "Record", "unplayed|x" to "Saved"))
        assertThat(stats.topAlbums.map { it.name }).containsExactly("Record")
    }

    @Test
    fun nothingPlayedMeansNothingShown() {
        val stats = buildLocalStats(emptyList(), mapOf("song|artist" to "Record"))
        assertThat(stats.recentTracks).isEmpty()
        assertThat(stats.topTracks).isEmpty()
        assertThat(stats.topAlbums).isEmpty()
    }
}

class AcceptsAddedSongsTest {
    @Test
    fun mixesAndSavedPlaylistsTakeSongs() {
        listOf("custom", "liked", "track", "artist", "discover", "tag").forEach {
            assertThat(com.tuned.app.data.playlist.acceptsAddedSongs(it)).isTrue()
        }
    }

    @Test
    fun localSongsAndYouTubeCopiesDoNot() {
        assertThat(com.tuned.app.data.playlist.acceptsAddedSongs("local")).isFalse()
        assertThat(com.tuned.app.data.playlist.acceptsAddedSongs("youtube_remote")).isFalse()
    }
}
