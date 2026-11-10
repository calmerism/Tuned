package com.tuned.app.data.repository

import com.tuned.app.data.local.db.DownloadedTrackDao
import com.tuned.app.data.local.db.SongPlayStatsEntity
import com.tuned.app.util.ArtistHelper
import com.tuned.app.data.playlist.PlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Stats Mode for the Stats tab (HomeScreen).
 *
 * When Last.fm is NOT connected, the Stats page switches here. Everything comes from what was
 * actually played (`song_play_stats`, the same log as the recap). Saved playlists, liked songs
 * and downloads are a library, not a history, so they only lend album names.
 *
 * This is the local-first counterpart to [HomeRepository.fetchStats]: same
 * [HomeStats] shape so the UI renders identically, with a banner pointing to
 * Settings → Integrations to sync global scrobbles.
 */
@Singleton
class LocalStatsRepository @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val downloadedTrackDao: DownloadedTrackDao,
    private val playStatsDao: com.tuned.app.data.local.db.SongPlayStatsDao,
) {
    data class LocalStats(
        val stats: HomeStats,
        val topTracks: List<HomeTrack>,
        val topArtists: List<HomeArtistItem>,
        val topAlbums: List<HomeAlbum>,
        /** Played songs, newest first, with when they were last heard. */
        val recentTracks: List<HomeTrack> = emptyList(),
    )

    suspend fun load(): LocalStats {
        val played = runCatching { playStatsDao.allListened() }.getOrDefault(emptyList())
        val library = runCatching {
            playlistRepository.getAll().flatMap { it.tracks } + playlistRepository.getLikedSongs()?.tracks.orEmpty()
        }.getOrDefault(emptyList())
        val downloaded = runCatching { downloadedTrackDao.getAllList() }.getOrDefault(emptyList())
        val albums = library.mapNotNull { t -> t.album?.trim()?.takeIf(String::isNotBlank)?.let { "${t.name}|${t.artist}".lowercase() to it } } +
            downloaded.mapNotNull { d -> d.album.trim().takeIf(String::isNotBlank)?.let { "${d.title}|${d.artist}".lowercase() to it } }
        return buildLocalStats(played, albums.toMap())
    }
}

/** Plays as a count: a song only counts a play when it ends, so one heard in part is one. */
private val SongPlayStatsEntity.plays: Int get() = maxOf(playCount, 1)

/**
 * The You tab's figures from the play log. [albumOf] maps "title|artist" (lowercase) to an
 * album name, since the log doesn't keep one.
 */
internal fun buildLocalStats(played: List<SongPlayStatsEntity>, albumOf: Map<String, String>): LocalStatsRepository.LocalStats {
    val songs = played.filter { it.title.isNotBlank() && it.artist.isNotBlank() && !ArtistHelper.isPlayCountOrStat(it.artist) }
    fun SongPlayStatsEntity.toHomeTrack(timestamp: Long?) = HomeTrack(
        name = title,
        artist = artist,
        artworkUrl = artworkUrl,
        timestampMillis = timestamp,
        playCount = plays,
        videoId = videoId,
    )
    val topTracks = songs.sortedWith(compareByDescending<SongPlayStatsEntity> { it.plays }.thenByDescending { it.totalPlayTimeMs })
        .take(50)
        .map { it.toHomeTrack(null) }
    val recent = songs.sortedByDescending { it.lastPlayedAtMillis }
        .take(200)
        .map { it.toHomeTrack(it.lastPlayedAtMillis.takeIf { ms -> ms > 0 }) }
    val byArtist = songs.groupBy { ArtistHelper.primaryArtist(it.artist).trim().lowercase() }
    val topArtists = byArtist.entries
        .sortedByDescending { (_, list) -> list.sumOf { it.plays } }
        .take(30)
        .map { (_, list) ->
            HomeArtistItem(
                name = ArtistHelper.primaryArtist(list.first().artist).trim(),
                playCount = list.sumOf { it.plays }.toLong(),
                artworkUrl = list.firstNotNullOfOrNull { it.artworkUrl },
            )
        }
    val withAlbum = songs.mapNotNull { song -> albumOf["${song.title}|${song.artist}".lowercase()]?.let { it to song } }
    val topAlbums = withAlbum.groupBy { (album, song) -> "${ArtistHelper.primaryArtist(song.artist).lowercase()}|${album.lowercase()}" }
        .values
        .sortedByDescending { group -> group.sumOf { it.second.plays } }
        .take(10)
        .map { group ->
            HomeAlbum(
                name = group.first().first,
                artist = ArtistHelper.primaryArtist(group.first().second.artist),
                artworkUrl = group.firstNotNullOfOrNull { it.second.artworkUrl },
                playCount = group.sumOf { it.second.plays }.toLong(),
            )
        }
    return LocalStatsRepository.LocalStats(
        stats = HomeStats(
            scrobbles = songs.sumOf { it.plays.toLong() },
            trackCount = songs.size.toLong(),
            artistCount = byArtist.size.toLong(),
            albumCount = topAlbums.size.toLong(),
            avatarUrl = null,
            listenedSeconds = songs.sumOf { it.totalPlayTimeMs } / 1000L,
        ),
        topTracks = topTracks,
        topArtists = topArtists,
        topAlbums = topAlbums,
        recentTracks = recent,
    )
}
