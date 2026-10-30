# Changelog

## [2026.10] - 2026-10-29

### Added
- **Library import:** installed over the app it replaces (same app ID and signing key), Tuned brings over liked songs, playlists, listening history, searches and the YouTube Music sign-in on first launch, then opens on Home.
- **Widgets:** four fixed sizes (Full cover, Small, Medium, Wide), coloured from the cover with a soft wash from the top.
- **Lyric story card:** share up to five lines as a 9:16 story with a frosted panel on a gradient from the cover's colours.
- **Open-source licences** in Settings, Data and storage, with the version.

### Changed
- **Loading indicator:** one circular spinner everywhere, including pull-to-refresh.
- **Mini player:** restyled, same controls.
- **Logo:** the flower in notifications.
- **Text:** widget, Jam, queue and audio-speed labels can now be translated; sentence case throughout; no "·" separators; "Synced 1 playlist" reads correctly.
- **Code:** package `com.tuned.app`, with saved-data names kept in `data/StorageNames.kt` so nothing is lost on update.

### Fixed
- Album page crashing on scroll when an album appeared twice in a shelf.
- Liked songs showing as unliked after the app restarted.
- The About page had no way in; App language now sits in Appearance, and Share logs and the licences in Data and storage.
- Lossless add-on requests never being signed: the native check expected a package name the app didn't have.
- The lyrics source list showing a backend's name instead of "Apple Lyrics".

## [0.1.0]

First Tuned build.
