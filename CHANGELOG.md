# Changelog

## [2026.10] - 2026-10-29

### Added
- **Library import:** installed over the app it replaces (same app ID and signing key), Tuned brings over liked songs, playlists, listening history, searches and the YouTube Music sign-in on first launch, then opens on Home.
- **Widgets:** four fixed sizes (Full cover, Small, Medium, Wide), coloured from the cover with a soft wash from the top.
- **Lyric story card:** share up to five lines as a 9:16 story with a frosted panel on a gradient from the cover's colours.
- **In-app updates:** Tuned checks GitHub for a new release, downloads the APK, checks it against the release's SHA-256 and hands it to Android to install. Find it under Settings, App.
- **AutoMix:** beat-matches songs whose tempos are close and swaps the bass lines halfway. When one song has no clear beat, they overlap at full level. Two clear beats that would clash still fade.
- **AutoMix on the cover:** the old cover melts into the new one during a blend.
- **Videos tab** in search.
- **Playlist Playground:** type an idea and get a playlist built from YouTube Music searches.
- **Sing:** lowers the vocals from the lyrics screen. It's a rough reducer, so centred instruments dip too.
- **Lyrics:** duets, background vocals, and one translate and pronounce button.
- **AutoPlay toggle** in the queue, and **Music Haptics**.

### Changed
- **Loading indicator:** one circular spinner everywhere, including pull-to-refresh.
- **Mini player:** restyled, same controls.
- **Logo:** the flower in notifications.
- **Text:** widget, Jam, queue and audio-speed labels can now be translated; sentence case throughout; no "·" separators; "Synced 1 playlist" reads correctly.
- **Code:** package `com.tuned.app`, with saved-data names kept in `data/StorageNames.kt` so nothing is lost on update.

### Fixed
- Closing the app now stops the music.
- Volume jumping to full on USB and bit-perfect output.
- Headphones going silent until replugged after exclusive USB output.
- Speed and pitch doing nothing on the main audio path.
- The sung lyric line sitting too low on songs with a long intro. It now stays centred.
- Album page crashing on scroll when an album appeared twice in a shelf.
- Liked songs showing as unliked after the app restarted.
- The About page had no way in; App language now sits in Appearance, and Share logs and the licences in Data and storage.
- Lossless add-on requests never being signed: the native check expected a package name the app didn't have.
- The lyrics source list showing a backend's name instead of "Apple Lyrics".

## [0.1.0]

First Tuned build.
