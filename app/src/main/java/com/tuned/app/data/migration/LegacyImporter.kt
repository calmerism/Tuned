package com.tuned.app.data.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.tuned.app.data.local.SessionPreferences
import com.tuned.app.data.local.db.SongPlayStatsDao
import com.tuned.app.data.local.db.SongPlayStatsEntity
import com.tuned.app.data.playlist.LikedSongsManager
import com.tuned.app.data.playlist.PlaylistRepository
import com.tuned.app.data.search.SearchHistoryRepository
import com.tuned.app.data.ytmusic.YtMusicAuthManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Brings the previous app's library into Tuned, once. Tuned ships as an update to the
 * app it replaces (same app ID and signing key), so that app's database is still in this
 * app's data folder after the update. This copies liked songs, playlists, listening history and
 * searches into Tuned's own stores. The previous app's files are left untouched, so nothing is
 * lost if a run is cut short: an unfinished import simply runs again next launch.
 *
 * A YouTube Music sign-in carries over too: the previous app keeps the session cookie in its
 * settings file, and Tuned signs in with it, so nobody has to log in again.
 *
 * Not brought over: downloads (kept in the previous app's player cache) and its
 * other settings, which have no counterpart in Tuned.
 */
@Singleton
class LegacyImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val likedSongsManager: LikedSongsManager,
    private val playStatsDao: SongPlayStatsDao,
    private val searchHistory: SearchHistoryRepository,
    private val ytAuth: YtMusicAuthManager,
    private val session: SessionPreferences,
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** What was imported, for the one-time message; null when there was nothing to do. */
    data class Result(val liked: Int, val playlists: Int)

    suspend fun runOnce(): Result? {
        // The sign-in is its own stage, so phones that imported before it existed still get it.
        if (!prefs.getBoolean(KEY_ACCOUNT_DONE, false)) {
            val previousAppWasHere = context.getDatabasePath(LegacyLibraryReader.DATABASE_NAME).exists()
            runCatching { carryOverYouTubeSignIn() }.onFailure { Log.e(TAG, "Couldn't carry over the YouTube Music sign-in", it) }
            // Someone coming from the previous app already chose how to use the app: without a
            // YouTube sign-in that was without an account, so skip the welcome screen.
            if (previousAppWasHere && !ytAuth.connection.value.isConnected && !session.isGuestMode) {
                runCatching { session.setGuestMode(true) }
            }
            prefs.edit().putBoolean(KEY_ACCOUNT_DONE, true).apply()
        }
        if (prefs.getBoolean(KEY_DONE, false)) return null
        val file = context.getDatabasePath(LegacyLibraryReader.DATABASE_NAME)
        if (!file.exists()) {
            // A fresh install, or the previous app was never here: nothing to look for again.
            prefs.edit().putBoolean(KEY_DONE, true).apply()
            return null
        }
        val library = runCatching {
            // Read-write only so SQLite can fold in the old write-ahead log; nothing is written.
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use(LegacyLibraryReader::read)
        }.onFailure { Log.e(TAG, "Couldn't read the previous library", it) }.getOrNull() ?: return null

        if (!library.isEmpty) {
            likedSongsManager.likeAll(library.liked)
            val existing = playlistRepository.titles().map { it.lowercase() }.toSet()
            for (playlist in library.playlists) {
                // A rerun after an interrupted import must not stack second copies.
                if (playlist.name.lowercase() in existing) continue
                if (playlist.tracks.isEmpty()) playlistRepository.createCustom(playlist.name)
                else playlistRepository.save(playlist.name, "", "custom", playlist.tracks)
            }
            // Play counts add up, so they are merged once even if the import reruns.
            if (!prefs.getBoolean(KEY_PLAYS_DONE, false)) {
                for (play in library.plays) mergePlays(play)
                prefs.edit().putBoolean(KEY_PLAYS_DONE, true).apply()
            }
            library.searches.forEach(searchHistory::add)
        }
        prefs.edit().putBoolean(KEY_DONE, true).apply()
        Log.i(TAG, "Imported ${library.liked.size} liked songs, ${library.playlists.size} playlists, ${library.plays.size} play records")
        return if (library.isEmpty) null else Result(library.liked.size, library.playlists.size)
    }

    /** Signs Tuned in to the YouTube Music account the previous app was signed in to, if Tuned isn't already. */
    private suspend fun carryOverYouTubeSignIn() {
        if (ytAuth.connection.value.isConnected) return
        val account = LegacyAccountReader.read(context) ?: return
        ytAuth.connect(account.cookie, account.name, account.channelHandle, photoUrl = null)
        Log.i(TAG, "Carried over the YouTube Music sign-in")
    }

    private suspend fun mergePlays(play: LegacyPlays) {
        val title = play.track.name.trim()
        val artist = play.track.artist.trim()
        if (title.isBlank() || artist.isBlank()) return
        // Same key as SongPlayStatsRepository, so Tuned's own listening adds to it.
        val key = "$title|$artist".lowercase()
        val existing = playStatsDao.find(key)
        playStatsDao.upsert(
            (existing ?: SongPlayStatsEntity(trackKey = key, title = title, artist = artist)).copy(
                videoId = existing?.videoId ?: play.videoId,
                artworkUrl = existing?.artworkUrl ?: play.track.artworkUrl,
                totalPlayTimeMs = (existing?.totalPlayTimeMs ?: 0L) + play.totalPlayTimeMs,
                playCount = (existing?.playCount ?: 0) + play.playCount,
                lastPlayedAtMillis = maxOf(existing?.lastPlayedAtMillis ?: 0L, play.lastPlayedAtMillis),
            ),
        )
    }

    private companion object {
        const val TAG = "LegacyImporter"
        const val PREFS = "tuned_migration"
        const val KEY_DONE = "legacy_import_done"
        const val KEY_PLAYS_DONE = "legacy_plays_done"
        const val KEY_ACCOUNT_DONE = "legacy_account_done"
    }
}
